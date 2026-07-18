package com.bhui.Service.impl;

import com.bhui.Common.InfoCommon;
import com.bhui.Common.PathCommon;
import com.bhui.Service.ImportData;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImportDataImpl implements ImportData {

    public Driver driver = GraphDatabase.driver(InfoCommon.url, AuthTokens.basic(InfoCommon.username, InfoCommon.password));


    @Override
    public void importFileByLayerNameShapFilePath(String layerName, String shapFilePath) {
        try (Session session = driver.session()) {
                // 第一步：创建图层
                String createLayerQuery = String.format(
                        "CALL spatial.addWKTLayer('%s', 'geometry')",
                        layerName
                );
                log.info("创建语句:{}",createLayerQuery);

                // 执行创建图层的查询
            session.run(createLayerQuery);

                // 第二步：导入 Shapefile
                String importShapefileQuery = String.format(
                        "CALL spatial.importShapefileToLayer('%s', '%s')",
                        layerName, shapFilePath
                );
                log.info("导入语句:{}",importShapefileQuery);


                // 执行导入 Shapefile 的查询
            session.run(importShapefileQuery);


                // 第三步设置标签名称
                String setTagName = "MATCH (n:SpatialLayer {layer: '"+layerName+"'})-[]->(m)-[]->(x)  " +
                                "SET m:"+layerName+", x:"+layerName+" ";

                log.info("设置标签语句:{}",setTagName);

                // 执行导入 Shapefile 的查询
            session.run(setTagName);

        } catch (Exception e) {
            // 处理会话中的异常
            System.err.println("Session failed: " + e.getMessage());
        }
    }
}
