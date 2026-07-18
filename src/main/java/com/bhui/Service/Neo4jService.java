package com.bhui.Service;


import com.bhui.Bean.GroupMap;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.HashMap;
import java.util.List;

/**
 * @author JXS
 */
public interface Neo4jService {
     String getNameByOsmId(String[] list);

     List<Integer> getGroupIdByTags(String tags);

      HashMap<Integer,List<GroupMap>> getGroupIdMap(String caoTuLabel, String realLabel);

    Double getPartLocationSimByType(Integer groupId1,Integer groupId2,String type);
    Double getPartDistance(Integer groupId1,Integer groupId2);

     Double getPartOrderSimByNear(Integer groupId1,Integer groupId2);
    Double getPartOrderSimByNextTo(Integer groupId1,Integer groupId2);

    Double getPartLocationSimNoType(Integer groupId1,Integer groupId2);

    Integer[] getGroupIdsByTag(String tagName);


}
