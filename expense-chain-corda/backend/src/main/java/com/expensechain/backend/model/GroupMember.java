package com.expensechain.backend.model;

import com.expensechain.backend.dto.UserDto;

public class GroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private String role;
    private String joinedAt;
    private UserDto user;

    public GroupMember() {}

    public GroupMember(Long id, Long groupId, Long userId, String role, String joinedAt) {
        this(id, groupId, userId, role, joinedAt, null);
    }

    public GroupMember(Long id, Long groupId, Long userId, String role, String joinedAt, UserDto user) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
