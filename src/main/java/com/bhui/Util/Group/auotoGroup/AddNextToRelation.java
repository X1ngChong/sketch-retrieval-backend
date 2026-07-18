package com.bhui.Util.Group.auotoGroup;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * 为组节点创建next_to关系，添加了双向关系
 */
public class AddNextToRelation {

    /**
     * 为组节点创建 NEXT_TO 关系
     *
     * @param url          Neo4j数据库的URL
     * @param username     数据库用户名
     * @param password     数据库密码
     * @param labelName    节点标签前缀（如 "S6"）
     * @param relationship 节点之间的关系类型（如 "Contain"）
     * @param index        用于动态调整查询逻辑的参数
     */
    public static void createNextToRelations(String url, String username, String password, String labelName, String roadFileName, String relationship, int index) {
        // 动态生成标签和图层名称
        String layerName = roadFileName;
        String groupLabel = labelName + "Group";

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {

            // 查找所有的组节点
            List<Integer> fatherIds = fetchFatherIds(session, groupLabel, relationship);

            // 检查每一对节点之间的相邻关系
            for (int i = 0; i < fatherIds.size(); i++) {
                for (int j = i + 1; j < fatherIds.size(); j++) {
                    Integer group1 = fatherIds.get(i);
                    Integer group2 = fatherIds.get(j);

                    // 使用包围盒的四个点检查相邻关系
                    if (areFatherNextToChange(session, group1, group2, layerName, index)) {
                        // 为两个组节点添加 NEXT_TO 关系
                        createNextToRelationship(session, group1, group2);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查找所有组节点的ID
     */
    private static List<Integer> fetchFatherIds(Session session, String groupLabel, String relationship) {
        String fetchFatherIdQuery = "MATCH p=(start:" + groupLabel + ")-[r:" + relationship + "]->(end) " +
                "WITH start, COLLECT(p) AS paths " +
                "RETURN id(start) AS startId, HEAD(paths) AS firstPath";
        List<Integer> fatherIds = new ArrayList<>();
        Result result = session.run(fetchFatherIdQuery);
        while (result.hasNext()) {
            Record record = result.next();
            fatherIds.add(record.get("startId").asInt());
        }
        return fatherIds;
    }

    /**
     * 使用包围盒的四个点检查两个组节点是否相邻
     */
    private static boolean areFatherNextToChange(Session session, Integer group1, Integer group2, String layerName, int index) {
        // 根据 index 动态调整 WHERE 条件
        String whereClause = "";
        if (index == 0) {
            whereClause = " WHERE node.level = 5 OR node.level = 1 ";
        }

        for (int i = 0; i < 4; i += 2) {
            for (int y = 0; y < 4; y += 2) {
                String cypherQuery = "MATCH (b1), (b2) " +
                        "WHERE id(b1) = $buildingId1 AND id(b2) = $buildingId2 " +
                        "WITH b1, b2, " +
                        "(b1.bbox[$x1]) AS centerX1, " +
                        "(b1.bbox[$y1]) AS centerY1, " +
                        "(b2.bbox[$x2]) AS centerX2, " +
                        "(b2.bbox[$y2]) AS centerY2 " +
                        "WITH 'LINESTRING(' + " +
                        "toString(centerX1) + ' ' + toString(centerY1) + ', ' + " +
                        "toString(centerX2) + ' ' + toString(centerY2) + ')' AS line " +
                        "CALL spatial.intersects('" + layerName + "', line) YIELD node " +
                        whereClause + // 动态添加 WHERE 条件
                        "RETURN COUNT(node) AS count";
                Result result = session.run(cypherQuery, parameters(
                        "buildingId1", group1,
                        "buildingId2", group2,
                        "x1", y,
                        "y1", y + 1,
                        "x2", i,
                        "y2", i + 1
                ));

                if (result.hasNext()) {
                    Record record = result.next();
                    int count = record.get("count").asInt();
                    System.out.println("相邻关系检查结果: " + count);
                    // 如果返回的计数等于 1，代表两个区域是相邻的
                    if (count == 1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 为两个组节点创建双向 NEXT_TO 关系
     */
    private static void createNextToRelationship(Session session, Integer group1, Integer group2) {
        String createRelationshipQuery =
                "MATCH (b1), (b2) " +
                        "WHERE id(b1) = $buildingId1 AND id(b2) = $buildingId2 " +
                        "MERGE (b1)-[:NEXT_TO]->(b2) " +
                        "MERGE (b2)-[:NEXT_TO]->(b1)";

        session.run(createRelationshipQuery, parameters("buildingId1", group1, "buildingId2", group2));
        System.out.println("创建 NEXT_TO 关系: " + group1 + " 和 " + group2);
    }
}