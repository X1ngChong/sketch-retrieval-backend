package com.bhui.Util.addNearRelation.autoNearRelastion;


import com.bhui.Util.CalculateLocation;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.neo4j.driver.Values.parameters;

/**
 * 在计算Near关系的时候，为一个组下面的所有节点添加Near关系
 * Near:{distance:距离的远近,location:方位,order:顺序关系}
 */
public class AddNearRelation {

    public static void calculateAndAddNearRelations(String url, String username, String password,String textName) {
            try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password))) {
                Session session = driver.session();
                Session session2 = driver.session();

                // 遍历所有组节点
                try (Transaction tx = session.beginTransaction()) {
                    List<Integer> groupIds = fetchGroupIds(tx, textName);

                    for (Integer groupId : groupIds) {
                        System.out.println("计算组节点ID: " + groupId);

                        // 获取当前组下的所有子节点
                        List<Integer> childIds = fetchChildIds(tx, groupId);

                        // 计算所有最短距离的最大值
                        double maxShortestDistance = calculateMaxShortestDistance(tx, childIds);

                        // 对当前组下的所有节点进行 Near 关系计算
                        for (Integer childId1 : childIds) {
                            for (Integer childId2 : childIds) {
                                if (!Objects.equals(childId1, childId2)) {
                                    processNearRelation(tx, session2, childId1, childId2, maxShortestDistance);
                                }
                            }
                        }
                    }

                    tx.commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /**
     * 获取所有组节点的ID
     */
    private static List<Integer> fetchGroupIds(Transaction tx, String textName) {
        List<Integer> groupIds = new ArrayList<>();
        Result result = tx.run("MATCH (n:" + textName + ") RETURN id(n) as id");
        while (result.hasNext()) {
            Record record = result.next();
            groupIds.add(record.get("id").asInt());
        }
        return groupIds;
    }

    /**
     * 获取组节点下的所有子节点ID
     */
    private static List<Integer> fetchChildIds(Transaction tx, Integer groupId) {
        List<Integer> childIds = new ArrayList<>();
        Result result = tx.run("MATCH (n)-[]->(m) WHERE m.OBJECTID IS NOT NULL AND id(n) = $groupId RETURN id(m) as id",
                parameters("groupId", groupId));
        while (result.hasNext()) {
            Record record = result.next();
            childIds.add(record.get("id").asInt());
        }
        return childIds;
    }

    /**
     * 计算所有最短距离的最大值
     */
    private static double calculateMaxShortestDistance(Transaction tx, List<Integer> childIds) {
        double maxDistance = 0.0;
        for (Integer childId1 : childIds) {
            for (Integer childId2 : childIds) {
                if (!Objects.equals(childId1, childId2)) {
                    Result result = tx.run("MATCH (n), (m) " +
                                    "WHERE id(n) = $childId1 AND id(m) = $childId2 " +
                                    "WITH n, m, " +
                                    "     n.bbox[0] AS n_minX, n.bbox[1] AS n_minY, n.bbox[2] AS n_maxX, n.bbox[3] AS n_maxY, " +
                                    "     m.bbox[0] AS m_minX, m.bbox[1] AS m_minY, m.bbox[2] AS m_maxX, m.bbox[3] AS m_maxY " +
                                    "RETURN CASE " +
                                    "        WHEN n_maxX < m_minX THEN m_minX - n_maxX " +
                                    "        WHEN n_minX > m_maxX THEN n_minX - m_maxX " +
                                    "        ELSE 0 " +
                                    "    END AS x_distance, " +
                                    "    CASE " +
                                    "        WHEN n_maxY < m_minY THEN m_minY - n_maxY " +
                                    "        WHEN n_minY > m_maxY THEN n_minY - m_maxY " +
                                    "        ELSE 0 " +
                                    "    END AS y_distance, " +
                                    "    CASE " +
                                    "        WHEN n_maxX < m_minX OR n_minX > m_maxX OR n_maxY < m_minY OR n_minY > m_maxY THEN " +
                                    "            sqrt((CASE " +
                                    "                    WHEN n_maxX < m_minX THEN m_minX - n_maxX " +
                                    "                    WHEN n_minX > m_maxX THEN n_minX - m_maxX " +
                                    "                    ELSE 0 " +
                                    "                END * CASE " +
                                    "                    WHEN n_maxX < m_minX THEN m_minX - n_maxX " +
                                    "                    WHEN n_minX > m_maxX THEN n_minX - m_maxX " +
                                    "                    ELSE 0 " +
                                    "                END) + " +
                                    "                (CASE " +
                                    "                    WHEN n_maxY < m_minY THEN m_minY - n_maxY " +
                                    "                    WHEN n_minY > m_maxY THEN n_minY - m_maxY " +
                                    "                    ELSE 0 " +
                                    "                END * CASE " +
                                    "                    WHEN n_maxY < m_minY THEN m_minY - n_maxY " +
                                    "                    WHEN n_minY > m_maxY THEN n_minY - m_maxY " +
                                    "                    ELSE 0 " +
                                    "                END)) " +
                                    "        ELSE 0 " +
                                    "    END AS shortest_distance",
                            parameters("childId1", childId1, "childId2", childId2));
                    while (result.hasNext()) {
                        Record record = result.next();
                        Double shortestDistance = record.get("shortest_distance").asDouble();
                        if (shortestDistance > maxDistance) {
                            maxDistance = shortestDistance;
                        }
                    }
                }
            }
        }
        return maxDistance;
    }

    /**
     * 处理 Near 关系
     */
    private static void processNearRelation(Transaction tx, Session session, Integer childId1, Integer childId2, double maxShortestDistance) {
        Result result = tx.run("MATCH (n), (m) " +
                        "WHERE id(n) = $childId1 AND id(m) = $childId2 " +
                        "WITH n, m, " +
                        "     n.bbox[0] AS n_minX, n.bbox[1] AS n_minY, n.bbox[2] AS n_maxX, n.bbox[3] AS n_maxY, " +
                        "     m.bbox[0] AS m_minX, m.bbox[1] AS m_minY, m.bbox[2] AS m_maxX, m.bbox[3] AS m_maxY " +
                        "RETURN n.bbox AS box1, m.bbox AS box2, " +
                        "    CASE " +
                        "        WHEN n_maxX < m_minX THEN m_minX - n_maxX " +
                        "        WHEN n_minX > m_maxX THEN n_minX - m_maxX " +
                        "        ELSE 0 " +
                        "    END AS x_distance, " +
                        "    CASE " +
                        "        WHEN n_maxY < m_minY THEN m_minY - n_maxY " +
                        "        WHEN n_minY > m_maxY THEN n_minY - m_maxY " +
                        "        ELSE 0 " +
                        "    END AS y_distance, " +
                        "    CASE " +
                        "        WHEN n_maxX < m_minX OR n_minX > m_maxX OR n_maxY < m_minY OR n_minY > m_maxY THEN " +
                        "            sqrt((CASE " +
                        "                    WHEN n_maxX < m_minX THEN m_minX - n_maxX " +
                        "                    WHEN n_minX > m_maxX THEN n_minX - m_maxX " +
                        "                    ELSE 0 " +
                        "                END * CASE " +
                        "                    WHEN n_maxX < m_minX THEN m_minX - n_maxX " +
                        "                    WHEN n_minX > m_maxX THEN n_minX - m_maxX " +
                        "                    ELSE 0 " +
                        "                END) + " +
                        "                (CASE " +
                        "                    WHEN n_maxY < m_minY THEN m_minY - n_maxY " +
                        "                    WHEN n_minY > m_maxY THEN n_minY - m_maxY " +
                        "                    ELSE 0 " +
                        "                END * CASE " +
                        "                    WHEN n_maxY < m_minY THEN m_minY - n_maxY " +
                        "                    WHEN n_minY > m_maxY THEN n_minY - m_maxY " +
                        "                    ELSE 0 " +
                        "                END)) " +
                        "        ELSE 0 " +
                        "    END AS shortest_distance",
                parameters("childId1", childId1, "childId2", childId2));

        while (result.hasNext()) {
            Record record = result.next();
            Double shortestDistance = record.get("shortest_distance").asDouble();
            List<Object> box1 = record.get("box1").asList();
            List<Object> box2 = record.get("box2").asList();

            // 归一化处理
            double normalizedDistance = shortestDistance / maxShortestDistance;

            String distance = CalculateLocation.getDistanceRelation(normalizedDistance); // 计算 LD, MD, SD
            String location = CalculateLocation.getBaFangWei(box1, box2); // 计算八方位
            String order = CalculateLocation.getOrder(box1, box2); // 计算顺序关系

            // 添加 Near 关系
            addNearRelation(session, childId1, childId2, location, distance, order);
        }
    }

    /**
     * 添加 Near 关系
     */
    public static void addNearRelation(Session session, Integer nodeId1, Integer nodeId2, String location, String distance, String order) {
        // 检查是否已经存在 NEAR 关系
        Result checkResult = session.run("MATCH (n1)-[r:NEAR]->(n2) " +
                "WHERE ID(n1) = $id1 AND ID(n2) = $id2 " +
                "RETURN r", parameters("id1", nodeId1, "id2", nodeId2));

        if (!checkResult.hasNext()) {
            // 如果不存在，则添加关系
            session.run("MATCH (n1), (n2) " +
                            "WHERE ID(n1) = $id1 AND ID(n2) = $id2 " +
                            "CREATE (n1)-[:NEAR {location: $location, distance: $distance, order: $order}]->(n2)",
                    parameters("id1", nodeId1, "id2", nodeId2, "location", location, "distance", distance, "order", order));
            System.out.println("添加 NEAR 关系成功: " + nodeId1 + " -> " + nodeId2);
        } else {
            System.out.println("NEAR 关系已存在，未添加: " + nodeId1 + " -> " + nodeId2);
        }
    }
}