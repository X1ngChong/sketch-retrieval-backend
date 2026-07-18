package com.bhui.Common;


import com.bhui.Util.Group.xianlinhu.RunThisForAllXianLinHuGroup;
import com.bhui.Util.Next_TO.xianlinhu.RunThisForALLXianLinHuNextTo;

public class ImprotLabel {
//    public static final String label = "S9";
//
//    public static final String LAYER_NAME = label+"Road2"; //设置道路节点所在图层的名称
//
//    public static final String buildingLabel = label+"Build2"; //设置道路节点所在图层的名称
//
//    public static final String groupLabel = label+"Group2"; //设置组的名称

    public static final String label = "jianye";

    public static final String LAYER_NAME = label+"Road2"; //设置道路节点所在图层的名称

    public static final String buildingLabel = label+"Build2"; //设置道路节点所在图层的名称

    public static final String groupLabel = label+"Group2"; //设置组的名称

    public final static String Relationship = "Contain";//真实图谱的关系

    public static void main(String[] args) throws Exception {
        RunThisForAllXianLinHuGroup.main(args);
        RunThisForALLXianLinHuNextTo.main(args);
    }

}
