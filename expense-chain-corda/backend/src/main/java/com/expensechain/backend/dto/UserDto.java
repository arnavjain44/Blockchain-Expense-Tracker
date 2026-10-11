package com.expensechain.backend.dto;

import com.expensechain.backend.model.User;

public class UserDto {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String cordaX500Name;
    private String upiId;
    private String createdAt;

    public UserDto() {}

    public UserDto(Long id, String name, String username, String email, String phone, String cordaX500Name, String upiId, String createdAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.cordaX500Name = cordaX500Name;
        this.upiId = upiId;
        this.createdAt = createdAt;
    }

    public static UserDto fromUser(User u) {
        if (u == null) return null;
        return new UserDto(
                u.getId(),
                u.getName(),
                u.getUsername(),
                u.getEmail(),
                u.getPhone(),
                u.getCordaX500Name(),
                u.getUpiId(),
                u.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCordaX500Name() { return cordaX500Name; }
    public void setCordaX500Name(String cordaX500Name) { this.cordaX500Name = cordaX500Name; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
