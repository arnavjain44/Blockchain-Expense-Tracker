package com.expensechain.backend.repository;

import com.expensechain.backend.model.Settlement;
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
public class SettlementRepository {

    private static final Logger log = LoggerFactory.getLogger(SettlementRepository.class);
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Settlement> settlementRowMapper = (rs, rowNum) -> new Settlement(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getLong("paid_by"),
            rs.getLong("paid_to"),
            rs.getDouble("amount"),
            rs.getString("status"),
            rs.getString("corda_tx_id"),
            rs.getString("created_at"),
            rs.getString("settled_at")
    );

    public SettlementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Settlement save(Settlement settlement) {
        String sql = "INSERT INTO settlements (group_id, paid_by, paid_to, amount, status, corda_tx_id, created_at, settled_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, settlement.getGroupId());
            ps.setLong(2, settlement.getPaidBy());
            ps.setLong(3, settlement.getPaidTo());
            ps.setDouble(4, settlement.getAmount());
            ps.setString(5, settlement.getStatus());
            ps.setString(6, settlement.getCordaTxId());
            ps.setString(7, settlement.getCreatedAt());
            ps.setString(8, settlement.getSettledAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            settlement.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        } else if (keyHolder.getKey() != null) {
            settlement.setId(keyHolder.getKey().longValue());
        }
        return settlement;
    }

    public Settlement findById(Long id) {
        String sql = "SELECT id, group_id, paid_by, paid_to, amount, status, corda_tx_id, created_at, settled_at FROM settlements WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, settlementRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Settlement> findByGroupId(Long groupId) {
        String sql = "SELECT id, group_id, paid_by, paid_to, amount, status, corda_tx_id, created_at, settled_at FROM settlements WHERE group_id = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, settlementRowMapper, groupId);
    }

    public List<Settlement> findPendingForUser(Long userId) {
        String sql = "SELECT id, group_id, paid_by, paid_to, amount, status, corda_tx_id, created_at, settled_at " +
                     "FROM settlements WHERE (paid_to = ? AND status = 'PENDING_VERIFICATION') " +
                     "OR (paid_by = ? AND status = 'REJECTED') ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, settlementRowMapper, userId, userId);
    }

    public boolean hasPending(Long groupId, Long paidBy, Long paidTo) {
        String sql = "SELECT COUNT(*) FROM settlements WHERE group_id = ? AND paid_by = ? AND paid_to = ? AND status = 'PENDING_VERIFICATION'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, groupId, paidBy, paidTo);
        return count != null && count > 0;
    }

    public double getPendingAmount(Long groupId, Long paidBy, Long paidTo) {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM settlements WHERE group_id = ? AND paid_by = ? AND paid_to = ? AND status = 'PENDING_VERIFICATION'";
        Double amt = jdbcTemplate.queryForObject(sql, Double.class, groupId, paidBy, paidTo);
        return amt != null ? amt : 0.0;
    }

    public void updateStatus(Long id, String status, String cordaTxId, String settledAt) {
        if (cordaTxId != null && settledAt != null) {
            String sql = "UPDATE settlements SET status = ?, corda_tx_id = ?, settled_at = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, cordaTxId, settledAt, id);
        } else if (cordaTxId != null) {
            String sql = "UPDATE settlements SET status = ?, corda_tx_id = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, cordaTxId, id);
        } else if (settledAt != null) {
            String sql = "UPDATE settlements SET status = ?, settled_at = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, settledAt, id);
        } else {
            String sql = "UPDATE settlements SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        }
    }

    public List<Settlement> findAll() {
        String sql = "SELECT id, group_id, paid_by, paid_to, amount, status, corda_tx_id, created_at, settled_at FROM settlements ORDER BY id DESC";
        return jdbcTemplate.query(sql, settlementRowMapper);
    }
}
