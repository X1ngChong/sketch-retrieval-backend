# 项目 Wiki - 旅行规划与地图相似度分析系统

> 本文档用于描述整个工作区的项目架构、技术栈、模块功能，供后续研究与需求调整参考。

---

## 一、项目总览

本工作区包含 **2个独立项目**，共 **2个服务**：

| 服务 | 路径 | 类型 | 端口 | 说明 |
|------|------|------|------|------|
| travel-platform | `d:\arcEngineDemo\travel-platform` | Spring Boot (Java 17) | 9090 | 旅游规划 + 地图相似度计算（合并后端） |
| travel-frontend | `d:\arcEngineDemo\travel-frontend` | Vue 3 + Vite | 默认5173 | 前端地图展示与交互 |

---

## 二、服务1：travel-platform（合并后端）

### 2.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.4 | Web框架 |
| Java | 17 | 运行环境 |
| Spring Data Neo4j | - | Neo4j图数据库操作 |
| Spring Data Redis | - | Redis缓存 |
| JTS Topology Suite | 1.19.0 | 空间几何计算 |
| proj4j | 0.1.0 | 坐标投影转换 |
| Apache HttpClient | 4.5.13 | HTTP请求（调用高德API） |
| FastJSON | 1.2.83 | JSON解析 |
| Gson | 2.10.1 | JSON解析 |
| Lombok | 1.18.34 | 简化代码 |
| Spring Actuator | - | 健康检查/监控 |

### 2.2 项目结构

```
travel-platform/src/main/java/com/bhui/
├── MainApplication.java                  # 启动类
├── Bean/                                 # 实体类（similarity模块）
│   ├── GroupLocationRelationship.java    # 组-位置关系
│   ├── GroupMap.java                     # 组映射
│   ├── GroupRelationship.java            # 组关系
│   ├── Line.java / Point.java            # 几何实体
│   ├── Pair.java                         # 键值对
│   ├── PathResult.java                   # 路径结果
│   ├── RealNodeInfo.java                 # 真实节点信息
│   └── SelectRoadSql.java               # 道路查询SQL
├── Common/                               # 公共常量/工具
│   ├── DriverCommon.java                 # Neo4j驱动常量
│   ├── ImprotLabel.java                  # 导入标签常量
│   ├── InfoCommon.java                   # Neo4j连接信息
│   └── PathCommon.java                   # 路径相关常量
├── Service/                              # 服务层（similarity模块）
│   ├── Neo4jService.java                 # Neo4j核心服务接口
│   ├── Neo4jGetGroupNodesService.java    # 组节点查询服务
│   ├── Neo4jImportService.java           # 数据导入服务
│   ├── PartService.java                  # 局部相似度计算服务
│   ├── ImportData.java                   # Shapefile导入接口
│   └── impl/
│       ├── Neo4jServiceImpl.java         # Neo4j服务实现
│       ├── Neo4jGetGroupNodesServiceImpl.java
│       ├── PartServiceImpl.java          # 局部相似度实现
│       └── ImportDataImpl.java           # 数据导入实现
├── controller/                           # 控制器（similarity模块）
│   ├── DataController.java               # 数据查询API (/data/*)
│   ├── ImportController.java             # 数据导入API (/import/*)
│   ├── FileController.java               # 文件操作API
│   └── RedisController.java              # Redis操作API
├── config/
│   ├── RedisConfig.java                  # Redis配置
│   └── WebConfig.java                    # Web跨域配置（已合并）
├── redis/
│   └── RedisService.java                 # Redis操作服务
├── dto/
│   └── SimilarityResultDTO.java          # 相似度结果DTO
├── handle/
│   ├── CustomException.java              # 自定义异常
│   ├── ErrorResponse.java                # 错误响应
│   └── GlobalExceptionHandler.java       # 全局异常处理
├── response/
│   └── ResponseData.java                 # 统一响应封装
├── Util/                                 # 工具类（similarity模块）
│   ├── Group/                            # 分组相关工具
│   ├── JTSUtil/                          # JTS空间计算工具
│   ├── Next_TO/                          # 邻接关系工具
│   ├── addNearRelation/                  # 邻近关系工具
│   ├── file/                             # 文件下载工具
│   ├── list/                             # 列表工具
│   ├── location/                         # 方位相似度工具
│   ├── matrix/                           # 矩阵打印工具
│   ├── CalculateDistanceByBboxUtil.java  # 包围盒距离计算
│   ├── CalculateLocation.java            # 位置计算
│   ├── CommonUtil.java                   # 通用工具
│   ├── Neo4jCalculatePointUtil.java      # Neo4j点计算
│   ├── ProjectionConverter.java          # 投影转换器
│   └── RoadUtils.java                    # 道路工具
├── travel/                               # 新增：travel模块（从travel-backend合并）
│   ├── controller/
│   │   ├── TourController.java           # 旅游规划API (/api/*)
│   │   └── ShapefileController.java      # Shapefile文件下载 (/file/*)
│   ├── service/
│   │   └── TourService.java              # 旅游规划核心逻辑（高德API调用）
│   ├── entity/
│   │   └── Location.java                 # 经纬度实体
│   └── bean/
│       ├── CityAttractions.java          # 城市景点实体
│       └── RouteRequest.java             # 路线请求实体
└── demo/                                 # 演示/测试代码
    ├── NewDemo/
    ├── before/
    ├── overall/                          # 全局相似度计算
    │   ├── NewDemoRun/
    │   └── impl/matrix/                  # 矩阵实现
    └── part/                             # 局部相似度计算
```

