package com.expensechain.backend.repository;

import com.expensechain.backend.model.Group;
import com.expensechain.backend.model.GroupMember;
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
public class GroupRepository {

    private static final Logger log = LoggerFactory.getLogger(GroupRepository.class);
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Group> groupRowMapper = (rs, rowNum) -> new Group(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getLong("created_by"),
            rs.getString("created_at")
    );

    private final RowMapper<GroupMember> memberRowMapper = (rs, rowNum) -> new GroupMember(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getLong("user_id"),
            rs.getString("role"),
            rs.getString("joined_at")
    );

    public GroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Group save(Group group) {
        String sql = "INSERT INTO groups (name, description, created_by, created_at) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, group.getName());
            ps.setString(2, group.getDescription());
            ps.setLong(3, group.getCreatedBy());
            ps.setString(4, group.getCreatedAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            group.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        } else if (keyHolder.getKey() != null) {
            group.setId(keyHolder.getKey().longValue());
        }
        return group;
    }

    public Group findById(Long id) {
        String sql = "SELECT id, name, description, created_by, created_at FROM groups WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, groupRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Group> findAll() {
        String sql = "SELECT id, name, description, created_by, created_at FROM groups ORDER BY id ASC";
        return jdbcTemplate.query(sql, groupRowMapper);
    }

    public List<Group> findGroupsForUser(Long userId) {
        String sql = "SELECT g.id, g.name, g.description, g.created_by, g.created_at " +
                     "FROM groups g " +
                     "INNER JOIN group_members gm ON g.id = gm.group_id " +
                     "WHERE gm.user_id = ? " +
                     "ORDER BY g.id ASC";
        return jdbcTemplate.query(sql, groupRowMapper, userId);
    }

    public GroupMember addMember(GroupMember member) {
        String checkSql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, member.getGroupId(), member.getUserId());
        if (count != null && count > 0) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        String sql = "INSERT INTO group_members (group_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, member.getGroupId());
            ps.setLong(2, member.getUserId());
            ps.setString(3, member.getRole());
            ps.setString(4, member.getJoinedAt());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            member.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        } else if (keyHolder.getKey() != null) {
            member.setId(keyHolder.getKey().longValue());
        }
        return member;
    }

    public List<GroupMember> getMembers(Long groupId) {
        String sql = "SELECT id, group_id, user_id, role, joined_at FROM group_members WHERE group_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, memberRowMapper, groupId);
    }

    public boolean isMember(Long groupId, Long userId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, groupId, userId);
        return count != null && count > 0;
    }
}
