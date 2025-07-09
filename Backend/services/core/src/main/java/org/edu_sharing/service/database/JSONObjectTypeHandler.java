package org.edu_sharing.service.database;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class JSONObjectTypeHandler extends BaseTypeHandler<JSONObject> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JSONObject parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("json");
        pgObject.setValue(parameter.toString());
        ps.setObject(i, pgObject);
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null) {
            return null;
        }
        return new JSONObject(json);
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null) {
            return null;
        }
        return new JSONObject(json);
    }

    @Override
    public JSONObject getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null) {
            return null;
        }
        return new JSONObject(json);
    }
}
