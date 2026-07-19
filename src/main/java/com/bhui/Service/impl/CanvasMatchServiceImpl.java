package com.bhui.Service.impl;

import com.bhui.Common.InfoCommon;
import com.bhui.Common.PathCommon;
import com.bhui.Service.CanvasMatchService;
import com.bhui.Service.Neo4jGetGroupNodesService;
import com.bhui.Service.Neo4jService;
import com.bhui.Util.CalculateLocation;
import com.bhui.dto.SimilarityResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 手绘草图匹配服务实现
 * 参考现有匹配逻辑：从 bbox 计算 location/distance/order 关系分布，与 Neo4j 中预计算的关系分布做比较
 * @author JXS
 */
@Service
@Slf4j
public class CanvasMatchServiceImpl implements CanvasMatchService {

    @Autowired
    private Neo4jService neo4jService;

    @Autowired
    private Neo4jGetGroupNodesService neo4jGetGroupNodesService;

    // 八方位名称列表
    private static final String[] DIRECTIONS = {"North", "NorthEast", "East", "SouthEast", "South", "SouthWest", "West", "NorthWest"};
    // 距离等级
    private static final String[] DISTANCES = {"SD", "MD", "LD"};
    // 顺序关系
    private static final String[] ORDERS = {"before", "after", "equal"};

    @Override
    public List<SimilarityResultDTO> matchCanvasDrawings(Map<String, Object> geoJson, String realLabel) {
        log.info("开始手绘草图匹配，realLabel: {}", realLabel);

        List<SimilarityResultDTO> results = new ArrayList<>();

        try {
            // 1. 解析 GeoJSON，提取建筑物 bbox
            List<List<Object>> canvasBboxes = parseGeoJsonToBboxes(geoJson);
            log.info("解析到 {} 个手绘建筑物", canvasBboxes.size());

            if (canvasBboxes.isEmpty()) {
                return results;
            }

            // 2. 计算 canvas 建筑物之间的关系分布
            Map<String, Integer> canvasLocationDist = computeLocationDistribution(canvasBboxes);
            Map<String, Integer> canvasDistanceDist = computeDistanceDistribution(canvasBboxes);
            Map<String, Integer> canvasOrderDist = computeOrderDistribution(canvasBboxes);

            log.debug("Canvas 方位分布: {}", canvasLocationDist);
            log.debug("Canvas 距离分布: {}", canvasDistanceDist);
            log.debug("Canvas 顺序分布: {}", canvasOrderDist);

            // 3. 获取真实地图的组 ID
            Integer[] realGroupIds = neo4jService.getGroupIdsByTag(realLabel);
            log.info("真实地图组数量: {}", realGroupIds.length);

            if (realGroupIds.length == 0) {
                return results;
            }

            // 4. 为每个真实地图组计算相似度
            for (Integer realGroupId : realGroupIds) {
                try {
                    // 从 Neo4j 获取该组的建筑物 bbox 列表
                    List<List<Object>> realBboxes = getRealGroupBboxes(realGroupId);

                    if (realBboxes.isEmpty()) {
                        continue;
                    }

                    // 计算真实组的关系分布
                    Map<String, Integer> realLocationDist = computeLocationDistribution(realBboxes);
                    Map<String, Integer> realDistanceDist = computeDistanceDistribution(realBboxes);
                    Map<String, Integer> realOrderDist = computeOrderDistribution(realBboxes);

                    // 计算三种相似度（余弦相似度）
                    double locationSim = computeDistributionSimilarity(canvasLocationDist, realLocationDist, DIRECTIONS);
                    double distanceSim = computeDistributionSimilarity(canvasDistanceDist, realDistanceDist, DISTANCES);
                    double orderSim = computeDistributionSimilarity(canvasOrderDist, realOrderDist, ORDERS);

                    // 加权综合
                    double finalSim = locationSim * 0.4 + distanceSim * 0.3 + orderSim * 0.3;

                    if (finalSim > 0.1) {
                        Integer[] groupIdArray = new Integer[]{realGroupId};
                        List<Integer[]> groupIdList = Collections.singletonList(groupIdArray);

                        ArrayList<Integer[]> objectIdsList = neo4jGetGroupNodesService.getObjectIdByIds(groupIdList);
                        ArrayList<Integer[]> roadIdsList = neo4jGetGroupNodesService.getObjectRoadIdsByIds(groupIdList);

                        Integer[] objectIds = objectIdsList.isEmpty() ? new Integer[0] : objectIdsList.get(0);
                        Integer[] roadIds = roadIdsList.isEmpty() ? new Integer[0] : roadIdsList.get(0);

                        SimilarityResultDTO dto = new SimilarityResultDTO(finalSim, objectIds, roadIds);
                        results.add(dto);
                        log.debug("组 {} 相似度: {:.4f} (方位:{:.3f}, 距离:{:.3f}, 顺序:{:.3f})",
                                realGroupId, finalSim, locationSim, distanceSim, orderSim);
                    }
                } catch (Exception e) {
                    log.error("计算组 {} 相似度时出错: {}", realGroupId, e.getMessage());
                }
            }

            // 5. 按相似度降序排序
            results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

            // 6. 去重
            results = removeDuplicates(results);

            log.info("匹配完成，共 {} 个结果", results.size());

        } catch (Exception e) {
            log.error("手绘草图匹配失败: ", e);
        }

        return results;
    }

