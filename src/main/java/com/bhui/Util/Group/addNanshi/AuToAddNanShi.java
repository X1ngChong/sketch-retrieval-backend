package com.bhui.Util.Group.addNanshi;

import com.bhui.Common.InfoCommon;
import com.bhui.controller.FileController;

public class AuToAddNanShi {
    public static final String label = "S9";

    public static final String LAYER_NAME = label+"Road"; //设置道路节点所在图层的名称

    public static  String groupRelationShip = "Contain";

    public static void main(String[] args) {

        FileController.createGroup(InfoCommon.url,InfoCommon.username,InfoCommon.password,label,LAYER_NAME,groupRelationShip,1);

    }
}
