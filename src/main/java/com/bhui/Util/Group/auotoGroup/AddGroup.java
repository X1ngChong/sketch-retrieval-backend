package com.bhui.Util.Group.auotoGroup;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

/**
 * 添加组节点
 */
public class AddGroup {

    /**
     * 添加组节点的方法
     *
     * @param url           Neo4j数据库的URL
     * @param username      数据库用户名
     * @param password      数据库密码
     * @param label         标签前缀（如 "S6"）
     */
    public static void addGroups(String url, String username, String password, String label,String roadFileName,int index) {
        // 动态生成图层和标签名称
        String LAYER_NAME = roadFileName; // 设置道路节点所在图层的名称
        String buildingLabel = label + "Build"; // 设置建筑节点所在图层的名称
        String groupLabel = label + "Group"; // 设置组的名称

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {

            // 查找所有的building节点
            List<Integer> buildingIds = fetchBuildingIds(session, buildingLabel);

            // 地图用于跟踪每栋建筑所属的组
            Map<Integer, Integer> buildingGroupMap = new HashMap<>(); // <Building ID, Group ID>

            for (Integer buildingId : buildingIds) {
                // 跳过已经分组的建筑
                if (buildingGroupMap.containsKey(buildingId)) continue;

                // 如果没有组 那么创建新的组
                Integer groupId = createGroup(session, groupLabel);
                buildingGroupMap.put(buildingId, groupId); // 记录组之间的关系

                // 将非道路交叉的建筑与该组关联
                associateBuildingsToGroup(session, buildingIds, buildingGroupMap, groupId, buildingId, buildingLabel, groupLabel, LAYER_NAME,index);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Integer> fetchBuildingIds(Session session, String buildingLabel) {
        String fetchBuildingsQuery = "MATCH (b:" + buildingLabel + ") WHERE b.ID IS NOT NULL RETURN b.ID AS id";
        List<Integer> buildingIds = new ArrayList<>();
        Result result = session.run(fetchBuildingsQuery);
        while (result.hasNext()) {
            Record record = result.next();
            buildingIds.add(record.get("id").asInt());
        }
        return buildingIds;
    }

    private static boolean areBuildingsConnected(Session session, Integer buildingId1, Integer buildingId2, String buildingLabel, String LAYER_NAME, int index) {
        // 根据 index 动态调整 WHERE 条件
        String whereClause = "";
        if (index == 0) {
            whereClause = " WHERE node.level = 5 OR node.level = 2 OR node.level = 1 ";
        }

        // 动态拼接 Cypher 查询
        String cypherQuery =
                "MATCH (b1:" + buildingLabel + " {ID: $buildingId1}), (b2:" + buildingLabel + "  {ID: $buildingId2}) " +
                        "WHERE b1.ID <> b2.ID " +
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

        // 执行查询
        Result result = session.run(cypherQuery, parameters("buildingId1", buildingId1, "buildingId2", buildingId2));
        return !result.hasNext(); // 如果建筑之间没有道路交叉，则为真
    }

    private static Integer createGroup(Session session, String groupLabel) {
        String createGroupQuery = "CREATE (g:" + groupLabel + ") RETURN id(g) AS id";
        Record groupRecord = session.run(createGroupQuery).single();
        return groupRecord.get("id").asInt();
    }

    private static void associateBuildingsToGroup(Session session, List<Integer> buildingIds,
                                                  Map<Integer, Integer> buildingGroupMap, Integer groupId, Integer startingBuildingId,
                                                  String buildingLabel, String groupLabel, String LAYER_NAME,int index) {
        // 添加组和节点之间的关系
        String addNodeToGroupQuery =
                "MATCH (g:" + groupLabel + "), (b:" + buildingLabel + ") WHERE id(g) = $groupNodeId AND b.ID = $buildingId " +
                        "CREATE (g)-[:Contain]->(b)";

        // 将起始建筑添加到组中
        session.run(addNodeToGroupQuery, parameters("groupNodeId", groupId, "buildingId", startingBuildingId));

        // 如果连接，则将其他建筑分配到同一组
        for (Integer otherBuildingId : buildingIds) {
            if (!otherBuildingId.equals(startingBuildingId) && !buildingGroupMap.containsKey(otherBuildingId)) {
                if (areBuildingsConnected(session, startingBuildingId, otherBuildingId, buildingLabel, LAYER_NAME,index)) {
                    session.run(addNodeToGroupQuery, parameters("groupNodeId", groupId, "buildingId", otherBuildingId));
                    buildingGroupMap.put(otherBuildingId, groupId);
                    System.out.println("Building ID: " + otherBuildingId + " 添加到 Group的ID: " + groupId);
                }
            }
        }
    }
}