### 2.3 核心API

#### 2.3.1 旅游规划API（travel模块）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/attractions?city=xxx` | GET | 获取城市景点列表（高德POI搜索） |
| `/api/route-cities?origin=xx&destination=xx` | GET | 获取驾车路线途经城市 |
| `/api/route-cities2?origin=xx&destination=xx` | GET | 获取路线城市及坐标 |
| `/api/getLocationByAddress?address=xx` | GET | 地址转坐标（地理编码） |
| `/file/content?filename=xx` | GET | 下载本地Shapefile文件（weiguan目录） |
| `/file/content2?filename=xx` | GET | 下载本地Shapefile文件（zhongguan目录） |

#### 2.3.2 相似度计算API（similarity模块）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/data/S10AndXianLin2?caoTuLabel=xx&realLabel=xx` | GET | 获取两组标签的路径匹配结果 |
| `/data/OverAll?caoTuLabel=xx&realLabel=xx` | GET | 全局相似度计算（含去重） |
| `/data/partMethod?caoTuLabel=xx&realLabel=xx` | GET | 标志性地物局部匹配 |
| `/data/getPartSim1Map?caoTuLabel=xx&realLabel=xx` | GET | 局部相似度1（Map形式） |
| `/data/getPartSim1?caoTuLabel=xx&realLabel=xx` | GET | 局部相似度1（列表形式） |
| `/data/getPartSim2?caoTuLabel=xx&realLabel=xx` | GET | 局部相似度2 |
| `/data/getPartSim3?caoTuLabel=xx&realLabel=xx` | GET | 局部相似度3 |
| `/import/shapefile?layerName=xx&shapFilePath=xx` | POST | 导入Shapefile到Neo4j |

#### 2.3.3 相似度API返回格式

**`/data/OverAll`、`/data/getPartSim` 等相似度接口返回格式：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "similarity": 0.111,
      "resultArray": [44, 20],
      "resultRoadArray": [175, 176, 177]
    },
    {
      "similarity": 0.095,
      "resultArray": [45, 21],
      "resultRoadArray": [180, 181]
    }
  ]
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `similarity` | Double | 相似度值，范围 0-1，值越大表示越相似 |
| `resultArray` | Integer[] | 匹配的节点ID对，`[groupId1, groupId2]` 表示两组地物的匹配 |
| `resultRoadArray` | Integer[] | 对应的道路ID列表，表示匹配地物涉及的道路 |

**`/data/S10AndXianLin2` 返回格式：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    [44, 20],
    [45, 21]
  ]
}
```

返回 `ArrayList<Integer[]>`，每个元素是匹配的节点ID对。

### 2.4 核心业务逻辑

#### 2.4.1 Neo4j图数据模型

系统使用Neo4j存储地图空间数据，核心概念：

- **节点标签**：通过 `caoTuLabel`（草图标签）和 `realLabel`（真实标签）区分不同地图数据源
- **关系类型**：
  - `NEAR`：邻近关系，包含 `location`（方位）、`order`（顺序）、`distance`（距离）属性
  - `NEXT_TO`：邻接关系，包含 `typeOrderList`（类型顺序列表）属性
- **节点属性**：`type`/`fclass`（地物类型）、`osmId`（OSM ID）等

#### 2.4.2 相似度计算维度

| 维度 | 方法 | 说明 |
|------|------|------|
| 方位相似度 | `getPartLocationSimByType` | 基于地物方位（东/南/西/北）的相似度 |
| 距离相似度 | `getPartDistance` | 基于地物间距离的相似度 |
| 顺序相似度(Near) | `getPartOrderSimByNear` | 基于NEAR关系中的顺序相似度 |
| 顺序相似度(NextTo) | `getPartOrderSimByNextTo` | 基于NEXT_TO关系的类型顺序列表相似度（使用LCSS/自定义算法） |
| 全局相似度 | `GetFinalResultByMatrix` | 通过矩阵运算合并多维度相似度 |

#### 2.4.3 数据分组与关系构建

工具类中包含了完整的数据预处理流程：

1. **分组**（`Group/`）：将地图要素按空间关系分组（`addGroup` → `addGroupBox` → `addNextToRelation` → `adjustGroup`）
2. **邻接关系**（`Next_TO/`）：构建NEXT_TO关系，添加道路ID、位置、顺序、类型顺序列表
3. **邻近关系**（`addNearRelation/`）：构建NEAR关系，计算方位、距离、顺序
4. **特定区域处理**：针对 `nanjingMESO`、`xianlinhu`、`S9` 等特定区域有专门的处理类

### 2.5 配置文件

```yaml
server:
  port: 9090
  tomcat:
    connection-timeout: 30000
