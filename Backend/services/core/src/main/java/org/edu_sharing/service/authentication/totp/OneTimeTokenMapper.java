package org.edu_sharing.service.authentication.totp;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OneTimeTokenMapper {

    @Select("SELECT * FROM edu_one_time_tokens WHERE username = #{username}")
    OneTimeToken findByUserName(String username);

    @Insert("INSERT INTO edu_one_time_tokens (username, secret) VALUES (#{username}, #{secret}) ON CONFLICT (username) DO UPDATE SET secret = #{secret}")
    void save(OneTimeToken oneTimeToken);

    @Delete("DELETE FROM edu_one_time_tokens WHERE username = #{username}")
    void deleteByUserName(String userName);
}
