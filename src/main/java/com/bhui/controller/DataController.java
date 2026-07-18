package com.bhui.controller;

import com.bhui.Bean.Pair;
import com.bhui.Service.Neo4jGetGroupNodesService;
import com.bhui.Service.PartService;
import com.bhui.demo.overall.impl.matrix.GetFinalResultByMatrix;

import com.bhui.dto.SimilarityResultDTO;
import com.bhui.redis.RedisService;
import com.bhui.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @author JXS
 */
@RestController
@RequestMapping("/data")
@Slf4j
public class DataController {

    @Autowired
    private PartService partService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private GetFinalResultByMatrix getFinalResultByMatrix;

    @Autowired
    private Neo4jGetGroupNodesService neo4jGetGroupNodesService;
    @GetMapping("/S10AndXianLin2")
    public ResponseData<ArrayList<Integer[]>> getS10AndXianLin2(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"S10AndXianLin2 ");

        String labelName = caoTuLabel+realLabel;
        List<Integer[]> finalResultByMatrix = null;
        finalResultByMatrix = redisService.getIntegerArrays(labelName+"integerArrays");

        if(finalResultByMatrix == null){
            // GetFinalResultByMatrix 获取路径
            finalResultByMatrix  = getFinalResultByMatrix.getFinalResultByMatrix(caoTuLabel,realLabel);
            //存储数据
            if (finalResultByMatrix != null) {
                redisService.saveIntegerArrays(labelName+"integerArrays", finalResultByMatrix);
            }
        }

