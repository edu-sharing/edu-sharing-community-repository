package org.edu_sharing.spring.security.server.oauth2.persistence;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.edu_sharing.spring.security.server.oauth2.config.OAuth2ConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Slf4j
@Component("oAuth2SchemaInitializer")
@ConditionalOnProperty(name = OAuth2ConfigService.CONFIG_PATH + ".enabled", havingValue = "true")
public class OAuth2SchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public OAuth2SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() throws Exception {
        log.info("Initializing OAuth2 schema...");

        if (!tableExists("oauth2_authorization")) {
            executeSqlFromClasspath(
                    "org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql");
        }

        if (!tableExists("oauth2_authorization_consent")) {
            executeSqlFromClasspath(
                    "org/springframework/security/oauth2/server/authorization/oauth2-authorization-consent-schema.sql");
        }

        // not part of the spring schema: an authorization that waits for the consent of the user carries no
        // token yet and therefore no expires_at, this is the only way OAuth2AuthorizationCleanupJob can tell
        // its age. spring names its columns explicitly on insert and update, so the extra column stays
        // untouched by it. rows that exist before the column is added get the time of the migration.
        executeSql("ALTER TABLE oauth2_authorization ADD COLUMN IF NOT EXISTS created_at timestamptz DEFAULT CURRENT_TIMESTAMP");
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

            sql = sql.replace("blob", "text");

            executeSql(sql);
        }
    }

    private void executeSql(String sql) throws Exception {
        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        Connection con = dbAlf.getConnection();
        try {
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) {
                    try (PreparedStatement ps = con.prepareStatement(statement)) {
                        ps.executeUpdate();
                    }
                }
            }
            con.commit();
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            dbAlf.cleanUp(con);
        }
    }
}
