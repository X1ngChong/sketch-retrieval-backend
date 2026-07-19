package com.bhui.Service;

import com.bhui.dto.SimilarityResultDTO;

import java.util.List;
import java.util.Map;

/**
 * 手绘草图匹配服务接口
 * @author JXS
 */
public interface CanvasMatchService {
    
    /**
     * 根据手绘 GeoJSON 数据进行匹配
     * @param geoJson 手绘图形的 GeoJSON 数据
     * @param realLabel 真实地图的标签
     * @return 匹配结果列表
     */
    List<SimilarityResultDTO> matchCanvasDrawings(Map<String, Object> geoJson, String realLabel);
}
