package com.bhui.controller;

import com.bhui.Service.CanvasMatchService;
import com.bhui.dto.SimilarityResultDTO;
import com.bhui.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 手绘草图匹配控制器
 * @author JXS
 */
@RestController
@RequestMapping("/data")
@Slf4j
public class CanvasMatchController {

    @Autowired
    private CanvasMatchService canvasMatchService;

    /**
     * 手绘草图匹配接口
     * @param request 请求体，包含 geoJson 和 realLabel
     * @return 匹配结果列表
     */
    @PostMapping("/canvasMatch")
    public ResponseData canvasMatch(@RequestBody Map<String, Object> request) {
        log.info("收到手绘草图匹配请求");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> geoJson = (Map<String, Object>) request.get("geoJson");
            String realLabel = (String) request.get("realLabel");

            if (geoJson == null || realLabel == null) {
                return ResponseData.failed("缺少必要参数：geoJson 或 realLabel");
            }

            log.info("realLabel: {}", realLabel);

            List<SimilarityResultDTO> results = canvasMatchService.matchCanvasDrawings(geoJson, realLabel);

            log.info("匹配完成，返回 {} 个结果", results.size());
            return ResponseData.succeed(results);

        } catch (Exception e) {
            log.error("手绘草图匹配失败: ", e);
            return ResponseData.failed("匹配失败: " + e.getMessage());
        }
    }
}
