package org.edu_sharing.service.share.ibatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.edu_sharing.service.share.ShareType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@MappedTypes(ShareType.class)
@MappedJdbcTypes(JdbcType.SMALLINT)
public class ShareTypeHandler extends BaseTypeHandler<ShareType> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ShareType parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getType());
    }

    @Override
    public ShareType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : ShareType.getType(value);
    }

    @Override
    public ShareType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : ShareType.getType(value);
    }

    @Override
    public ShareType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : ShareType.getType(value);
    }
}
