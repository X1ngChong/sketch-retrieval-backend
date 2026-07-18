package com.bhui.Util.Next_TO.autoNextTo;

import com.bhui.Common.InfoCommon;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

/**
 * 在NextTo关系上添加地物的投影顺序
 * @author JXS
 */
public class AddTypeOrderListInNextToRelation {

    /**
     * 更新所有 NEXT_TO 关系的地物投影顺序
     *
     * @param url      Neo4j数据库的URL
     * @param username 数据库用户名
     * @param password 数据库密码
     */
    public static void updateAllTypeOrderLists(String url, String username, String password) {
        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password));
             Session session = driver.session()) {
            // 获取所有的NextTo关系的id和orderList
            HashMap<Integer, List<Object>> allNextToRelation = getAllNextToRelation(session);

            for (Map.Entry<Integer, List<Object>> entry : allNextToRelation.entrySet()) {
                Integer rId = entry.getKey(); // 关系ID
                List<Object> orderList = entry.getValue(); // 道路集合
                List<String> typeByOrderList = getTypeByOrderList(session, orderList);
                setTypeOrderList(session, rId, typeByOrderList);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashMap<Integer, List<Object>> getAllNextToRelation(Session session) {
        String cypherQuery = "MATCH p=()-[r:NEXT_TO]->() WHERE r.orderList IS NOT NULL RETURN id(r) AS id, r.orderList AS list";

        System.out.println(cypherQuery);
        Result result = session.run(cypherQuery);
        HashMap<Integer, List<Object>> temp = new HashMap<>();
        while (result.hasNext()) {
            Record record = result.next();
            int rId = record.get("id").asInt(); // 关系ID
            List<Object> list = record.get("list").asList();
            temp.put(rId, list);
        }
        return temp; // 返回所有NextTo关系的ID和orderList
    }

    private static List<String> getTypeByOrderList(Session session, List<Object> orderList) {
        List<String> typeList = new ArrayList<>();
        String cypherQuery = "MATCH (n) WHERE id(n) IN " + orderList + " RETURN n.fclass AS type";

        System.out.println(cypherQuery);
        Result result = session.run(cypherQuery);
        while (result.hasNext()) {
            Record record = result.next();
            String type = record.get("type").asString(); // 地物类型
            typeList.add(type);
        }
        return typeList; // 返回地物类型列表
    }

    public static void setTypeOrderList(Session session, Integer rId, List<String> orderList) {
        /**
         * 修改当前的NEXT_TO关系,给当前的NEXT_TO关系上添加typeOrderList
         */
        session.run("MATCH p=()-[r:NEXT_TO]->() WHERE id(r) = $idr SET r.typeOrderList = $orderList", parameters("idr", rId, "orderList", orderList));
        System.out.println("修改成功");
    }
}