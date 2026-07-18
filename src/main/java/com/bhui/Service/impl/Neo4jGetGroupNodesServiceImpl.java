package com.bhui.Service.impl;

import com.bhui.Common.DriverCommon;
import com.bhui.Common.InfoCommon;
import com.bhui.Common.PathCommon;
import com.bhui.Service.Neo4jGetGroupNodesService;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class Neo4jGetGroupNodesServiceImpl implements Neo4jGetGroupNodesService {

    @Override
    public ArrayList<String[]> getNodeListByIds(List<Integer[]> resultIdList) {
        ArrayList<String[]> resultList = new ArrayList<>();

        try (DriverCommon driverCommon = new DriverCommon()) {
            Driver driver = driverCommon.getGraphDatabase();
            try (Session session = driver.session()) {
                try (Transaction tx = session.beginTransaction()) {
                    for (Integer[] list : resultIdList) {
                        List<String> tempList = new ArrayList<>(); // 使用 ArrayList 来存储 osmID
                        for (Integer i : list) {
                            String cypherQuery = "MATCH p=(n)-[r:Contain]->(m) WHERE id(n) = " + i + " RETURN m." + PathCommon.OSMID + " as osmID";
                            Result result = tx.run(cypherQuery);
                            while (result.hasNext()) {
                                Record record = result.next();
                                tempList.add("'" + record.get("osmID").asString() + "'"); // 正确地将 osmID 添加到 tempList
                                //[155438410, 155438043, 8006, 8005, 871590011, 871600929, 871600928, 871600927, 871600926, 871600925, 871600924, 8007, 8004, 871600937, 871600933, 871600930, 525942922, 871600936, 871600935, 871600934, 871600932, 871600931, 871600922, 583447630]
                                //["'460504071'", "'8001'", "'8005'"]
                                //["'"155438410"'", "'"155438043"'", "'"8006"'", "'"8005"'", "'"871590011"'", "'"871600929"'", "'"871600928"'", "'"871600927"'", "'"871600926"'", "'"871600925"'", "'"871600924"'", "'"8007"'", "'"8004"'", "'"871600937"'", "'"871600933"'", "'"871600930"'", "'"525942922"'", "'"871600936"'", "'"871600935"'", "'"871600934"'", "'"871600932"'", "'"871600931"'", "'"871600922"'", "'"583447630"'"]
                            }
                        }
                        // 将 tempList 转换为 String[] 并加入 resultList
                        resultList.add(tempList.toArray(new String[0]));
                    }
                    tx.commit(); // 提交事务
                }
            }
        } catch (Exception e) {
            log.info("报错信息:{}", e.getMessage());
        }
        return resultList;
    }

    @Override
    public ArrayList<Integer[]> getObjectIdByIds(List<Integer[]> resultIdList) {
        ArrayList<Integer[]> resultList = new ArrayList<>();

        try (Driver driver = GraphDatabase.driver(InfoCommon.url, AuthTokens.basic(InfoCommon.username, InfoCommon.password));
             Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                for (Integer[] list : resultIdList) {
                    List<Integer> tempList = new ArrayList<>(); // 使用 ArrayList 来存储 osmID
                    for (Integer i : list) {
                        String cypherQuery = "MATCH p=(n)-[r:Contain]->(m) WHERE id(n) = " + i + " and m.OBJECTID IS NOT NULL RETURN m.OBJECTID as osmID";
                        Result result = tx.run(cypherQuery);
                        while (result.hasNext()) {
                            Record record = result.next();
                            tempList.add(record.get("osmID").asInt()); // 正确地将 osmID 添加到 tempList
                            }
                    }
                    // 将 tempList 转换为 String[] 并加入 resultList
                    resultList.add(tempList.toArray(new Integer[0]));
                }
                tx.commit(); // 提交事务
            }
        } catch (Exception e) {
            log.info("报错信息:{}", e.getMessage());
        }
        return resultList;
    }

    @Override
    public ArrayList<Integer[]> getObjectRoadIdsByIds(List<Integer[]> resultIdList) {
        ArrayList<Integer[]> resultList = new ArrayList<>();

        try (Driver driver = GraphDatabase.driver(InfoCommon.url, AuthTokens.basic(InfoCommon.username, InfoCommon.password));
             Session session = driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                for (Integer[] list : resultIdList) {
                    List<Integer> tempList = new ArrayList<>(); // 使用 ArrayList 来存储 osmID
                    for (int i = 0 ;i<list.length-1;i++){
                            String cypherQuery = " MATCH p=(n)-[r:NEXT_TO]->(m)   " +
                                    " WHERE id(n) = "+list[i]+" AND id(m) =  "+list[i+1]+"  " +
                                    " MATCH (road)   " +
                                    " WHERE id(road) = r.roadId    " +
                                    "  RETURN road.OBJECTID  AS osmID   ";
                            Result result = tx.run(cypherQuery);
                            while (result.hasNext()) {
                                Record record = result.next();
                                tempList.add(record.get("osmID").asInt()); // 正确地将 osmID 添加到 tempList
                            }
                            if(i == list.length-2 ){//最后的尾组和首组可能有关系需要判断
                                String cypherQuery2 = " MATCH p=(n)-[r:NEXT_TO]->(m)   " +
                                        " WHERE id(n) = "+list[i+1]+" AND id(m) =  "+list[0]+"  " +
                                        " MATCH (road)   " +
                                        " WHERE id(road) = r.roadId    " +
                                        "  RETURN road.OBJECTID  AS osmID   ";
                                Result result2 = tx.run(cypherQuery2);
                                while (result2.hasNext()) {
                                    Record record2 = result2.next();
                                    tempList.add(record2.get("osmID").asInt()); // 正确地将 osmID 添加到 tempList
                                }
                            }
                    }
                    // 将 tempList 转换为 String[] 并加入 resultList
                    resultList.add(tempList.toArray(new Integer[0]));
                }
                tx.commit(); // 提交事务
            }
        } catch (Exception e) {
            log.info("报错信息:{}", e.getMessage());
        }
        return resultList;
    }
}