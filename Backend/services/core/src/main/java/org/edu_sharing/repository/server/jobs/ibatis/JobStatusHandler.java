package org.edu_sharing.repository.server.jobs.ibatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.edu_sharing.repository.server.jobs.JobStatus;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@MappedTypes(JobStatus.class)
@MappedJdbcTypes(JdbcType.SMALLINT)
public class JobStatusHandler extends BaseTypeHandler<JobStatus> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JobStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getStatus());
    }

    @Override
    public JobStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : JobStatus.getStatus(value);
    }

    @Override
    public JobStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : JobStatus.getStatus(value);
    }

    @Override
    public JobStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : JobStatus.getStatus(value);
    }
}
