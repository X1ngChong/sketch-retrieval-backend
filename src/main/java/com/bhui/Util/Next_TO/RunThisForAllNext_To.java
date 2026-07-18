package com.bhui.Util.Next_TO;

import com.bhui.Common.DriverCommon;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;


/**
 * @author JXS
 */
public class RunThisForAllNext_To {
    public static void main(String[] args) {
        try (DriverCommon driverCommon = new DriverCommon();
             Driver driver = driverCommon.getGraphDatabase();
             Session session = driver.session()) {
            addRoadIdInNextToRelation.addRoadId(session);
            addLocationInNextToRelation.addLocationInNextToRelation(session);
            addOrderListInNextToRelation.processNextToRelations(driver);
            addTypeOrderListInNextToRelation.processTypeOrderListInNextToRelation(session);
        }
    }

}
