package org.edu_sharing.service.tracking.ibatis;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserTrackingMapper {

    @Insert("INSERT INTO edu_tracking_user (authority,authority_organization,authority_mediacenter,time,type,data) VALUES (#{authority},#{authority_organization},#{authority_mediacenter},#{time},#{type},#{data})")
    void insertNode(UserTrackingEntry node);


    @Update("UPDATE edu_tracking_user SET authority = #{newAuthority} WHERE authority = #{oldAuthority}")
    void updateNodesWithAuthority(String oldAuthority, String newAuthority);

    @Delete("DELETE FROM edu_tracking_user WHERE authority = #{authority}")
    void deleteNodesByAuthority(String authority);
}
