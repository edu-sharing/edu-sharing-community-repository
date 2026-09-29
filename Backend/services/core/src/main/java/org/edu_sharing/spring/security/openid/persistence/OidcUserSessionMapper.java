package org.edu_sharing.spring.security.openid.persistence;

import org.apache.ibatis.annotations.*;


import java.util.Collection;
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

    /**
     * the idp session id, used to map an incoming back channel logout token to the client sessions
     */
    @Select("SELECT session_id, session_information FROM oidc_session_registry WHERE session_information->'claims'->>'sid' = #{sid}")
    @Results({
            @Result(column = "session_id", property = "sessionId", id = true),
            @Result(column = "session_information", property = "sessionInformation",
                    typeHandler = org.edu_sharing.spring.security.openid.persistence.JsonTypeHandler.class)
    })
    List<OidcUserSessionRecord> findBySid(@Param("sid") String sid);

    /**
     * fallback for logout tokens without a sid, all sessions of the subject are affected then
     */
    @Select("SELECT session_id, session_information FROM oidc_session_registry WHERE session_information->>'subject' = #{subject}")
    @Results({
            @Result(column = "session_id", property = "sessionId", id = true),
            @Result(column = "session_information", property = "sessionInformation",
                    typeHandler = org.edu_sharing.spring.security.openid.persistence.JsonTypeHandler.class)
    })
    List<OidcUserSessionRecord> findBySubject(@Param("subject") String subject);

    /**
     * candidates for the cleanup job: entries that were created longer than the given interval ago.
     * paginated by session id so that the amount of data held in memory stays constant regardless of the table size
     */
    @Select("SELECT session_id FROM oidc_session_registry "
            + "WHERE session_id > #{afterSessionId} AND created_at < now() - CAST(#{maxAge} AS interval) "
            + "ORDER BY session_id LIMIT #{limit}")
    List<String> findOutdatedSessionIds(@Param("maxAge") String maxAge,
                                        @Param("afterSessionId") String afterSessionId,
                                        @Param("limit") int limit);

    @Delete("DELETE FROM oidc_session_registry WHERE session_id = #{sessionId}")
    void deleteBySessionId(String sessionId);

    @Delete({"<script>",
            "DELETE FROM oidc_session_registry WHERE session_id IN",
            "<foreach item='sessionId' collection='sessionIds' open='(' separator=',' close=')'>#{sessionId}</foreach>",
            "</script>"})
    int deleteBySessionIds(@Param("sessionIds") Collection<String> sessionIds);
}