spring:
  data:
    redis:
      port: 6379
      host: localhost
      password: 123456
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: 198234bh
```

### 2.6 外部依赖

- **Neo4j**：图数据库，存储地图空间关系（本地 bolt://localhost:7687）
- **Redis**：缓存计算结果（本地 localhost:6379）
- **高德地图API**：POI搜索、驾车路线规划、地理编码/反编码
- **本地资源文件**：`resources/data/weiguan/` 和 `resources/data/zhongguan/` 下的Shapefile压缩包

---

## 三、服务2：travel-frontend（前端）

### 3.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.2.13 | 前端框架 |
| Vuetify | 3.x | UI组件库 |
| Vue Router | 4.4.5 | 路由管理 |
| Leaflet | 1.9.4 | 地图展示 |
| 高德地图JS API | - | 地图展示（AMap） |
| shpjs | 6.1.0 | Shapefile解析 |
| axios | 1.7.8 | HTTP请求 |
| TailwindCSS | 3.4.12 | 样式框架 |
| Vite | 3.x | 构建工具 |
| p-limit | 6.1.0 | 并发控制 |

### 3.2 项目结构

```
travel-frontend/src/
├── App.vue                    # 根组件（包含侧边栏）
├── main.js                    # 入口文件
├── mapConfig.js               # 地图配置
├── linux.js                   # Linux相关配置/脚本
├── components/
│   ├── Sidebar.vue            # 侧边栏导航
│   ├── Attractions.vue        # 首页 - 景点展示
│   ├── GetCity.vue            # 城市路线规划
│   ├── GetCity2.vue           # 城市路线规划2
│   ├── Sketch.vue             # 相似度比较（微观）
│   ├── Sketch2.vue            # 中观相似度比较
│   ├── Customize.vue          # 自定义匹配（Windows运行）
│   └── HelloWorld.vue         # 默认组件
├── views/
│   └── HomeView.vue           # 首页视图
├── router/
│   └── index.js               # 路由配置
├── plugins/
│   ├── vuetify.js             # Vuetify插件
│   └── webfontloader.js       # 字体加载
├── page/
│   └── methodImpl.ts          # 方法实现（TypeScript）
└── assets/
    ├── logo.png
    └── logo.svg
```

### 3.3 页面路由

| 路由 | 组件 | 说明 |
|------|------|------|
| `/` | Attractions.vue | 首页，景点展示 |
| `/sketch` | Sketch.vue | 相似度比较（微观层面） |
| `/sketch2` | Sketch2.vue | 中观相似度比较 |
| `/customize` | Customize.vue | 自定义匹配功能 |
| `/cityRoutePlanner` | GetCity.vue | 城市路线规划（已注释） |
| `/cityRoutePlanner2` | GetCity2.vue | 城市路线规划2（已注释） |

> 注：侧边栏中"城市路线规划"相关路由已被注释，当前仅显示首页、相似度比较、中观相似度比较和自定义匹配四个入口。

### 3.4 前端与后端交互

前端通过 axios 调用后端服务（统一端口9090）：

- **travel-platform (9090)**：获取景点数据、路线规划、文件下载、相似度计算结果

### 3.5 前端启动说明

#### 环境要求
- Node.js >= 16.x
- npm >= 8.x

#### 安装依赖
```bash
cd d:\arcEngineDemo\travel-frontend
npm install
```

#### 启动开发服务器
```bash
npm run dev
```
启动后访问：http://localhost:5173

#### 构建生产版本
```bash
npm run build
```
构建产物输出到 `dist/` 目录

#### 预览生产版本
```bash
npm run serve
```

#### 注意事项
- 前端默认端口 5173，可在 `vite.config.js` 中修改
- 需要确保后端服务已启动（9090）
- 高德地图 API Key 配置在 `src/mapConfig.js` 中

---

## 四、系统架构图

```
┌─────────────────────────────────────────────────────┐
│              travel-frontend (5173)                  │
│  ┌──────────┬──────────┬──────────┬──────────┐      │
│  │ 首页     │ 相似度   │ 中观比较 │ 自定义   │      │
│  │景点展示  │ 比较     │          │ 匹配     │      │
│  └────┬─────┴────┬─────┴────┬─────┴────┬─────┘      │
└───────┼──────────┼──────────┼──────────┼────────────┘
        │          │          │          │
        └──────────┴──────────┴──────────┘
                      │
        ┌─────────────▼─────────────┐
        │   travel-platform (9090)  │
        │   (Spring Boot 合并后端)   │
        │                           │
        │  ┌─────────┐  ┌────────┐  │
        │  │ Neo4j   │  │ Redis  │  │
        │  │(图数据库)│  │ (缓存) │  │
        │  └─────────┘  └────────┘  │
        │                           │
        │  ┌─────────┐  ┌────────┐  │
        │  │ JTS     │  │高德API │  │
        │  │空间计算  │  │(地图)  │  │
        │  └─────────┘  └────────┘  │
        └───────────────────────────┘
