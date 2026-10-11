package com.expensechain.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        log.info("Checking and applying backward-compatible database schema migrations...");
        try {
            // 1. Add username column if not exists
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255)");
            } catch (Exception e) {
                log.debug("Column username check/add note: {}", e.getMessage());
            }

            // 2. Add upi_id column if not exists
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS upi_id VARCHAR(255)");
            } catch (Exception e) {
                log.debug("Column upi_id check/add note: {}", e.getMessage());
            }

            // 3. Create user_sessions table if not exists
            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_sessions (" +
                        "id BIGSERIAL PRIMARY KEY, " +
                        "user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                        "token_hash VARCHAR(255) NOT NULL UNIQUE, " +
                        "expires_at VARCHAR(50) NOT NULL, " +
                        "created_at VARCHAR(50) NOT NULL" +
                        ")");
            } catch (Exception e) {
                log.debug("Table user_sessions check/add note: {}", e.getMessage());
            }

            // 4. Create Indexes
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_sessions_token ON user_sessions(token_hash)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_sessions_user ON user_sessions(user_id)");
            } catch (Exception e) {
                log.debug("Index creation note: {}", e.getMessage());
            }

            // 5. Backfill username for any existing users with null username
            try {
                List<Map<String, Object>> usersWithoutUsername = jdbcTemplate.queryForList(
                        "SELECT id, email, name FROM users WHERE username IS NULL OR TRIM(username) = ''"
                );
                for (Map<String, Object> u : usersWithoutUsername) {
                    Long id = ((Number) u.get("id")).longValue();
                    String email = (String) u.get("email");
                    String candidate = "user" + id;
                    if (email != null && email.contains("@")) {
                        String prefix = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.]", "").toLowerCase();
                        if (!prefix.isEmpty()) {
                            candidate = prefix;
                        }
                    }
                    // Ensure unique
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) AND id <> ?",
                            Integer.class, candidate, id
                    );
                    if (count != null && count > 0) {
                        candidate = candidate + "_" + id;
                    }
                    jdbcTemplate.update("UPDATE users SET username = ? WHERE id = ?", candidate, id);
                    log.info("Backfilled username '{}' for user ID {}", candidate, id);
                }
            } catch (Exception e) {
                log.warn("Username backfill note: {}", e.getMessage());
            }

            log.info("Database schema migrations completed successfully.");
        } catch (Exception e) {
            log.error("Migration execution error: ", e);
        }
    }
}
