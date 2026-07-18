package com.bhui.Util.Next_TO.autoNextTo;

import com.bhui.Common.DriverCommon;
import com.bhui.Util.CalculateLocation;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class AddLocationInNextToRelation {


    // 新增的静态方法
    public static void addLocation(String url, String username, String password,String featureFileName ) {
        String groupLabel = featureFileName+"Group";
        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password))) {
            Session session = driver.session();

            addLocationInNextToRelation(session,groupLabel);

        }
    }

    private static void addLocationInNextToRelation(Session session,String groupLabel) {
        Result result = session.run("MATCH p=(s:" + groupLabel + ")-[r:NEXT_TO]->(e:" + groupLabel + ") RETURN id(r) as id,s.bbox as sb,e.bbox as eb");
        while (result.hasNext()) {
            Record record = result.next();
            Integer rId = record.get("id").asInt();
            List<Object> box1 = record.get("sb").asList();
            List<Object> box2 = record.get("eb").asList();
            String location = CalculateLocation.getBaFangWei(box1, box2); // 计算八方位
            setLocationInNextTo(session, rId, location);
        }
    }

    private static void setLocationInNextTo(Session session, Integer rId, String location) {
        session.run("MATCH ()-[r:NEXT_TO]->() " +
                "WHERE ID(r) = $rId " +
                "SET r.location = $location", parameters("rId", rId, "location", location));
       // System.out.println("添加成功");
    }
}