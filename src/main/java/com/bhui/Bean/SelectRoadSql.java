package com.bhui.Bean;

/**
 * 设置道路等级的查询语句的类
 */
public class SelectRoadSql {
    public static final String roadLevel =   " WHERE node.level = 5 OR node.level = 2 " ;

    public static final String S9roadLevel =   " WHERE node.level = 1 OR node.level = 2 " ;

}
