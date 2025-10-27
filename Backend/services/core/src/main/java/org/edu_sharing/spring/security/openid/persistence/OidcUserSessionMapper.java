package org.edu_sharing.spring.security.openid.persistence;

import org.apache.ibatis.annotations.*;


import java.util.List;

@Mapper
public interface OidcUserSessionMapper {

    @Insert("INSERT INTO oidc_session_registry (session_id, session_information) VALUES (#{sessionId}, #{sessionInformation, typeHandler=org.edu_sharing.spring.security.openid.persistence.JsonTypeHandler}) ON CONFLICT (session_id) DO UPDATE SET session_information = EXCLUDED.session_information")
    void save(@Param("sessionId") String sessionId,
              @Param("sessionInformation") OidcSessionInformationDto sessionInformation);

    @Select("SELECT session_id, session_information FROM oidc_session_registry WHERE session_id = #{sessionId}")
    @Results({
            @Result(column = "session_id", property = "sessionId", id = true),
            @Result(column = "session_information", property = "sessionInformation",
                    typeHandler = org.edu_sharing.spring.security.openid.persistence.JsonTypeHandler.class)
    })
    OidcUserSessionRecord findBySessionId(String sessionId);

    @Select("SELECT session_id, session_information FROM oidc_session_registry")
    @Results({
            @Result(column = "session_id", property = "sessionId", id = true),
            @Result(column = "session_information", property = "sessionInformation",
                    typeHandler = org.edu_sharing.spring.security.openid.persistence.JsonTypeHandler.class)
    })
    List<OidcUserSessionRecord> findAll();

    @Delete("DELETE FROM oidc_session_registry WHERE session_id = #{sessionId}")
    void deleteBySessionId(String sessionId);
}
