package com.expensechain.backend.repository;

import com.expensechain.backend.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        String username = null;
        String upiId = null;
        try { username = rs.getString("username"); } catch (Exception ignored) {}
        try { upiId = rs.getString("upi_id"); } catch (Exception ignored) {}
        return new User(
                rs.getLong("id"),
                rs.getString("name"),
                username,
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("phone"),
                rs.getString("corda_x500_name"),
                upiId,
                rs.getString("created_at")
        );
    };

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        String sql = "INSERT INTO users (name, username, email, password_hash, phone, corda_x500_name, upi_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String normalizedUsername = user.getUsername() != null && !user.getUsername().trim().isEmpty()
                ? user.getUsername().trim().toLowerCase()
                : null;

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getName());
            ps.setString(2, normalizedUsername);
            ps.setString(3, user.getEmail().trim().toLowerCase());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getCordaX500Name());
            ps.setString(7, user.getUpiId() != null && !user.getUpiId().trim().isEmpty() ? user.getUpiId().trim() : null);
            ps.setString(8, user.getCreatedAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            user.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        } else if (keyHolder.getKey() != null) {
            user.setId(keyHolder.getKey().longValue());
        }
        return user;
    }

    public User findById(Long id) {
        String sql = "SELECT id, name, username, email, password_hash, phone, corda_x500_name, upi_id, created_at FROM users WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        String sql = "SELECT id, name, username, email, password_hash, phone, corda_x500_name, upi_id, created_at FROM users WHERE LOWER(email) = LOWER(?)";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, email.trim());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        String sql = "SELECT id, name, username, email, password_hash, phone, corda_x500_name, upi_id, created_at FROM users WHERE LOWER(username) = LOWER(?)";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, username.trim());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;
        String clean = identifier.trim().toLowerCase();
        String sql = "SELECT id, name, username, email, password_hash, phone, corda_x500_name, upi_id, created_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, clean, clean);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email.trim());
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username.trim());
        return count != null && count > 0;
    }

    public void updateUpiId(Long userId, String upiId) {
        if (userId == null) return;
        String cleanUpi = (upiId != null && !upiId.trim().isEmpty()) ? upiId.trim() : null;
        String sql = "UPDATE users SET upi_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, cleanUpi, userId);
    }

    public void updateUsername(Long userId, String username) {
        if (userId == null || username == null) return;
        String sql = "UPDATE users SET username = ? WHERE id = ?";
        jdbcTemplate.update(sql, username.trim().toLowerCase(), userId);
    }

    public List<User> findAll() {
        String sql = "SELECT id, name, username, email, password_hash, phone, corda_x500_name, upi_id, created_at FROM users ORDER BY id ASC";
        return jdbcTemplate.query(sql, userRowMapper);
    }
}
