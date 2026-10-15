package com.expensechain.backend.repository;

import com.expensechain.backend.model.PaymentReminder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class PaymentReminderRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PaymentReminder> reminderRowMapper = (rs, rowNum) -> new PaymentReminder(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getLong("sender_id"),
            rs.getLong("recipient_id"),
            rs.getDouble("amount"),
            rs.getString("status"),
            rs.getString("provider_message_id"),
            rs.getString("error_message"),
            rs.getString("created_at")
    );

    public PaymentReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PaymentReminder save(PaymentReminder reminder) {
        String sql = "INSERT INTO payment_reminders (group_id, sender_id, recipient_id, amount, status, provider_message_id, error_message, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, reminder.getGroupId());
            ps.setLong(2, reminder.getSenderId());
            ps.setLong(3, reminder.getRecipientId());
            ps.setDouble(4, reminder.getAmount());
            ps.setString(5, reminder.getStatus());
            ps.setString(6, reminder.getProviderMessageId());
            ps.setString(7, reminder.getErrorMessage());
            ps.setString(8, reminder.getCreatedAt());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            reminder.setId(key.longValue());
        }
        return reminder;
    }

    public List<PaymentReminder> findRecentReminders(Long groupId, Long senderId, Long recipientId) {
        String sql = "SELECT * FROM payment_reminders WHERE group_id = ? AND sender_id = ? AND recipient_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, reminderRowMapper, groupId, senderId, recipientId);
    }

    public List<PaymentReminder> findByGroupId(Long groupId) {
        String sql = "SELECT * FROM payment_reminders WHERE group_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, reminderRowMapper, groupId);
    }
}