```

---

## 五、核心业务流程

### 5.1 地图相似度分析流程

```
1. 数据准备阶段
   ├── Shapefile数据导入 → Neo4j图数据库
   ├── 构建分组关系（Group）
   ├── 构建NEAR关系（方位、距离、顺序）
   └── 构建NEXT_TO关系（邻接、类型顺序）

2. 相似度计算阶段
   ├── 方位相似度计算
   ├── 距离相似度计算
   ├── 顺序相似度计算
   ├── 邻接顺序相似度计算（LCSS/自定义算法）
   └── 矩阵合并 → 全局相似度

3. 结果展示阶段
   ├── 后端返回相似度结果 + OBJECTID
   ├── 前端通过文件接口下载Shapefile
   ├── 前端解析Shapefile渲染地图
   └── 对比展示草图与真实地图的相似度
```

### 5.2 旅游规划流程

```
1. 用户输入城市名
2. 调用高德API获取景点列表
3. 用户选择起点和终点
4. 调用高德驾车路线规划API
5. 提取途经城市及坐标
6. 前端地图展示路线
```

---

## 六、数据说明

### 6.1 Shapefile数据

- **weiguan（微观）目录**：S1-S13、nanjingMESO、xianlin 等
- **zhongguan（中观）目录**：S2-S9、S10、S14-S15、jianye1-2、nanjingMESO、xianlin 等

### 6.2 Neo4j图数据

- 节点标签：对应不同地图数据源（如 `nanjingMESO`、`xianlin`、`S9` 等）
- 关系类型：`NEAR`（邻近）、`NEXT_TO`（邻接）
- 节点属性：`type`/`fclass`（地物分类）、`osmId`（OpenStreetMap ID）

---

## 七、开发注意事项

1. **两个服务需同时运行**：travel-frontend(5173) + travel-platform(9090)
2. **Neo4j和Redis需预先启动**：travel-platform依赖本地Neo4j(7687)和Redis(6379)
3. **高德API Key**：硬编码在 `TourService.java` 中（`538ad44097e0b56a7e4c4ef7dce5a3c8`）
4. **数据目录**：`resources/data/` 下的Shapefile压缩包是核心数据，勿删除
5. **demo包**：项目中有大量 `demo/` 和 `Util/` 下的实验性代码，部分可能未在生产使用

---

## 八、关键类索引

| 类名 | 位置 | 作用 |
|------|------|------|
| TourService | travel-platform (travel模块) | 高德API调用封装 |
| ShapefileController | travel-platform (travel模块) | Shapefile文件下载 |
| TourController | travel-platform (travel模块) | 旅游规划API |
| DataController | travel-platform (similarity模块) | 相似度查询API |
| Neo4jServiceImpl | travel-platform (similarity模块) | Neo4j查询核心实现 |
| PartServiceImpl | travel-platform (similarity模块) | 局部相似度计算 |
| GetFinalResultByMatrix | travel-platform (similarity模块) | 矩阵方式计算全局相似度 |
| ImportDataImpl | travel-platform (similarity模块) | Shapefile导入Neo4j |
| RedisService | travel-platform (similarity模块) | 计算结果缓存 |
| Sketch/Sketch2 | travel-frontend | 前端相似度展示页面 |
| Customize | travel-frontend | 前端自定义匹配页面 |

---

## 九、合并历史

本项目由以下两个项目合并而成：

1. **travel-backend**（旅游规划后端，端口9091）
   - 提供旅游规划、景点查询、路线规划、文件下载功能
   - 调用高德地图API
   - 包名：`com.bhui.mytravel`

2. **similarity-backend**（相似度计算后端，端口9090）
   - 提供地图空间数据管理、Neo4j图数据库操作、相似度计算功能
   - 使用JTS进行空间几何计算
   - 包名：`com.bhui`

**合并后：**
- 统一使用 `travel-platform` 名称，端口9090
- travel模块迁移到 `com.bhui.travel` 包
- similarity模块保留在 `com.bhui` 包
- 合并了pom.xml依赖、WebConfig配置、资源文件
