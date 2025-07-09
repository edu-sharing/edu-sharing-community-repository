package org.edu_sharing.service.database;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array sqlArray = conn.createArrayOf("VARCHAR", parameter);
        ps.setArray(i, sqlArray);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array sqlArray = rs.getArray(columnName);
        if (sqlArray == null) {
            return null;
        }
        return (String[]) sqlArray.getArray();
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array sqlArray = rs.getArray(columnIndex);
        if (sqlArray == null) {
            return null;
        }
        return (String[]) sqlArray.getArray();
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array sqlArray = cs.getArray(columnIndex);
        if (sqlArray == null) {
            return null;
        }
        return (String[]) sqlArray.getArray();
    }
}
