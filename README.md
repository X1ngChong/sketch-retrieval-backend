# Sketch Retrieval Backend

草图检索系统后端服务

## 项目简介

这是一个基于 Spring Boot 的草图检索系统后端，提供地图空间数据管理、相似度计算、旅游规划等功能。

## 技术栈

- Java 17
- Spring Boot 3.3.4
- Neo4j (图数据库)
- Redis (缓存)
- JTS Topology Suite (空间计算)
- 高德地图 API

## 主要功能

### 1. 旅游规划模块
- 城市景点查询
- 路线规划
- 地理编码/反编码
- Shapefile 文件下载

### 2. 相似度计算模块
- 地图空间数据导入
- 多维度相似度计算（方位、距离、顺序、邻接）
- 全局相似度匹配
- Neo4j 图数据库操作

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- Neo4j 5.x
- Redis 6.x

### 安装依赖

```bash
mvn clean install
```

### 运行项目

```bash
mvn spring-boot:run
```

服务将启动在 http://localhost:9090

## API 接口

### 旅游规划 API
- `GET /api/attractions?city=xxx` - 获取城市景点
- `GET /api/route-cities?origin=xx&destination=xx` - 路线规划
- `GET /api/getLocationByAddress?address=xx` - 地址转坐标

### 相似度计算 API
- `GET /data/OverAll?caoTuLabel=xx&realLabel=xx` - 全局相似度
- `GET /data/partMethod?caoTuLabel=xx&realLabel=xx` - 局部匹配
- `POST /import/shapefile` - 导入 Shapefile

## 配置说明

编辑 `src/main/resources/application.yml` 配置数据库连接：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your_password
```

## 项目结构

```
src/main/java/com/bhui/
├── travel/              # 旅游规划模块
│   ├── controller/
│   ├── service/
│   ├── entity/
│   └── bean/
├── Service/             # 相似度计算模块
├── controller/
├── config/
├── redis/
├── dto/
├── handle/
├── response/
├── Util/
└── demo/
```

## 许可证

MIT