        //获取OBJECTID作为主键去查找
        ArrayList<Integer[]> list = neo4jGetGroupNodesService.getObjectIdByIds(finalResultByMatrix);
        for (Integer [] temp:list
             ) {
            log.info("结果列表:{}",Arrays.toString(temp));
        }
        return ResponseData.succeed(list);
    }

    @GetMapping("/OverAll")
    public ResponseData OverAll(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"OverAll");


        // 获取相似度和对应的整数数组
        List<Pair<Double, Integer[]>> finalList = getFinalResultByMatrix.getFinalResult(caoTuLabel,realLabel);

        // 将所有需要查询的节点 ID 组合成一个批量查询列表
        List<Integer[]> batchQueryList = new ArrayList<>();
        for (Pair<Double, Integer[]> pair : finalList) {
            batchQueryList.add(pair.getValue());
        }

        // 批量查询所有节点的 OBJECTID
        for (Integer [] temp:batchQueryList){
           // System.out.println(Arrays.toString(temp));
        }
        ArrayList<Integer[]> objectIdResults = neo4jGetGroupNodesService.getObjectIdByIds(batchQueryList);
        ArrayList<Integer[]> objectRoadResults = neo4jGetGroupNodesService.getObjectRoadIdsByIds(batchQueryList);



        List<Double> doubleList = new ArrayList<>(); //存储相似度
        // 转换为 DTO 列表
        List<SimilarityResultDTO> resultDTOList = new ArrayList<>();
        for (int i = 0; i < finalList.size(); i++) {
            Pair<Double, Integer[]> pair = finalList.get(i);
            Integer[] objectIds = objectIdResults.get(i); // 获取对应的 OBJECTID 结果
            Integer[] objectRoadId = objectRoadResults.get(i); // 获取对应的 OBJECTID 结果

            SimilarityResultDTO similarityResultDTO = new SimilarityResultDTO(pair.getKey(), objectIds,objectRoadId);
            resultDTOList.add(similarityResultDTO);
            doubleList.add(pair.getKey());
            log.info("结果列表:{}", similarityResultDTO);
        }

        redisService.saveDoubleArrays(caoTuLabel+realLabel+"OverAllSim",doubleList);//将相似度存储进redis

        List<SimilarityResultDTO> similarityResultDTOS = removeDuplicates(resultDTOList);

        // 返回 DTO 列表给前端
        return ResponseData.succeed(similarityResultDTOS);
    }

    // 辅助方法：去除重复的 DTO
    private  List<SimilarityResultDTO> removeDuplicates(List<SimilarityResultDTO> list) {

        Set<String> seen = new HashSet<>();
        List<SimilarityResultDTO> uniqueList = new ArrayList<>();

        for (SimilarityResultDTO dto : list) {
            // 对结果数组进行排序以确保顺序一致
            Integer[] sortedArray = dto.getResultArray();
            Arrays.sort(sortedArray); // 排序

            // 创建一个唯一的标识符，用于检查重复
            String uniqueKey = dto.getSimilarity() + Arrays.toString(sortedArray);

            // 检查是否已经存在相同的结果
            if (!seen.contains(uniqueKey)) {
                seen.add(uniqueKey); // 添加到已见集合中
                uniqueList.add(dto); // 添加到唯一列表中
            }
        }
        return uniqueList;
    }

    @GetMapping("/partMethod")
    public ResponseData partMethod(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"PartMethod");

        List<String[]> iconicFeatureList = partService.getIconicFeatureList(caoTuLabel,realLabel);
        for (String [] temp:iconicFeatureList) {
            log.info("标志性地物列表 : " + Arrays.toString(temp));
        }
        return ResponseData.succeed();
    }

    @GetMapping("/getPartSim1Map")
    public ResponseData getPartSim1Map(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"getPartSim1Map");

        HashMap<String, Double> partSim1Map = partService.getPartSim1Map(caoTuLabel,realLabel);
        // 遍历 HashMap 的键值对
        for (Map.Entry<String, Double> entry : partSim1Map.entrySet()) {
            String key = entry.getKey(); // 获取键
            Double value = entry.getValue(); // 获取值
            log.info("节点ID: " + key + ", 相似度: " + value);
        }
        return ResponseData.succeed();
    }

    @GetMapping("/getPartSim1")
    public ResponseData getPartSim1(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"getPartSim1");

        List<Double[]> partSim1List = partService.getPartSim1(caoTuLabel,realLabel);
        for (Double[] array : partSim1List) {
            // 计算平均值
            double sum = 0.0;
            int count = 0;
            for (Double value : array) {
                if (value != null) {
                    sum += value;
                    count++;
                }
            }
            double average = count > 0 ? sum / count : 0.0;

            // 记录整个 Double[] 数组和平均值
            log.info("Values: " + Arrays.toString(array) + ", Average: " + average);
        }
        return ResponseData.succeed();
    }

    @GetMapping("/getPartSim2")
    public ResponseData getPartSim2(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"getPartSim2");

        List<Double[]> partSim1List = partService.getPartSim2(caoTuLabel,realLabel);
        for (Double[] array : partSim1List) {
            // 计算平均值
            double sum = 0.0;
            int count = 0;
            for (Double value : array) {
                if (value != null) {
                    sum += value;
                    count++;
                }
            }
            double average = count > 0 ? sum / count : 0.0;

            // 记录整个 Double[] 数组和平均值
            log.info("Values: " + Arrays.toString(array) + ", Average: " + average);
        }
        return ResponseData.succeed();
    }

    @GetMapping("/getPartSim3")
    public ResponseData getPartSim3(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"getPartSim3");

        List<Double[]> partSim1List = partService.getPartSim3(caoTuLabel,realLabel);
        for (Double[] array : partSim1List) {
            // 计算平均值
            double sum = 0.0;
            int count = 0;
            for (Double value : array) {
                if (value != null) {
                    sum += value;
                    count++;
                }
            }
            double average = count > 0 ? sum / count : 0.0;

            // 记录整个 Double[] 数组和平均值
            log.info("Values: " + Arrays.toString(array) + ", Average: " + average);
        }
        return ResponseData.succeed();
    }
    @GetMapping("/getPartSim")
    public ResponseData getPartSim(@RequestParam String caoTuLabel,@RequestParam String realLabel) {
        log.info(caoTuLabel+" "+realLabel+"getPartSim");
        // 获取相似度和对应的整数数组
        List<Pair<Double, Integer[]>> finalList = partService.getFinalList(caoTuLabel,realLabel);

        // 将所有需要查询的节点 ID 组合成一个批量查询列表
        List<Integer[]> batchQueryList = new ArrayList<>();
        for (Pair<Double, Integer[]> pair : finalList) {
            batchQueryList.add(pair.getValue());
        }

        // 批量查询所有节点的 OBJECTID
        ArrayList<Integer[]> objectIdResults = neo4jGetGroupNodesService.getObjectIdByIds(batchQueryList);
        ArrayList<Integer[]> objectRoadResults = neo4jGetGroupNodesService.getObjectRoadIdsByIds(batchQueryList);


        // 转换为 DTO 列表
        List<SimilarityResultDTO> resultDTOList = new ArrayList<>();
        for (int i = 0; i < finalList.size(); i++) {
            Pair<Double, Integer[]> pair = finalList.get(i);
            Integer[] objectIds = objectIdResults.get(i); // 获取对应的 OBJECTID 结果
            Integer[] objectRoadIds = objectRoadResults.get(i); // 获取对应的 OBJECTID 结果

            SimilarityResultDTO similarityResultDTO = new SimilarityResultDTO(pair.getKey(), objectIds,objectRoadIds);
            resultDTOList.add(similarityResultDTO);
            log.info("结果列表:{}", similarityResultDTO);
        }

        List<SimilarityResultDTO> similarityResultDTOS = removeDuplicates(resultDTOList);

        // 返回 DTO 列表给前端
        return ResponseData.succeed(similarityResultDTOS);
    }

}
