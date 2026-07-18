package com.bhui.Util.Group.auotoGroup;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;

/**
 * 获取所有父节点的ID 然后用区域内的左上角的点还有右下角的点作为整个地物的包围盒
 */
public class AddGroupBox {

    /**
     * 设置组的范围大小
     *
     * @param url          Neo4j数据库的URL
     * @param username     数据库用户名
     * @param password     数据库密码
     * @param labelName    节点标签前缀（如 "S6"）
     * @param relationship 节点之间的关系类型（如 "Contain"）
     */
    public static void setGroupBox(String url, String username, String password, String labelName, String relationship) {
        // 动态生成标签名称
        String label = labelName + "Group";

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {

            // 查找所有的组节点的ID
            List<Integer> fatherIds = fetchFatherIds(session, label, relationship);

            // 根据组节点ID去设置这个组的范围大小
            setFatherBbox(session, fatherIds, relationship);

            System.out.println("父节点ID列表: " + fatherIds);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查找所有父节点的ID
     */
    private static List<Integer> fetchFatherIds(Session session, String label, String relationship) {
        String fetchFatherIdQuery = "MATCH p=(start:" + label + ")-[r:" + relationship + "]->(end) " +
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
     * 根据父节点ID设置范围大小
     */
    private static void setFatherBbox(Session session, List<Integer> fatherIds, String relationship) {
        for (Integer fatherId : fatherIds) {
            String fetchBboxQuery = "MATCH p=(start)-[r:" + relationship + "]->(end) WHERE id(start) = " + fatherId + " RETURN end.bbox AS bbox";
            List<List<Double>> bboxLists = new ArrayList<>();
            Result result = session.run(fetchBboxQuery);

            while (result.hasNext()) {
                Record record = result.next();
                bboxLists.add(record.get("bbox").asList(Value::asDouble));
            }

            // 计算最左上角和最右下角的点
            if (!bboxLists.isEmpty()) {
                double minX = Double.MAX_VALUE;
                double maxY = Double.NEGATIVE_INFINITY;
                double maxX = Double.NEGATIVE_INFINITY;
                double minY = Double.MAX_VALUE;

                for (List<Double> bbox : bboxLists) {
                    minX = Math.min(minX, bbox.get(0));
                    maxY = Math.max(maxY, bbox.get(1));
                    maxX = Math.max(maxX, bbox.get(2));
                    minY = Math.min(minY, bbox.get(3));
                }

                // 创建新的 bbox 属性
                List<Double> newBbox = Arrays.asList(minX, maxY, maxX, minY);

                // 更新 start 节点的 bbox 属性
                String updateBboxQuery = "MATCH (start) WHERE id(start) = " + fatherId + " SET start.bbox = $bbox";
                session.run(updateBboxQuery, Values.parameters("bbox", newBbox));
            }
        }
    }
}