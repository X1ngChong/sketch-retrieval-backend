package com.bhui.travel.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Shapefile文件下载控制器 - 从本地resources/data目录读取zip文件
 * @author JXS
 */
@RestController
@RequestMapping("/file")
public class ShapefileController {

    /**
     * 从本地读取微观(weiguan)目录下的zip文件
     * @param filename 文件名（可省略.zip后缀）
     * @return zip文件二进制流
     */
    @GetMapping("/content")
    public ResponseEntity<byte[]> getFileContent(@RequestParam String filename) {
        return readLocalZip("data/weiguan/", filename);
    }

    /**
     * 从本地读取中观(zhongguan)目录下的zip文件
     * @param filename 文件名（可省略.zip后缀）
     * @return zip文件二进制流
     */
    @GetMapping("/content2")
    public ResponseEntity<byte[]> getFileContent2(@RequestParam String filename) {
        return readLocalZip("data/zhongguan/", filename);
    }

    /**
     * 通用本地zip文件读取方法
     * @param dir 资源目录（如 data/weiguan/ 或 data/zhongguan/）
     * @param filename 文件名
     * @return 文件内容响应
     */
    private ResponseEntity<byte[]> readLocalZip(String dir, String filename) {
        try {
            if (!filename.endsWith(".zip")) {
                filename = filename + ".zip";
            }
            ClassPathResource resource = new ClassPathResource(dir + filename);

            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            byte[] content = resource.getInputStream().readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");

            return ResponseEntity.ok().headers(headers).body(content);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
