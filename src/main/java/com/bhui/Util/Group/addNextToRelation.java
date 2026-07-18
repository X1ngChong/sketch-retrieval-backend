package com.bhui.Util.Group;

import com.bhui.Bean.SelectRoadSql;
import com.bhui.Common.DriverCommon;
import com.bhui.Common.InfoCommon;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * 为组节点创建next_to关系 添加了双向关系
 */
public class addNextToRelation {


    public static void main(String[] args) {
       String labelName = "nanjing";

      String LAYER_NAME = labelName+"Road"; // 设置道路节点所在图层的名称
        String buildingLabel = labelName+"Build"; //设置道路节点所在图层的名称
        String groupLabel = labelName+"Group"; //设置道路节点所在图层的名称


        try (DriverCommon driverCommon = new DriverCommon();
             Driver driver = driverCommon.getGraphDatabase();
             Session session = driver.session()) {
            List<List<Integer>> groupIds = addGroup.createBuildingGroups(InfoCommon.url, InfoCommon.username, InfoCommon.password, buildingLabel,groupLabel,LAYER_NAME);
            processBuildingRelationships(session, groupIds,LAYER_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processBuildingRelationships(Session session, List<List<Integer>> initBuildings,String LAYER_NAME) {
        for (List<Integer> buildingIds : initBuildings) {
            int size = buildingIds.size();
            if (size > 1) {
                // 检查每一对节点之间的相邻关系
                for (int i = 0; i < size; i++) {
                    for (int j = i + 1; j < size; j++) {
                        Integer group1 = buildingIds.get(i);
                        Integer group2 = buildingIds.get(j);

                        if (areFatherNextToChange(session, group1, group2,LAYER_NAME)) {
                            // 为两个组添加nextTo关系
                            createNextToRelationship(session, group1, group2);
                        }
                    }
                }
            }
        }
    }

    /**
     * 使用包围盒的俩个点 而不是单独使用中心点
     * @param session
     * @param group1
     * @param group2
     * @return
     */
    private static boolean areFatherNextToChange(Session session, Integer group1, Integer group2,String LAYER_NAME){
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
                     SelectRoadSql.roadLevel +
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