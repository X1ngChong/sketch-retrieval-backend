package com.bhui.Util.Group.xianlinhu;

import com.bhui.Bean.SelectRoadSql;
import com.bhui.Common.ImprotLabel;
import com.bhui.Common.InfoCommon;
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
public class S9_1addGroup {

    public static final String label = ImprotLabel.label;

    public static final String LAYER_NAME = ImprotLabel.LAYER_NAME; //设置道路节点所在图层的名称

    public static final String buildingLabel = ImprotLabel.buildingLabel; //设置道路节点所在图层的名称

    public static final String groupLabel = ImprotLabel.groupLabel; //设置组的名称



    public static void main(String[] args) {
        List<List<Integer>> groupIds = createBuildingGroups(InfoCommon.url, InfoCommon.username, InfoCommon.password);
        // 你可以在这里使用 groupIds 进行后续处理
        for (List<Integer> group : groupIds) {
            // 处理每个组
            System.out.println("Group ID: " + group);
        }
    }

    public static List<List<Integer>> createBuildingGroups(String url, String username, String password) {
        List<List<Integer>> groupIds = new ArrayList<>();

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {
            // 查找所有的building节点
            List<Integer> buildingIds = fetchBuildingIds(session);
                // 地图用于跟踪每栋建筑所属的组
                Map<Integer, Integer> buildingGroupMap = new HashMap<>(); // <Building ID, Group ID>
                List<Integer> groupId2 = new ArrayList<>();
                for (Integer buildingId : buildingIds) {
                    // 跳过已经分组的建筑
                    if (buildingGroupMap.containsKey(buildingId)) continue;

                    // 如果没有组，那么创建新的组
                    Integer groupId = createGroup(session);
                    buildingGroupMap.put(buildingId, groupId); // 记录组之间的关系

                    // 将非道路交叉的建筑与该组关联
                    associateBuildingsToGroup(session, buildingIds, buildingGroupMap, groupId, buildingId);
                    groupId2.add(groupId);
                }
                // 将当前组 ID 添加到 groupIds 列表中
                groupIds.add(groupId2); // 将 groupId 包装成 List 并添加到 groupIds

        } catch (Exception e) {
            e.printStackTrace();
        }

        return groupIds; // 返回创建的组 ID 列表
    }

    private static List<Integer> fetchBuildingIds(Session session) {
        String fetchBuildingsQuery = "MATCH (b:"+buildingLabel+") WHERE b.ID IS NOT NULL RETURN b.ID AS id";
        List<Integer> buildingIds = new ArrayList<>();
        Result result = session.run(fetchBuildingsQuery);
        while (result.hasNext()) {
            Record record = result.next();
            buildingIds.add(record.get("id").asInt());
        }
        return buildingIds;
    }



    private static boolean areBuildingsConnected(Session session, Integer buildingId1, Integer buildingId2) {
        String cypherQuery =
                "MATCH (b1:"+buildingLabel+" {ID: $buildingId1}), (b2:"+buildingLabel+"  {ID: $buildingId2}) " +
                        "WHERE b1.ID <> b2.ID " +
                        "WITH b1, b2, " +
                        "((b1.bbox[0] + b1.bbox[2]) / 2) AS centerX1, " +
                        "((b1.bbox[1] + b1.bbox[3]) / 2) AS centerY1, " +
                        "((b2.bbox[0] + b2.bbox[2]) / 2) AS centerX2, " +
                        "((b2.bbox[1] + b2.bbox[3]) / 2) AS centerY2 " +
                        "WITH 'LINESTRING(' + " +
                        "toString(centerX1) + ' ' + toString(centerY1) + ', ' + " +
                        "toString(centerX2) + ' ' + toString(centerY2) + ')' AS line " +
                        "CALL spatial.intersects('"+LAYER_NAME+"', line) YIELD node " +
                        SelectRoadSql.S9roadLevel +//TODO：这边修改了道路的级别
                        "  RETURN node ";

        Result result = session.run(cypherQuery, parameters("buildingId1", buildingId1, "buildingId2", buildingId2));
        return !result.hasNext(); // 如果建筑之间没有道路交叉，则为真
    }

    private static Integer createGroup(Session session) {
        String createGroupQuery = "CREATE (g:"+groupLabel+") RETURN id(g) AS id";
        Record groupRecord = session.run(createGroupQuery).single();
        return groupRecord.get("id").asInt();
    }

    private static void associateBuildingsToGroup(Session session, List<Integer> buildingIds,
                                                  Map<Integer, Integer> buildingGroupMap, Integer groupId, Integer startingBuildingId) {
        // 添加组和节点之间的关系
        String addNodeToGroupQuery =
                "MATCH (g:"+groupLabel+"), (b:"+buildingLabel+") WHERE id(g) = $groupNodeId AND b.ID = $buildingId " +
                        "CREATE (g)-[:Contain]->(b)";

        // 将起始建筑添加到组中
        session.run(addNodeToGroupQuery, parameters("groupNodeId", groupId, "buildingId", startingBuildingId));

        //如果连接，则将其他建筑分配到同一组
        for (Integer otherBuildingId : buildingIds) {
            if (!otherBuildingId.equals(startingBuildingId) && !buildingGroupMap.containsKey(otherBuildingId)) {
                if (areBuildingsConnected(session, startingBuildingId, otherBuildingId)) {
                    session.run(addNodeToGroupQuery, parameters("groupNodeId", groupId, "buildingId", otherBuildingId));
                    buildingGroupMap.put(otherBuildingId, groupId);
                    System.out.println("Building ID: " + otherBuildingId + " 添加到 Group的ID: " + groupId);
                }
            }
        }
    }
}