    /**
     * 解析 GeoJSON 数据，提取建筑物的 bbox（保持原始经纬度坐标）
     */
    private List<List<Object>> parseGeoJsonToBboxes(Map<String, Object> geoJson) {
        List<List<Object>> bboxes = new ArrayList<>();

        try {
            String type = (String) geoJson.get("type");
            if (!"FeatureCollection".equals(type)) {
                return bboxes;
            }

            List<Map<String, Object>> features = (List<Map<String, Object>>) geoJson.get("features");
            if (features == null || features.isEmpty()) {
                return bboxes;
            }

            for (Map<String, Object> feature : features) {
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                if (geometry == null) continue;

                String geomType = (String) geometry.get("type");
                if ("Polygon".equals(geomType)) {
                    List<List<List<Double>>> coordinates = (List<List<List<Double>>>) geometry.get("coordinates");
                    if (coordinates != null && !coordinates.isEmpty()) {
                        List<List<Double>> outerRing = coordinates.get(0);

                        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
                        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

                        for (List<Double> point : outerRing) {
                            if (point.size() >= 2) {
                                double lng = point.get(0);
                                double lat = point.get(1);
                                minX = Math.min(minX, lng);
                                maxX = Math.max(maxX, lng);
                                minY = Math.min(minY, lat);
                                maxY = Math.max(maxY, lat);
                            }
                        }

                        List<Object> bbox = new ArrayList<>();
                        bbox.add(minX);
                        bbox.add(minY);
                        bbox.add(maxX);
                        bbox.add(maxY);
                        bboxes.add(bbox);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析 GeoJSON 失败: ", e);
        }

        return bboxes;
    }

    /**
     * 从 Neo4j 获取真实地图组的 bbox 列表
     */
    private List<List<Object>> getRealGroupBboxes(Integer groupId) {
        List<List<Object>> bboxes = new ArrayList<>();

        try {
            Integer[] groupIdArray = new Integer[]{groupId};
            List<Integer[]> groupIdList = Collections.singletonList(groupIdArray);
            ArrayList<String[]> nodeIdsList = neo4jGetGroupNodesService.getNodeListByIds(groupIdList);

            if (nodeIdsList.isEmpty() || nodeIdsList.get(0).length == 0) {
                return bboxes;
            }

            String[] osmIds = nodeIdsList.get(0);

            try (Driver driver = GraphDatabase.driver(InfoCommon.url,
                    AuthTokens.basic(InfoCommon.username, InfoCommon.password));
                 Session session = driver.session()) {

                for (String osmId : osmIds) {
                    String cleanOsmId = osmId.replace("'", "");

                    String query = String.format(
                        "MATCH (n) WHERE n.%s = '%s' AND n.bbox IS NOT NULL " +
                        "RETURN n.bbox as bbox LIMIT 1",
                        PathCommon.OSMID, cleanOsmId
                    );

                    try (Transaction tx = session.beginTransaction()) {
                        Result result = tx.run(query);
                        if (result.hasNext()) {
                            Record record = result.next();
                            List<Object> bbox = record.get("bbox").asList();
                            if (bbox != null && bbox.size() == 4) {
                                bboxes.add(bbox);
                            }
                        }
                        tx.commit();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取真实地图组 bbox 失败: ", e);
        }

        return bboxes;
    }

    /**
     * 计算方位关系分布（复用 CalculateLocation.getBaFangWei）
     */
    private Map<String, Integer> computeLocationDistribution(List<List<Object>> bboxes) {
        Map<String, Integer> dist = new HashMap<>();
        for (String d : DIRECTIONS) dist.put(d, 0);

        for (int i = 0; i < bboxes.size(); i++) {
            for (int j = i + 1; j < bboxes.size(); j++) {
                String location = CalculateLocation.getBaFangWei(bboxes.get(i), bboxes.get(j));
                if (location != null && !location.isEmpty()) {
                    dist.put(location, dist.getOrDefault(location, 0) + 1);
                }
                // 反向关系
                String reverseLocation = CalculateLocation.getBaFangWei(bboxes.get(j), bboxes.get(i));
                if (reverseLocation != null && !reverseLocation.isEmpty()) {
                    dist.put(reverseLocation, dist.getOrDefault(reverseLocation, 0) + 1);
                }
            }
        }

        return dist;
    }

    /**
     * 计算距离关系分布（复用 CalculateLocation.getDistanceRelation）
     */
    private Map<String, Integer> computeDistanceDistribution(List<List<Object>> bboxes) {
        Map<String, Integer> dist = new HashMap<>();
        for (String d : DISTANCES) dist.put(d, 0);

        if (bboxes.size() < 2) {
            return dist;
        }

        // 计算所有建筑物对之间的距离
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < bboxes.size(); i++) {
            for (int j = i + 1; j < bboxes.size(); j++) {
                double[] c1 = CalculateLocation.calculateCenterPoint(bboxes.get(i));
                double[] c2 = CalculateLocation.calculateCenterPoint(bboxes.get(j));
                double d = Math.sqrt(Math.pow(c1[0] - c2[0], 2) + Math.pow(c1[1] - c2[1], 2));
                distances.add(d);
            }
        }

        if (distances.isEmpty()) {
            return dist;
        }

        // 找到最大距离用于归一化
        double maxDist = Collections.max(distances);
        if (maxDist == 0) {
            return dist;
        }

        // 归一化并分类
        for (double d : distances) {
            double normalized = d / maxDist;
            String distanceLevel = CalculateLocation.getDistanceRelation(normalized);
            dist.put(distanceLevel, dist.getOrDefault(distanceLevel, 0) + 1);
        }

        return dist;
    }

    /**
     * 计算顺序关系分布（复用 CalculateLocation.getOrder）
     */
    private Map<String, Integer> computeOrderDistribution(List<List<Object>> bboxes) {
        Map<String, Integer> dist = new HashMap<>();
        for (String o : ORDERS) dist.put(o, 0);

        for (int i = 0; i < bboxes.size(); i++) {
            for (int j = i + 1; j < bboxes.size(); j++) {
                String order = CalculateLocation.getOrder(bboxes.get(i), bboxes.get(j));
                if (order != null) {
                    dist.put(order, dist.getOrDefault(order, 0) + 1);
                }
                // 反向
                String reverseOrder = CalculateLocation.getOrder(bboxes.get(j), bboxes.get(i));
                if (reverseOrder != null) {
                    dist.put(reverseOrder, dist.getOrDefault(reverseOrder, 0) + 1);
                }
            }
        }

        return dist;
    }

    /**
     * 计算两个分布之间的相似度（余弦相似度）
     */
    private double computeDistributionSimilarity(Map<String, Integer> dist1, Map<String, Integer> dist2, String[] categories) {
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (String cat : categories) {
            int v1 = dist1.getOrDefault(cat, 0);
            int v2 = dist2.getOrDefault(cat, 0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 去重
     */
    private List<SimilarityResultDTO> removeDuplicates(List<SimilarityResultDTO> list) {
        Set<String> seen = new HashSet<>();
        List<SimilarityResultDTO> uniqueList = new ArrayList<>();

        for (SimilarityResultDTO dto : list) {
            Integer[] sortedArray = dto.getResultArray().clone();
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
