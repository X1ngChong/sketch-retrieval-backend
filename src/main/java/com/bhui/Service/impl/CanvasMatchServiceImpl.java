package com.bhui.Service.impl;

import com.bhui.Bean.Pair;
import com.bhui.Common.InfoCommon;
import com.bhui.Common.PathCommon;
import com.bhui.Service.CanvasMatchService;
import com.bhui.Service.Neo4jGetGroupNodesService;
import com.bhui.Service.Neo4jService;
import com.bhui.dto.SimilarityResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 手绘草图匹配服务实现
 * @author JXS
 */
@Service
@Slf4j
public class CanvasMatchServiceImpl implements CanvasMatchService {

    @Autowired
    private Neo4jService neo4jService;

    @Autowired
    private Neo4jGetGroupNodesService neo4jGetGroupNodesService;

    @Override
    public List<SimilarityResultDTO> matchCanvasDrawings(Map<String, Object> geoJson, String realLabel) {
        log.info("开始手绘草图匹配，realLabel: {}", realLabel);

        List<SimilarityResultDTO> results = new ArrayList<>();

        try {
            // 1. 解析 GeoJSON 数据，提取建筑物信息
            List<Map<String, Object>> buildings = parseGeoJson(geoJson);
            log.info("解析到 {} 个建筑物", buildings.size());

            if (buildings.isEmpty()) {
                return results;
            }

            // 2. 获取真实地图的组 ID
            Integer[] realGroupIds = neo4jService.getGroupIdsByTag(realLabel);
            log.info("真实地图组数量: {}", realGroupIds.length);

            if (realGroupIds.length == 0) {
                return results;
            }

            // 3. 为每个真实地图组计算相似度
            for (Integer realGroupId : realGroupIds) {
                try {
                    // 计算手绘草图与真实地图组的相似度
                    double similarity = calculateSimilarity(buildings, realGroupId);

                    if (similarity > 0.1) { // 只保留相似度大于 0.1 的结果
                        // 获取该组的 OBJECTID
                        Integer[] groupIdArray = new Integer[]{realGroupId};
                        List<Integer[]> groupIdList = Collections.singletonList(groupIdArray);
                        
                        ArrayList<Integer[]> objectIdsList = neo4jGetGroupNodesService.getObjectIdByIds(groupIdList);
                        ArrayList<Integer[]> roadIdsList = neo4jGetGroupNodesService.getObjectRoadIdsByIds(groupIdList);
                        
                        Integer[] objectIds = objectIdsList.isEmpty() ? new Integer[0] : objectIdsList.get(0);
                        Integer[] roadIds = roadIdsList.isEmpty() ? new Integer[0] : roadIdsList.get(0);

                        SimilarityResultDTO dto = new SimilarityResultDTO(
                            similarity,
                            objectIds,
                            roadIds
                        );
                        results.add(dto);
                        log.info("组 {} 相似度: {}", realGroupId, similarity);
                    }
                } catch (Exception e) {
                    log.error("计算组 {} 相似度时出错: {}", realGroupId, e.getMessage());
                }
            }

            // 4. 按相似度降序排序
            results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

            // 5. 去重
            results = removeDuplicates(results);

            log.info("匹配完成，共 {} 个结果", results.size());

        } catch (Exception e) {
            log.error("手绘草图匹配失败: ", e);
        }

        return results;
    }

    /**
     * 解析 GeoJSON 数据
     */
    private List<Map<String, Object>> parseGeoJson(Map<String, Object> geoJson) {
        List<Map<String, Object>> buildings = new ArrayList<>();

        try {
            String type = (String) geoJson.get("type");
            if (!"FeatureCollection".equals(type)) {
                return buildings;
            }

            List<Map<String, Object>> features = (List<Map<String, Object>>) geoJson.get("features");
            if (features == null) {
                return buildings;
            }

            for (Map<String, Object> feature : features) {
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                if (geometry == null) {
                    continue;
                }

                String geomType = (String) geometry.get("type");
                List<List<List<Double>>> coordinates = (List<List<List<Double>>>) geometry.get("coordinates");

                if ("Polygon".equals(geomType) && coordinates != null && !coordinates.isEmpty()) {
                    Map<String, Object> building = new HashMap<>();
                    building.put("type", "building");
                    building.put("coordinates", coordinates.get(0)); // 外环坐标
                    buildings.add(building);
                }
            }
        } catch (Exception e) {
            log.error("解析 GeoJSON 失败: ", e);
        }

        return buildings;
    }

