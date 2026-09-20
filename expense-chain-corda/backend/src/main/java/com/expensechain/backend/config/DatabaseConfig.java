package com.expensechain.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${DATABASE_USERNAME:}")
    private String databaseUsername;

    @Value("${DATABASE_PASSWORD:}")
    private String databasePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String jdbcUrl;
        String username = databaseUsername;
        String password = databasePassword;

        String rawUrl = (databaseUrl != null && !databaseUrl.trim().isEmpty())
                ? databaseUrl.trim()
                : "jdbc:postgresql://localhost:5432/expensechain";

        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(rawUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    try {
                        username = java.net.URLDecoder.decode(parts[0], "UTF-8");
                        password = java.net.URLDecoder.decode(parts[1], "UTF-8");
                    } catch (Exception ignored) {
                        username = parts[0];
                        password = parts[1];
                    }
                }
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();
                jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path + (query != null ? "?" + query : "");
            } catch (Exception e) {
                log.warn("Failed to parse DATABASE_URL as URI, using as-is: {}", e.getMessage());
                jdbcUrl = rawUrl;
            }
        } else if (!rawUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:postgresql://" + rawUrl;
        } else {
            jdbcUrl = rawUrl;
        }

        config.setJdbcUrl(jdbcUrl);
        if (username != null && !username.trim().isEmpty()) {
            config.setUsername(username.trim());
        } else if (config.getUsername() == null) {
            config.setUsername("postgres");
        }

        if (password != null) {
            config.setPassword(password.trim());
        }

        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(600000);

        log.info("Configured PostgreSQL DataSource with JDBC URL: {}", jdbcUrl.replaceAll(":[^/@:]+@", ":***@"));
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
