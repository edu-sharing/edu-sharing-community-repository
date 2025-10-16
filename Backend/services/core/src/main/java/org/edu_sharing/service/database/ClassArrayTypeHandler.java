package org.edu_sharing.service.database;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.jsoup.select.Evaluator;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

@Component
@MappedTypes(Class[].class)
public class ClassArrayTypeHandler extends BaseTypeHandler<Class<?>[]> {

    private final StringArrayTypeHandler stringArrayTypeHandler = new StringArrayTypeHandler();


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Class<?>[] parameter, JdbcType jdbcType) throws SQLException {
        stringArrayTypeHandler.setNonNullParameter(ps, i, Arrays.stream(parameter).map(Class::getName).toArray(String[]::new), jdbcType);
    }

    @Override
    public Class<?>[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String[] classNames = stringArrayTypeHandler.getNullableResult(rs, columnName);
        if(classNames == null){
            return null;
        }

        return Arrays.stream(classNames)
                .map(x->ClassUtils.resolveClassName(x, ClassUtils.getDefaultClassLoader()))
                .toArray(Class<?>[]::new);
    }

    @Override
        public Class<?>[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String[] classNames = stringArrayTypeHandler.getNullableResult(rs, columnIndex);
        if(classNames == null){
            return null;
        }

        return Arrays.stream(classNames)
                .map(x->ClassUtils.resolveClassName(x, ClassUtils.getDefaultClassLoader()))
                .toArray(Class<?>[]::new);
    }

    @Override
    public Class<?>[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String[] classNames = stringArrayTypeHandler.getNullableResult(cs, columnIndex);
        if(classNames == null){
            return null;
        }

        return Arrays.stream(classNames)
                .map(x->ClassUtils.resolveClassName(x, ClassUtils.getDefaultClassLoader()))
                .toArray(Class<?>[]::new);
    }
}