    /**
     * 计算手绘草图与真实地图组的相似度
     */
    private double calculateSimilarity(List<Map<String, Object>> buildings, Integer realGroupId) {
        try {
            // 1. 计算手绘草图的中心点和边界
            double[] canvasCenter = calculateCanvasCenter(buildings);
            double[] canvasBounds = calculateCanvasBounds(buildings);

            // 2. 获取真实地图组的中心点和边界
            double[] realCenter = calculateRealGroupCenter(realGroupId);
            double[] realBounds = calculateRealGroupBounds(realGroupId);

            // 3. 计算位置相似度（基于中心点距离）
            double positionSim = calculatePositionSimilarity(canvasCenter, realCenter);

            // 4. 计算形状相似度（基于边界框比例）
            double shapeSim = calculateShapeSimilarity(canvasBounds, realBounds);

            // 5. 计算布局相似度（基于建筑物数量和相对位置）
            double layoutSim = calculateLayoutSimilarity(buildings, realGroupId);

            // 6. 综合相似度（加权平均）
            double finalSim = positionSim * 0.4 + shapeSim * 0.3 + layoutSim * 0.3;

            return finalSim;

        } catch (Exception e) {
            log.error("计算相似度失败: ", e);
            return 0.0;
        }
    }

    /**
     * 计算手绘草图中心点
     */
    private double[] calculateCanvasCenter(List<Map<String, Object>> buildings) {
        double sumLng = 0, sumLat = 0;
        int count = 0;

        for (Map<String, Object> building : buildings) {
            List<List<Double>> coords = (List<List<Double>>) building.get("coordinates");
            if (coords != null) {
                for (List<Double> point : coords) {
                    if (point.size() >= 2) {
                        sumLng += point.get(0);
                        sumLat += point.get(1);
                        count++;
                    }
                }
            }
        }

        return count > 0 ? new double[]{sumLng / count, sumLat / count} : new double[]{0, 0};
    }

    /**
     * 计算手绘草图边界
     */
    private double[] calculateCanvasBounds(List<Map<String, Object>> buildings) {
        double minLng = Double.MAX_VALUE, maxLng = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE;

        for (Map<String, Object> building : buildings) {
            List<List<Double>> coords = (List<List<Double>>) building.get("coordinates");
            if (coords != null) {
                for (List<Double> point : coords) {
                    if (point.size() >= 2) {
                        minLng = Math.min(minLng, point.get(0));
                        maxLng = Math.max(maxLng, point.get(0));
                        minLat = Math.min(minLat, point.get(1));
                        maxLat = Math.max(maxLat, point.get(1));
                    }
                }
            }
        }

        return new double[]{
            maxLng - minLng,  // 宽度
            maxLat - minLat   // 高度
        };
    }

    /**
     * 计算真实地图组中心点
     */
    private double[] calculateRealGroupCenter(Integer groupId) {
        try {
            // 1. 通过 groupId 获取该组包含的所有节点 ID
            Integer[] groupIdArray = new Integer[]{groupId};
            List<Integer[]> groupIdList = Collections.singletonList(groupIdArray);
            ArrayList<String[]> nodeIdsList = neo4jGetGroupNodesService.getNodeListByIds(groupIdList);

            if (nodeIdsList.isEmpty() || nodeIdsList.get(0).length == 0) {
                return new double[]{0, 0};
            }

            String[] nodeIds = nodeIdsList.get(0);

            // 2. 查询所有节点的坐标（经纬度）
            double sumLng = 0, sumLat = 0;
            int count = 0;

            try (Driver driver = GraphDatabase.driver(InfoCommon.url,
                    AuthTokens.basic(InfoCommon.username, InfoCommon.password));
                 Session session = driver.session()) {

                for (String nodeId : nodeIds) {
                    // 去除引号
                    String cleanNodeId = nodeId.replace("'", "");

                    String query = String.format(
                        "MATCH (n) WHERE n.%s = '%s' AND n.lat IS NOT NULL AND n.lon IS NOT NULL " +
                        "RETURN n.lat as lat, n.lon as lon LIMIT 1",
                        PathCommon.OSMID, cleanNodeId
                    );

                    try (Transaction tx = session.beginTransaction()) {
                        Result result = tx.run(query);
                        if (result.hasNext()) {
                            Record record = result.next();
                            sumLat += record.get("lat").asDouble();
                            sumLng += record.get("lon").asDouble();
                            count++;
                        }
                        tx.commit();
                    }
                }
            }

            return count > 0 ? new double[]{sumLng / count, sumLat / count} : new double[]{0, 0};

        } catch (Exception e) {
            log.error("计算真实地图组中心点失败: ", e);
            return new double[]{0, 0};
        }
    }

