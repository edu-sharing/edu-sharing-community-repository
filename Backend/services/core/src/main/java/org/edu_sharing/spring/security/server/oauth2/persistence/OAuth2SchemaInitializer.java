package org.edu_sharing.spring.security.server.oauth2.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public OAuth2SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() throws Exception {

        if (!tableExists("oauth2_authorization")) {
            executeSqlFromClasspath(
                    "org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql");
        }

        if (!tableExists("oauth2_authorization_consent")) {
            executeSqlFromClasspath(
                    "org/springframework/security/oauth2/server/authorization/oauth2-authorization-consent-schema.sql");
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.query(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                ps -> ps.setString(1, tableName.toLowerCase()),
                rs -> rs.next() ? rs.getInt(1) : 0
        );
        return count != null && count > 0;
    }

    private void executeSqlFromClasspath(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Schema file not found: " + path);
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            sql = sql.replace("attributes blob","attributes text");
            sql = sql.replace("_metadata blob","_metadata varchar(2000)");

            //sql = sql.replace("blob", "bytea");
            sql = sql.replace("blob", "text");
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) {
                    jdbcTemplate.execute(statement.trim());
                }
            }
        }
    }
}
