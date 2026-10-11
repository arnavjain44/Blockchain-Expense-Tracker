package com.expensechain.backend.model;

public class User {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String passwordHash;
    private String phone;
    private String cordaX500Name;
    private String upiId;
    private String createdAt;

    public User() {}

    public User(Long id, String name, String email, String passwordHash, String phone, String cordaX500Name, String createdAt) {
        this(id, name, null, email, passwordHash, phone, cordaX500Name, null, createdAt);
    }

    public User(Long id, String name, String username, String email, String passwordHash, String phone, String cordaX500Name, String upiId, String createdAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.cordaX500Name = cordaX500Name;
        this.upiId = upiId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCordaX500Name() { return cordaX500Name; }
    public void setCordaX500Name(String cordaX500Name) { this.cordaX500Name = cordaX500Name; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
