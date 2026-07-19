package com.bhui.controller;

import com.bhui.Common.InfoCommon;
import com.bhui.Util.Group.auotoGroup.AddGroup;
import com.bhui.Util.Group.auotoGroup.AddGroupBox;
import com.bhui.Util.Group.auotoGroup.AddNextToRelation;
import com.bhui.Util.Group.auotoGroup.AdjustGroup;
import com.bhui.Util.Next_TO.autoNextTo.AddLocationInNextToRelation;
import com.bhui.Util.Next_TO.autoNextTo.AddOrderListInNextToRelation;
import com.bhui.Util.Next_TO.autoNextTo.AddRoadIdInNextToRelation;
import com.bhui.Util.Next_TO.autoNextTo.AddTypeOrderListInNextToRelation;
import com.bhui.Util.addNearRelation.autoNearRelastion.AddNearRelation;
import com.bhui.Util.file.FileDownloader;
import com.bhui.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.neo4j.driver.Values.parameters;


@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Value("${spring.neo4j.uri}")
    private String neo4jUri;

    @Value("${spring.neo4j.authentication.username}")
    private String neo4jUsername;

    @Value("${spring.neo4j.authentication.password}")
    private String neo4jPassword;

    // 文件上传目录
    private static final String UPLOAD_DIR = "D:/download/file/upload/";

    /**
     * 文件上传接口 - 保存上传的文件到本地
     * @param file 上传的文件
     * @return 文件本地路径
     */
    @PostMapping("/upload")
    public ResponseData<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 确保上传目录存在
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            // 生成唯一文件名，保留原始扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String savedFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件到本地
            Path targetPath = Paths.get(UPLOAD_DIR, savedFilename);
            file.transferTo(targetPath.toFile());

            log.info("文件上传成功: {}", targetPath);

            // 返回本地文件路径（供 initNeo4j 使用）
            return ResponseData.succeed(targetPath.toString());
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            return ResponseData.failed("文件上传失败: " + e.getMessage());
        }
    }

    /**
     *
     * @param request
     * @param index   用于判断是草图还是osm
     * @return
     */
    @PostMapping("/initNeo4j")
    public ResponseData<String> initNeo4j(@RequestBody Map<String, String> request, @RequestParam String index ) {
        String fileUrl = request.get("fileUrl");
        log.info("initNeo4j fileUrl: {}", fileUrl);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1).replace("\\", "/");
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        String path = "D:/download/file/test/";
        String localFilePath = "D:/download/file/test/" + fileName;

        try {

            // 确保目录存在
            Files.createDirectories(Paths.get(path));

            // 判断是本地路径还是远程URL
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // 远程URL，下载文件到本地
                FileDownloader.downloadFile(fileUrl, localFilePath);
            } else {
                // 本地路径，直接复制文件
                Files.copy(Paths.get(fileUrl), Paths.get(localFilePath));
            }

            // 解压文件到本地
            String unzipDir = path+"unzipped/"+fileName.split("_")[0]; // 修改为适当的路径
           log.info(unzipDir);
            unzipFile(localFilePath, unzipDir);


            // 获取解压后的文件夹中的下级目录名称
            String subDirectories = getFirstSubDirectory(unzipDir);
            log.info("下级目录: " + subDirectories);

            // 获取下级目录中的唯一文件名称
            Set<String> uniqueFileNames = new HashSet<>();
                String subDirPath = unzipDir + "/" + subDirectories;
                uniqueFileNames.addAll(getUniqueFileNames(subDirPath));

            log.info("唯一文件名称: " + uniqueFileNames);



            // 判断文件是道路还是地物
            String roadFileName = null;
            String featureFileName = null;
            String groupRelationShip = "Contain";

            for(String name : uniqueFileNames){
                if (name.toLowerCase().contains("road")) {
                    roadFileName = name+"Road";
                    importShp(roadFileName,subDirPath+"\\"+name+".shp");
                } else {
                    featureFileName = name;
                    importShp(featureFileName+"Build",subDirPath+"\\"+name+".shp");
                }
            }

            //在这里调用构建组的方法！！！
            createGroup(InfoCommon.url,InfoCommon.username,InfoCommon.password,featureFileName,roadFileName,groupRelationShip,Integer.parseInt(index));


            return ResponseData.succeed(featureFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseData.failed("Neo4j处理失败: " + e.getMessage());
        }
        }

    public static void createGroup(String url , String username,String password,String featureFileName,String roadFileName,String groupRelationShip,int index){
        log.info(featureFileName+"      "+roadFileName);
        AddGroup.addGroups(url,username,password,featureFileName,roadFileName,index);
        log.info("创建组节点");

        AdjustGroup.adjustGroups(url,username,password,featureFileName,roadFileName,index);
        log.info("调整组节点");

        AddGroupBox.setGroupBox(url,username,password,featureFileName,groupRelationShip);
        log.info("设置组节点包围盒");

        AddNextToRelation.createNextToRelations(url,username,password,featureFileName,roadFileName,groupRelationShip,index);
        log.info("创建组相邻关系");

        AddNearRelation.calculateAndAddNearRelations(url,username,password,featureFileName);
        log.info("创建Near关系");

        AddLocationInNextToRelation.addLocation(url,username,password,featureFileName);
        log.info("创建NextTo的方位关系");

        AddRoadIdInNextToRelation.addRoadId(url,username,password,featureFileName,roadFileName,index);
        log.info("添加NextTo上的道路ID关系");

        AddOrderListInNextToRelation.updateOrderList(url,username,password,featureFileName,roadFileName);
        log.info("添加NextTo上的地物ID列表投影关系");

        AddTypeOrderListInNextToRelation.updateAllTypeOrderLists(url,username,password);
        log.info("添加NextTo上的地物名称列表投影关系");



    }
        public void importShp(String fileName , String localFilePath){

            // 调用Neo4j过程
            try (Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
                 Session session = driver.session()) {
                String cypherQuery1 ="CALL spatial.addWKTLayer('"+fileName+"','geometry') ";
                session.run(cypherQuery1);
                log.info("图层创建完毕");

                String cypherQuery2 ="CALL spatial.importShapefileToLayer('"+fileName+"', $filePath)";
                session.run(cypherQuery2, parameters( "filePath", localFilePath));
                log.info("shp文件导入完毕");

                String cypherQuery3 ="MATCH (n:SpatialLayer {layer: '"+fileName+"'})-[]->(m)-[]->(x) " +
                        " SET m:"+fileName+", x:"+fileName+" " +
                        " RETURN m,x ";
                session.run(cypherQuery3);

                String cypherQuery4 =" MATCH (n:SpatialLayer {layer: '"+fileName+"'})-[]->(m)-[]->(x)-[]->(z) " +
                        " SET m:"+fileName+", x:"+fileName+",z:"+fileName+" " +
                        " RETURN m,x,z ";
                session.run(cypherQuery4);

                log.info("标签设置完毕");
        }

        }


    public static void unzipFile(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(dir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            log.error("Error while unzipping file: " + e.getMessage());
            throw e;
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private String getFirstSubDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        return file.getName(); // 返回第一个找到的下级目录名称
                    }
                }
            }
        }
        return null; // 如果没有找到下级目录，则返回 null
    }

    private Set<String> getUniqueFileNames(String directoryPath) {
        File dir = new File(directoryPath);
        Set<String> uniqueFileNames = new HashSet<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    uniqueFileNames.add(file.getName().split("\\.")[0]);
                }
            }
        }
        return uniqueFileNames;
    }

    /**
     * 获取可用的地图文件列表
     * @param type 文件类型：weiguan(微观) 或 zhongguan(中观)
     * @return 文件名列表（不含.zip扩展名）
     */
    @GetMapping("/list")
    public ResponseData<List<String>> getFileList(@RequestParam(defaultValue = "weiguan") String type) {
        try {
            // 使用 ClassPathResource 读取 classpath 下的文件
            String pattern = "classpath:data/" + type + "/*.zip";
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(pattern);
            
            List<String> fileNames = new ArrayList<>();
            
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".zip")) {
                    // 去掉.zip扩展名
                    fileNames.add(filename.substring(0, filename.length() - 4));
                }
            }
            
            log.info("获取文件列表 [{}]: {}", type, fileNames);
            return ResponseData.succeed(fileNames);
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage());
            return ResponseData.failed("获取文件列表失败: " + e.getMessage());
        }
    }
}
