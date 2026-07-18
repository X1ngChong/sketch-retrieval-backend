package com.bhui.Util.Group;

import com.bhui.Common.InfoCommon;
import com.bhui.Util.JTSUtil.JTSUtil;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

/**
 * 用于最开始的处理将level为5的地物
 * Final ID Lists:
 * [550, 551, 552, 553, 554, 555, 556, 557, 558]
 * [319]
 * [200, 205, 206, 207]
 * [208, 209, 210, 211, 212, 213, 214, 215, 216]
 * [86, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500, 501, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 542, 544, 545, 546, 547, 548]
 * [721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 735, 736, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 755, 757, 758, 759]
 * [760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771]
 * [71, 72, 74, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 807, 808, 809, 810, 811, 812]
 * [230]
 * [229]
 * [658, 659]
 * [564, 565, 566, 567, 568, 569, 570, 572, 573, 574, 575, 576, 577, 579, 580, 581, 582, 583, 584, 585, 586, 589, 590, 591, 592, 593, 594, 595, 598, 599, 600, 601, 602, 603, 604, 605, 606, 609, 610, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 683, 684, 685, 686, 687, 688]
 * [290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 361, 362, 363, 364, 365, 366, 367, 368]
 * [301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 330, 332, 333, 334, 336, 338, 339, 340, 341, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360]
 * [236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 411, 412, 57]
 * [281, 282, 283, 284, 285, 286, 287, 288, 289, 369, 370, 371, 372]
 * [375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 407, 408, 409]
 * [518, 519, 520, 521, 542]
 * [662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681]
 * [689, 690, 691, 692, 693, 695, 696]
 * [11, 12]
 * [374]
 * [661]
 */
public class InitBuilding {
    public static void main(String[] args) {
        // 示例调用，您需要替换为实际的数据库连接信息
        String labelName = "nanjingMESOBuild"; // 替换为您的标签名（如果需要）

        List<List<Integer>> result = nanJingMESOInitAllBuildings(InfoCommon.url, InfoCommon.username, InfoCommon.password, labelName);
        System.out.println("Final ID Lists:");
        for (List<Integer> idList : result) {
            System.out.println(idList);
        }
    }

    public static List<List<Integer>> initAllBuildings(String url, String username, String password, String labelName) {
        // 创建 Neo4j 驱动
        List<List<Integer>> finalIdLists = new ArrayList<>(); // 存储最终的ID列表
        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password))) {
            String getLevelEquals1List = "MATCH (n:"+labelName+") WHERE n.level = 1 and n.geometry IS NOT NULL RETURN n.geometry AS geometry"; // 查询所有level为1的宏观地物
            String getLevelEquals5List = "MATCH (n:"+labelName+") WHERE n.level = 5 AND n.ID IS NOT NULL AND n.geometry IS NOT NULL RETURN n.geometry AS geometry, n.ID AS id"; // 查询所有level为5的微观地物

            List<Integer> allIds = new ArrayList<>(); // 存储所有的ID
            List<String> levelEquals5WTK = new ArrayList<>(); // 存储所有level为5的WKT

            // 获取level为5的地物的ID和几何数据
            try (Session session2 = driver.session()) {
                Result result2 = session2.run(getLevelEquals5List);

                while (result2.hasNext()) {
                    Record record2 = result2.next();
                    levelEquals5WTK.add(record2.get("geometry").asString()); // 填充WKT数组
                    allIds.add(record2.get("id").asInt()); // 填充ID数组
                }
            }

            // 获取level为1的地物并判断是否包含level为5的地物
            try (Session session = driver.session()) {
                Result result = session.run(getLevelEquals1List);

                while (result.hasNext()) {
                    List<Integer> temp = new ArrayList<>();
                    Record record = result.next();
                    String level1Geometry = record.get("geometry").asString(); // 获取level为1的几何数据

                    // 检查level为1的几何是否包含level为5的几何
                    for (int i = 0; i < levelEquals5WTK.size(); i++) {
                        if (JTSUtil.isContains(level1Geometry, levelEquals5WTK.get(i))) { // 如果包含
                            temp.add(allIds.get(i)); // 记录下标
                            System.out.println(allIds.get(i));
                        }
                    }
                    if (!temp.isEmpty()) {
                        finalIdLists.add(temp); // 仅在temp不为空时添加到finalIdLists
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // 捕获并打印异常
        }
        return finalIdLists; // 返回最终的ID列表
    }

    public static List<List<Integer>> nanJingMESOInitAllBuildings(String url, String username, String password, String labelName) {
        List<List<Integer>> finalIdLists = new ArrayList<>();

        try (Driver driver = GraphDatabase.driver(url, AuthTokens.basic(username, password))) {
            // 查询所有level为1的地物
            String getLevelEquals1List =
                    "MATCH (n:" + labelName + ") WHERE n.level = 1 AND n.geometry IS NOT NULL RETURN n.geometry AS geometry,n.ID AS id";

            // 对于每个level为1的地物，查找与其具有InitNear关系的level为5的节点
            try (Session session = driver.session()) {
                Result level1Result = session.run(getLevelEquals1List);

                while (level1Result.hasNext()) {
                    Record level1Record = level1Result.next();
                    String level1Geometry = level1Record.get("geometry").asString();
                    Integer level1ID = level1Record.get("id").asInt();


                    // 查找与当前level为1地物具有InitNear关系的level为5节点
                    String findInitNearLevel5NodesQuery =
                            "MATCH (n:" + labelName + "{level: 1, ID: $ID})-[:InitNear]-(m:" + labelName + ") " +
                                    "WHERE m.level = 5 AND m.geometry IS NOT NULL " +
                                    "RETURN m.ID AS id, m.geometry AS geometry";

                    List<Integer> temp = new ArrayList<>();

                    // 在同一个session中执行子查询
                    Result initNearResult = session.run(findInitNearLevel5NodesQuery,
                            parameters("ID", level1ID));

                    while (initNearResult.hasNext()) {
                        Record initNearRecord = initNearResult.next();
                        String level5Geometry = initNearRecord.get("geometry").asString();

                        // 检查level为1的几何是否包含InitNear关系的level为5节点的几何
                        if (JTSUtil.isContains(level1Geometry, level5Geometry)) {
                            int id = initNearRecord.get("id").asInt();
                            temp.add(id);
                            System.out.println(id);
                        }
                    }

                    if (!temp.isEmpty()) {
                        finalIdLists.add(temp);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return finalIdLists;
    }
}