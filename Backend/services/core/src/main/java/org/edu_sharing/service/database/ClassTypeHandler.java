package org.edu_sharing.service.database;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@MappedTypes(Class.class)
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.NCHAR, JdbcType.NVARCHAR, JdbcType.LONGNVARCHAR, JdbcType.LONGVARCHAR})
public class ClassTypeHandler extends BaseTypeHandler<Class<?>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Class<?> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getName());
    }

    @Override
    public Class<?> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String className = rs.getString(columnName);
        return rs.wasNull() ? null : ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());
    }

    @Override
    public Class<?> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String className = rs.getString(columnIndex);

        return rs.wasNull() ? null : ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());
    }

    @Override
    public Class<?> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String className = cs.getString(columnIndex);
        return cs.wasNull() ? null : ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());
    }
}
