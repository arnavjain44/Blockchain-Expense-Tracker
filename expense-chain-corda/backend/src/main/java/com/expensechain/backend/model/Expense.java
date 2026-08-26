package com.expensechain.backend.model;

public class Expense {
    private Long id;
    private Long groupId;
    private String title;
    private double amount;
    private String category;
    private String description;
    private String expenseDate;
    private Long paidBy;
    private String splitType;
    private String cordaTxId;
    private String createdAt;

    public Expense() {}

    public Expense(Long id, Long groupId, String title, double amount, String category, String description, String expenseDate, Long paidBy, String splitType, String cordaTxId, String createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.expenseDate = expenseDate;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.cordaTxId = cordaTxId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }

    public Long getPaidBy() { return paidBy; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }

    public String getSplitType() { return splitType; }
    public void setSplitType(String splitType) { this.splitType = splitType; }

    public String getCordaTxId() { return cordaTxId; }
    public void setCordaTxId(String cordaTxId) { this.cordaTxId = cordaTxId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
