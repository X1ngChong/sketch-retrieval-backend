package com.bhui.Util.Group.auotoGroup;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * 调整组的分配逻辑
 */
public class AdjustGroup {

    /**
     * 调整组的分配逻辑
     *
     * @param url      Neo4j数据库的URL
     * @param username 数据库用户名
     * @param password 数据库密码
     * @param label    标签前缀（如 "S6"）
     */
    public static void adjustGroups(String url, String username, String password, String label, String roadFileName, int index) {
        // 动态生成图层和标签名称
        String buildingLabel = label + "Build"; // 建筑物节点标签
        String groupLabel = label + "Group"; // 组节点标签
        String LAYER_NAME = roadFileName; // 道路图层名称

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {

            // 获取所有组的 ID
            List<Integer> groupIds = fetchAllGroupIds(session, groupLabel);

            // 调整组的分配
            adjustGroupsLogic(session, groupIds, buildingLabel, groupLabel, LAYER_NAME, index);

            // 清除当前组下面的空组
            cleanNullGroup(session, groupLabel);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 调整组的分配逻辑
     */
    private static void adjustGroupsLogic(Session session, List<Integer> groupIds, String buildingLabel, String groupLabel, String LAYER_NAME, int index) {
        for (Integer groupId : groupIds) {
            // 获取当前组中的所有建筑物
            List<Integer> buildingIdsInGroup = fetchBuildingsInGroup(session, groupId, buildingLabel, groupLabel);

            for (Integer buildingId : buildingIdsInGroup) {
                // 遍历其他组，检查是否需要重新分配
                for (Integer otherGroupId : groupIds) {
                    if (!otherGroupId.equals(groupId)) {
                        // 判断当前建筑物与目标组的连接性（是否与一半以上的建筑物无道路交叉）
                        if (shouldMoveToAnotherGroup(session, otherGroupId, buildingId, buildingLabel, groupLabel, LAYER_NAME, index)) {
                            // 从当前组中移除建筑物
                            removeBuildingFromGroup(session, groupId, buildingId, buildingLabel, groupLabel);

                            // 将建筑物添加到目标组
                            addBuildingToGroup(session, otherGroupId, buildingId, buildingLabel, groupLabel);

                            System.out.println("建筑物 ID: " + buildingId + " 从组 " + groupId + " 移动到组 " + otherGroupId);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取所有组的 ID
     */
    private static List<Integer> fetchAllGroupIds(Session session, String groupLabel) {
        String query = "MATCH (g:" + groupLabel + ") RETURN id(g) AS id";
        Result result = session.run(query);

        List<Integer> groupIds = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            groupIds.add(record.get("id").asInt());
        }
        return groupIds;
    }

    /**
     * 获取组中的所有建筑物
     */
    private static List<Integer> fetchBuildingsInGroup(Session session, Integer groupId, String buildingLabel, String groupLabel) {
        String query = "MATCH (g:" + groupLabel + ")-[:Contain]->(b:" + buildingLabel + ") WHERE id(g) = $groupId RETURN b.ID  AS id";
        Result result = session.run(query, parameters("groupId", groupId));

        List<Integer> buildingIds = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            buildingIds.add(record.get("id").asInt());
        }
        return buildingIds;
    }

    /**
     * 判断建筑物是否应该被分配到目标组
     */
    private static boolean shouldMoveToAnotherGroup(Session session, Integer targetGroupId, Integer buildingId, String buildingLabel, String groupLabel, String LAYER_NAME, int index) {
        // 获取目标组中的所有建筑物
        List<Integer> targetGroupBuildingIds = fetchBuildingsInGroup(session, targetGroupId, buildingLabel, groupLabel);

        int noRoadCrossCount = 0; // 无道路交叉的建筑物计数
        int totalBuildings = targetGroupBuildingIds.size();

        for (Integer targetBuildingId : targetGroupBuildingIds) {
            if (areBuildingsConnected(session, buildingId, targetBuildingId, buildingLabel, LAYER_NAME, index)) {
                noRoadCrossCount++;
            }
        }
        System.out.println("判断数量是否超过一半:" + noRoadCrossCount + "  " + (noRoadCrossCount > (totalBuildings / 2)));
        // 如果无道路交叉的建筑物数量超过一半，则返回 true
        return noRoadCrossCount > (totalBuildings / 2);
    }

    /**
     * 判断两个建筑物是否连接（基于道路交叉）
     */
    private static boolean areBuildingsConnected(Session session, Integer buildingId1, Integer buildingId2, String buildingLabel, String LAYER_NAME, int index) {
        // 根据 index 动态调整 WHERE 条件
        String whereClause = "";
        if (index == 0) {
            whereClause = " WHERE node.level = 5 OR node.level = 1 ";
        }

        // 动态拼接 Cypher 查询
        String cypherQuery =
                "MATCH (b1:" + buildingLabel + " {ID: $buildingId1}), (b2:" + buildingLabel + " {ID: $buildingId2}) " +
                        "WHERE id(b1) <> id(b2) " +
                        "WITH b1, b2, " +
                        "((b1.bbox[0] + b1.bbox[2]) / 2) AS centerX1, " +
                        "((b1.bbox[1] + b1.bbox[3]) / 2) AS centerY1, " +
                        "((b2.bbox[0] + b2.bbox[2]) / 2) AS centerX2, " +
                        "((b2.bbox[1] + b2.bbox[3]) / 2) AS centerY2 " +
                        "WITH 'LINESTRING(' + " +
                        "toString(centerX1) + ' ' + toString(centerY1) + ', ' + " +
                        "toString(centerX2) + ' ' + toString(centerY2) + ')' AS line " +
                        "CALL spatial.intersects('" + LAYER_NAME + "', line) YIELD node " +
                        whereClause + // 动态添加 WHERE 条件
                        "RETURN node";

        Result result = session.run(cypherQuery, parameters("buildingId1", buildingId1, "buildingId2", buildingId2));
        return !result.hasNext(); // 如果没有道路交叉，则认为建筑物是连接的
    }

    /**
     * 从组中移除建筑物
     */
    private static void removeBuildingFromGroup(Session session, Integer groupId, Integer buildingId, String buildingLabel, String groupLabel) {
        String query = "MATCH (g:" + groupLabel + ")-[r:Contain]->(b:" + buildingLabel + ") " +
                "WHERE id(g) = $groupId AND b.ID = $buildingId DELETE r";
        session.run(query, parameters("groupId", groupId, "buildingId", buildingId));
    }

    /**
     * 将建筑物添加到组
     */
    private static void addBuildingToGroup(Session session, Integer groupId, Integer buildingId, String buildingLabel, String groupLabel) {
        String query = "MATCH (g:" + groupLabel + "), (b:" + buildingLabel + ") " +
                "WHERE id(g) = $groupId AND b.ID = $buildingId " +
                "CREATE (g)-[:Contain]->(b)";
        session.run(query, parameters("groupId", groupId, "buildingId", buildingId));
    }

    /**
     * 清除空的组节点
     */
    private static void cleanNullGroup(Session session, String groupLabel) {
        String query = "MATCH (g:" + groupLabel + ") " +
                "WHERE NOT (g)-[:Contain]->() " +
                "DETACH DELETE g";
        session.run(query);
        System.out.println("清除成功");
    }
}