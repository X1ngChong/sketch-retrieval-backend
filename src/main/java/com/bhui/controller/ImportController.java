package com.bhui.controller;

import com.bhui.Service.ImportData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导入数据
 * @author JXS
 */
@RestController
@RequestMapping("/import")
@Slf4j
public class ImportController {

    @Autowired
    private ImportData importData;

    @PostMapping("/shapefile")
    public String importShapefile(@RequestParam String layerName, @RequestParam String shapFilePath) {
        log.info("正在导入图层: {} 的 Shapefile，路径为: {}", layerName, shapFilePath);
        try {
            importData.importFileByLayerNameShapFilePath(layerName, shapFilePath);
            log.info("图层: {} 的 Shapefile 导入成功", layerName);
            return "图层: " + layerName + " 的 Shapefile 导入成功";
        } catch (Exception e) {
            log.error("导入图层: {} 的 Shapefile 时发生错误。错误信息: {}", layerName, e.getMessage());
            return "导入 Shapefile 时发生错误: " + e.getMessage();
        }
    }
}
