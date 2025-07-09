package org.edu_sharing.service.database;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@MapperScan(value = "org.edu_sharing", annotationClass = Mapper.class)
public class PersistenceConfig {

    public PersistenceConfig(SqlSessionFactory sqlSessionFactory, List<TypeHandler<?>> typeHandlers) {
        typeHandlers.forEach(sqlSessionFactory.getConfiguration().getTypeHandlerRegistry()::register);
    }
}
