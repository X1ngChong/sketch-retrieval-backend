package com.bhui.Util.Group;

import com.bhui.Common.DriverCommon;
import com.bhui.Common.InfoCommon;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.util.List;

/**
 * 运行一个解决全部组问题
 */
public class RunThisForAll{
    public static final String label = "nanjingMESO";

    public static final String LAYER_NAME = label+"Road"; //设置道路节点所在图层的名称

    public static final String BUILDING_LABEL = label+"Build"; //设置道路节点所在图层的名称

    public static final String groupLabel = label+"Group"; //设置组的名称

    public static final String Relationship = "Contain";//真实图谱的关系


    public static void main(String[] args) {
        try (DriverCommon driverCommon = new DriverCommon();
             Driver driver = driverCommon.getGraphDatabase();
             Session session = driver.session()) {
            List<List<Integer>> groupIds = addGroup.createBuildingGroups(InfoCommon.url, InfoCommon.username, InfoCommon.password, BUILDING_LABEL,groupLabel,LAYER_NAME);

            // 查找所有的组节点的ID
            List<Integer> fatherIds  = addGroupBox.fetchFatherIds(session,groupLabel,Relationship);

            //根据组节点ID去设置这个组的范围大小
            addGroupBox.setFatherBbox(session,fatherIds,Relationship);

            addNextToRelation.processBuildingRelationships(session, groupIds,LAYER_NAME);
        }
    }
}
