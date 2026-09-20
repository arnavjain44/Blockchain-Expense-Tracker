package com.expensechain.backend.repository;

import com.expensechain.backend.model.Expense;
import com.expensechain.backend.model.ExpenseSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ExpenseRepository {

    private static final Logger log = LoggerFactory.getLogger(ExpenseRepository.class);
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Expense> expenseRowMapper = (rs, rowNum) -> new Expense(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getString("title"),
            rs.getDouble("amount"),
            rs.getString("category"),
            rs.getString("description"),
            rs.getString("expense_date"),
            rs.getLong("paid_by"),
            rs.getString("split_type"),
            rs.getString("corda_tx_id"),
            rs.getString("created_at")
    );

    private final RowMapper<ExpenseSplit> splitRowMapper = (rs, rowNum) -> new ExpenseSplit(
            rs.getLong("id"),
            rs.getLong("expense_id"),
            rs.getLong("user_id"),
            rs.getDouble("share_amount")
    );

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Expense save(Expense expense, List<ExpenseSplit> splits) {
        String sql = "INSERT INTO expenses (group_id, title, amount, category, description, expense_date, paid_by, split_type, corda_tx_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, expense.getGroupId());
            ps.setString(2, expense.getTitle());
            ps.setDouble(3, expense.getAmount());
            ps.setString(4, expense.getCategory());
            ps.setString(5, expense.getDescription());
            ps.setString(6, expense.getExpenseDate());
            ps.setLong(7, expense.getPaidBy());
            ps.setString(8, expense.getSplitType());
            ps.setString(9, expense.getCordaTxId());
            ps.setString(10, expense.getCreatedAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            expense.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        } else if (keyHolder.getKey() != null) {
            expense.setId(keyHolder.getKey().longValue());
        }

        if (splits != null && !splits.isEmpty()) {
            String splitSql = "INSERT INTO expense_splits (expense_id, user_id, share_amount) VALUES (?, ?, ?)";
            for (ExpenseSplit split : splits) {
                split.setExpenseId(expense.getId());
                KeyHolder splitKeyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(splitSql, new String[]{"id"});
                    ps.setLong(1, split.getExpenseId());
                    ps.setLong(2, split.getUserId());
                    ps.setDouble(3, split.getShareAmount());
                    return ps;
                }, splitKeyHolder);
                if (splitKeyHolder.getKeys() != null && splitKeyHolder.getKeys().containsKey("id")) {
                    split.setId(((Number) splitKeyHolder.getKeys().get("id")).longValue());
                } else if (splitKeyHolder.getKey() != null) {
                    split.setId(splitKeyHolder.getKey().longValue());
                }
            }
        }

        return expense;
    }

    public Expense findById(Long id) {
        String sql = "SELECT id, group_id, title, amount, category, description, expense_date, paid_by, split_type, corda_tx_id, created_at " +
                     "FROM expenses WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, expenseRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Expense> findByGroupId(Long groupId) {
        String sql = "SELECT id, group_id, title, amount, category, description, expense_date, paid_by, split_type, corda_tx_id, created_at " +
                     "FROM expenses WHERE group_id = ? ORDER BY id DESC";
        return jdbcTemplate.query(sql, expenseRowMapper, groupId);
    }

    public List<Expense> findByUserId(Long userId) {
        String sql = "SELECT DISTINCT e.id, e.group_id, e.title, e.amount, e.category, e.description, e.expense_date, e.paid_by, e.split_type, e.corda_tx_id, e.created_at " +
                     "FROM expenses e " +
                     "LEFT JOIN expense_splits es ON e.id = es.expense_id " +
                     "WHERE e.paid_by = ? OR es.user_id = ? " +
                     "ORDER BY e.id DESC";
        return jdbcTemplate.query(sql, expenseRowMapper, userId, userId);
    }

    public List<ExpenseSplit> getSplitsForExpense(Long expenseId) {
        String sql = "SELECT id, expense_id, user_id, share_amount FROM expense_splits WHERE expense_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, splitRowMapper, expenseId);
    }

    public List<Expense> findAll() {
        String sql = "SELECT id, group_id, title, amount, category, description, expense_date, paid_by, split_type, corda_tx_id, created_at FROM expenses ORDER BY id DESC";
        return jdbcTemplate.query(sql, expenseRowMapper);
    }
}
