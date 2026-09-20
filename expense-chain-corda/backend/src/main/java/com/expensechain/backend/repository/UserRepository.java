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

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("phone"),
            rs.getString("corda_x500_name"),
            rs.getString("created_at")
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, phone, corda_x500_name, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getCordaX500Name());
            ps.setString(6, user.getCreatedAt());
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
        String sql = "SELECT id, name, email, password_hash, phone, corda_x500_name, created_at FROM users WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        String sql = "SELECT id, name, email, password_hash, phone, corda_x500_name, created_at FROM users WHERE LOWER(email) = LOWER(?)";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, email.trim());
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

    public List<User> findAll() {
        String sql = "SELECT id, name, email, password_hash, phone, corda_x500_name, created_at FROM users ORDER BY id ASC";
        return jdbcTemplate.query(sql, userRowMapper);
    }
}
