package com.bhui.Util.Group.xianlinhu;

import com.bhui.Bean.SelectRoadSql;
import com.bhui.Common.DriverCommon;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * 为组节点创建next_to关系 添加了双向关系
 */
public class xianlin2_3addNextToRelation {
    public static final String labelName = "nanjingMESO";

   public static final String LAYER_NAME = labelName+"Road2"; // 设置道路节点所在图层的名称
    public static final String Group_LABEL = labelName+"Group2"; // 设置道路节点所在图层的名称
    public static final String buildingLabel = labelName+"Build2"; //设置道路节点所在图层的名称

    public final static String Relationship = "Contain";//真实图谱的关系
    public static void main(String[] args) {
        try (DriverCommon driverCommon = new DriverCommon();
             Driver driver = driverCommon.getGraphDatabase();
             Session session = driver.session()) {
            List<Integer> groupIds = fetchFatherIds(session);
            processBuildingRelationships(session, groupIds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processBuildingRelationships(Session session, List<Integer> buildingIds) {
            int size = buildingIds.size();
                // 检查每一对节点之间的相邻关系
                for (int i = 0; i < size; i++) {
                    for (int j = i + 1; j < size; j++) {
                        Integer group1 = buildingIds.get(i);
                        Integer group2 = buildingIds.get(j);

                        if (areFatherNextToChange(session, group1, group2)) {
                            // 为两个组添加nextTo关系
                            createNextToRelationship(session, group1, group2);
                        }
                    }
                }


    }

    private static List<Integer> fetchFatherIds(Session session) {
        String fetchFatherIdQuery = "MATCH p=(start:"+Group_LABEL+")-[r:"+Relationship+"]->(end)  " +
                "WITH start, COLLECT(p) AS paths  " +
                "RETURN id(start) AS startId, HEAD(paths) AS firstPath  ";
        List<Integer> fatherIds = new ArrayList<>();
        Result result = session.run(fetchFatherIdQuery);
        while (result.hasNext()) {
            Record record = result.next();
            fatherIds.add(record.get("startId").asInt());
        }
        return fatherIds;
    }

    private static boolean areFatherNextTo(Session session, Integer group1, Integer group2) {
        String cypherQuery =
                "MATCH (b1), (b2) " +
                        "WHERE id(b1) = $buildingId1 AND id(b2) = $buildingId2 " +
                        "WITH b1, b2, " +
                        "((b1.bbox[0] + b1.bbox[2]) / 2) AS centerX1, " +
                        "((b1.bbox[1] + b1.bbox[3]) / 2) AS centerY1, " +
                        "((b2.bbox[0] + b2.bbox[2]) / 2) AS centerX2, " +
                        "((b2.bbox[1] + b2.bbox[3]) / 2) AS centerY2 " +
                        "WITH 'LINESTRING(' + " +
                        "toString(centerX1) + ' ' + toString(centerY1) + ', ' + " +
                        "toString(centerX2) + ' ' + toString(centerY2) + ')' AS line " +
                        "CALL spatial.intersects('" + LAYER_NAME + "', line) YIELD node " +
                        SelectRoadSql.S9roadLevel +//TODO：这边修改了道路的级别
                        " RETURN COUNT(node) AS count ";

        Result result = session.run(cypherQuery, parameters("buildingId1", group1, "buildingId2", group2));

        if (result.hasNext()) {
            Record record = result.next();
            int count = record.get("count").asInt();
            // 如果返回的计数等于 1，代表两个区域是相邻的
            return count == 1;
        }

        return false;
    }

    /**
     * 使用包围盒的俩个点 而不是单独使用中心点
     * @param session
     * @param group1
     * @param group2
     * @return
     */
    private static boolean areFatherNextToChange(Session session, Integer group1, Integer group2){
        for(int i = 0;i<4;i+=2){
            for(int y = 0;y<4;y+=2){
             String  cypherQuery = "MATCH (b1), (b2) " +
                       "WHERE id(b1) = $buildingId1 AND id(b2) = $buildingId2 " +
                       "WITH b1, b2, " +
                       "(b1.bbox[$x1]) AS centerX1, " +
                       "(b1.bbox[$y1]) AS centerY1, " +
                       "(b2.bbox[$x2]) AS centerX2, " +
                       "(b2.bbox[$y2]) AS centerY2 " +
                       "WITH 'LINESTRING(' + " +
                       "toString(centerX1) + ' ' + toString(centerY1) + ', ' + " +
                       "toString(centerX2) + ' ' + toString(centerY2) + ')' AS line " +
                       "CALL spatial.intersects('" + LAYER_NAME + "', line) YIELD node " +
                     SelectRoadSql.S9roadLevel +//TODO：这边修改了道路的级别
                       " RETURN COUNT(node) AS count ";
                Result result = session.run(cypherQuery, parameters("buildingId1", group1, "buildingId2", group2,"x1",y,"y1",y+1,"x2",i,"y2",i+1));

                if (result.hasNext()) {
                    Record record = result.next();
                    int count = record.get("count").asInt();
                    //System.out.println(count);
                    // 如果返回的计数等于 1，代表两个区域是相邻的
                    if(count == 1){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void createNextToRelationship(Session session, Integer group1, Integer group2) {
        String createRelationshipQuery =
                "MATCH (b1), (b2) " +
                        "WHERE id(b1) = $buildingId1 AND id(b2) = $buildingId2 " +
                        "MERGE (b1)-[:NEXT_TO]->(b2) " +
                        "MERGE (b2)-[:NEXT_TO]->(b1)";

        session.run(createRelationshipQuery, parameters("buildingId1", group1, "buildingId2", group2));
        System.out.println("创建next_to关系 " + group1 + " and " + group2);
    }
}