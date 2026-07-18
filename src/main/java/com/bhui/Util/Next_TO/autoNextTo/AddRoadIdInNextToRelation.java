package com.bhui.Util.Next_TO.autoNextTo;


import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import static org.neo4j.driver.Values.parameters;

/**
 * 在NextTo关系上添加经过了哪条道路的ID
 */
public class AddRoadIdInNextToRelation {

    /**
     * 根据传入的参数创建道路ID
     *
     * @param url          Neo4j数据库的URL
     * @param username     数据库用户名
     * @param password     数据库密码
     * @param labelName    节点标签前缀（如 "S6"）
     * @param index        用于动态调整查询逻辑的参数
     */
    public static void addRoadId(String url, String username, String password, String labelName,String road,  int index) {
        String LAYER_NAME = road; // 设置道路节点所在图层的名称
        String GroupName = labelName + "Group"; // 设置道路节点所在图层的名称

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {
            addRoadId(session, LAYER_NAME, GroupName, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addRoadId(Session session, String LAYER_NAME, String GroupName, int index) {
        String cypherQuery = "MATCH p=(s:" + GroupName + ")-[r:NEXT_TO]->(e:" + GroupName + ") RETURN id(s) as s, id(e) as e, id(r) as r";
        Result result = session.run(cypherQuery);

        while (result.hasNext()) {
            Record record = result.next();
            Integer groupRoadId = getGroupRoadId(session, record.get("s").asInt(), record.get("e").asInt(), LAYER_NAME,GroupName, index); // 获取道路的id
            changeLocation(session, record.get("r").asInt(), groupRoadId); // 将道路的id添加到关系上
        }
    }

    private static Integer getGroupRoadId(Session session, Integer groupID1, Integer groupID2, String LAYER_NAME,String GroupName, int index) {
        String whereClause = "";
        if (index == 0) {
            whereClause = " WHERE node.level = 5 OR node.level = 2 OR node.level = 1 ";
        }

        String cypherQuery =
                "MATCH (b1:" + GroupName + "), (b2:" + GroupName + ") " +
                        "WHERE id(b1) = $groupID1 AND id(b2) = $groupID2 " +
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
                        "RETURN id(node) as id";

        Result result = session.run(cypherQuery, parameters("groupID1", groupID1, "groupID2", groupID2));
        int id = 0; // 默认ID为0
        while (result.hasNext()) {
            Record record = result.next();
            id = record.get("id").asInt(); // 道路ID
        }

        return id; // 返回道路的id
    }

    public static void changeLocation(Session session, Integer idr, Integer roadId) {
        /**
         * 修改当前的NEXT_TO关系,给当前的NEXT_TO关系上添加roadID
         */
        session.run("MATCH p=()-[r:NEXT_TO]->() WHERE id(r) = $idr SET r.roadId = $roadId", parameters("idr", idr, "roadId", roadId));
        System.out.println("修改成功");
    }
}