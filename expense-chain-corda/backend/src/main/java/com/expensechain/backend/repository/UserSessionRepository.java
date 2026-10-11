package com.expensechain.backend.repository;

import com.expensechain.backend.model.UserSession;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class UserSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UserSession> sessionRowMapper = (rs, rowNum) -> new UserSession(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token_hash"),
            rs.getString("expires_at"),
            rs.getString("created_at")
    );

    public UserSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserSession save(UserSession session) {
        String sql = "INSERT INTO user_sessions (user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, session.getUserId());
            ps.setString(2, session.getTokenHash());
            ps.setString(3, session.getExpiresAt());
            ps.setString(4, session.getCreatedAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            session.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        } else if (keyHolder.getKey() != null) {
            session.setId(keyHolder.getKey().longValue());
        }
        return session;
    }

    public UserSession findByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.trim().isEmpty()) return null;
        String sql = "SELECT id, user_id, token_hash, expires_at, created_at FROM user_sessions WHERE token_hash = ?";
        try {
            return jdbcTemplate.queryForObject(sql, sessionRowMapper, tokenHash.trim());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void deleteByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.trim().isEmpty()) return;
        String sql = "DELETE FROM user_sessions WHERE token_hash = ?";
        jdbcTemplate.update(sql, tokenHash.trim());
    }

    public void deleteByUserId(Long userId) {
        if (userId == null) return;
        String sql = "DELETE FROM user_sessions WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public void deleteExpired(String nowIsoString) {
        if (nowIsoString == null) return;
        String sql = "DELETE FROM user_sessions WHERE expires_at < ?";
        jdbcTemplate.update(sql, nowIsoString);
    }
}
