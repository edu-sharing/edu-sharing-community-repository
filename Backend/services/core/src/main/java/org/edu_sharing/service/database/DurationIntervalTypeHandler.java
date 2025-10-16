package org.edu_sharing.service.database;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

@Component
@MappedTypes(java.time.Duration.class)
@MappedJdbcTypes(JdbcType.OTHER) // Postgres INTERVAL
public class DurationIntervalTypeHandler extends BaseTypeHandler<Duration> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Duration parameter, JdbcType jdbcType) throws SQLException {
    // submits as String, Postgres is parsing 'interval'
    ps.setObject(i, java.time.Duration.from(parameter).toString(), java.sql.Types.OTHER); // "PT15M"
  }

  @Override
  public Duration getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Object obj = rs.getObject(columnName);
    return parseInterval(obj);
  }

  @Override
  public Duration getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Object obj = rs.getObject(columnIndex);
    return parseInterval(obj);
  }

  @Override
  public Duration getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    Object obj = cs.getObject(columnIndex);
    return parseInterval(obj);
  }

  private Duration parseInterval(Object obj) throws SQLException {
    if (obj == null) return null;
    // Postgres provides org.postgresql.util.PGInterval or String (depends on driver)
    if (obj instanceof org.postgresql.util.PGInterval pg) {
      long seconds =
          pg.getYears() * 31557600L +
          pg.getMonths() * 2629800L +
          pg.getDays() * 86400L +
          pg.getHours() * 3600L +
          pg.getMinutes() * 60L +
          (long) pg.getSeconds();
      long nanos = (long) ((pg.getSeconds() - Math.floor(pg.getSeconds())) * 1_000_000_000L);
      return Duration.ofSeconds(seconds, nanos);
    }
    if (obj instanceof String s) {
      // Fallback: String like "00:15:00" oder "1 day 02:03:04"
      return parseIntervalString(s);
    }
    throw new SQLException("Unsupported interval type: " + obj.getClass());
  }

    private Duration parseIntervalString(String s) throws SQLException {
        String str = s.trim().toLowerCase();
        try {
            // Cases of: "1 day 02:03:04", "02:03:04", "15:00", "3 days"
            long days = 0;
            if (str.contains("day")) {
                int idx = str.indexOf("day");
                String num = str.substring(0, idx).trim();
                days = Long.parseLong(num.split("\\s+")[0]);
                str = str.substring(idx + 3).trim();
            }
            long h = 0, m = 0;
            double sec = 0;
            if (!str.isEmpty()) {
                String[] parts = str.split(":");
                if (parts.length == 3) {
                    h = Long.parseLong(parts[0].trim());
                    m = Long.parseLong(parts[1].trim());
                    sec = Double.parseDouble(parts[2].trim());
                } else if (parts.length == 2) {
                    h = Long.parseLong(parts[0].trim());
                    m = Long.parseLong(parts[1].trim());
                } else if (str.matches("^\\d+(\\.\\d+)?\\s*seconds?$")) {
                    sec = Double.parseDouble(str.replaceAll("[^0-9.]", ""));
                }
            }
            long totalSeconds = days * 86400 + h * 3600 + m * 60 + (long) Math.floor(sec);
            long nanos = Math.round((sec - Math.floor(sec)) * 1_000_000_000L);
            return Duration.ofSeconds(totalSeconds, nanos);
        } catch (Exception e) {
            throw new SQLException("Cannot parse interval string: " + s, e);
        }
    }
}