    /**
     * 计算真实地图组边界
     */
    private double[] calculateRealGroupBounds(Integer groupId) {
        try {
            // 1. 获取该组的所有节点 ID
            Integer[] groupIdArray = new Integer[]{groupId};
            List<Integer[]> groupIdList = Collections.singletonList(groupIdArray);
            ArrayList<String[]> nodeIdsList = neo4jGetGroupNodesService.getNodeListByIds(groupIdList);

            if (nodeIdsList.isEmpty() || nodeIdsList.get(0).length == 0) {
                return new double[]{0, 0};
            }

            String[] nodeIds = nodeIdsList.get(0);

            // 2. 查询所有节点的坐标，计算边界
            double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
            boolean hasCoords = false;

            try (Driver driver = GraphDatabase.driver(InfoCommon.url,
                    AuthTokens.basic(InfoCommon.username, InfoCommon.password));
                 Session session = driver.session()) {

                for (String nodeId : nodeIds) {
                    String cleanNodeId = nodeId.replace("'", "");

                    String query = String.format(
                        "MATCH (n) WHERE n.%s = '%s' AND n.lat IS NOT NULL AND n.lon IS NOT NULL " +
                        "RETURN n.lat as lat, n.lon as lon LIMIT 1",
                        PathCommon.OSMID, cleanNodeId
                    );

                    try (Transaction tx = session.beginTransaction()) {
                        Result result = tx.run(query);
                        if (result.hasNext()) {
                            Record record = result.next();
                            double lat = record.get("lat").asDouble();
                            double lng = record.get("lon").asDouble();

                            minLng = Math.min(minLng, lng);
                            maxLng = Math.max(maxLng, lng);
                            minLat = Math.min(minLat, lat);
                            maxLat = Math.max(maxLat, lat);
                            hasCoords = true;
                        }
                        tx.commit();
                    }
                }
            }

            if (!hasCoords) {
                return new double[]{0, 0};
            }

            return new double[]{
                maxLng - minLng,  // 宽度（经度范围）
                maxLat - minLat   // 高度（纬度范围）
            };

        } catch (Exception e) {
            log.error("计算真实地图组边界失败: ", e);
            return new double[]{0, 0};
        }
    }

    /**
     * 计算位置相似度
     */
    private double calculatePositionSimilarity(double[] center1, double[] center2) {
        double distance = Math.sqrt(
            Math.pow(center1[0] - center2[0], 2) +
            Math.pow(center1[1] - center2[1], 2)
        );

        // 使用高斯函数将距离转换为相似度（0-1之间）
        return Math.exp(-distance / 100);
    }

    /**
     * 计算形状相似度
     */
    private double calculateShapeSimilarity(double[] bounds1, double[] bounds2) {
        double widthRatio = Math.min(bounds1[0], bounds2[0]) / Math.max(bounds1[0], bounds2[0]);
        double heightRatio = Math.min(bounds1[1], bounds2[1]) / Math.max(bounds1[1], bounds2[1]);

        return (widthRatio + heightRatio) / 2;
    }

    /**
     * 计算布局相似度
     */
    private double calculateLayoutSimilarity(List<Map<String, Object>> buildings, Integer groupId) {
        try {
            // 1. 获取该组包含的所有节点 ID
            Integer[] groupIdArray = new Integer[]{groupId};
            List<Integer[]> groupIdList = Collections.singletonList(groupIdArray);
            ArrayList<String[]> nodeIdsList = neo4jGetGroupNodesService.getNodeListByIds(groupIdList);

            if (nodeIdsList.isEmpty()) {
                return 0.0;
            }

            int realBuildingCount = nodeIdsList.get(0).length;
            int canvasBuildingCount = buildings.size();

            // 2. 基于建筑物数量比例计算相似度
            double countRatio = (double) Math.min(canvasBuildingCount, realBuildingCount) /
                               Math.max(canvasBuildingCount, realBuildingCount);

            return countRatio;

        } catch (Exception e) {
            log.error("计算布局相似度失败: ", e);
            return 0.0;
        }
    }

    /**
     * 去重
     */
    private List<SimilarityResultDTO> removeDuplicates(List<SimilarityResultDTO> list) {
        Set<String> seen = new HashSet<>();
        List<SimilarityResultDTO> uniqueList = new ArrayList<>();

        for (SimilarityResultDTO dto : list) {
            Integer[] sortedArray = dto.getResultArray();
            Arrays.sort(sortedArray);

            String uniqueKey = dto.getSimilarity() + Arrays.toString(sortedArray);

            if (!seen.contains(uniqueKey)) {
                seen.add(uniqueKey);
                uniqueList.add(dto);
            }
        }

        return uniqueList;
    }
}
