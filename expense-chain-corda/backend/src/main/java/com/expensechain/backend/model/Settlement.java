package com.expensechain.backend.model;

public class Settlement {
    private Long id;
    private Long groupId;
    private Long paidBy;
    private Long paidTo;
    private double amount;
    private String status;
    private String cordaTxId;
    private String createdAt;
    private String settledAt;

    public Settlement() {}

    public Settlement(Long id, Long groupId, Long paidBy, Long paidTo, double amount, String status, String cordaTxId, String createdAt, String settledAt) {
        this.id = id;
        this.groupId = groupId;
        this.paidBy = paidBy;
        this.paidTo = paidTo;
        this.amount = amount;
        this.status = status;
        this.cordaTxId = cordaTxId;
        this.createdAt = createdAt;
        this.settledAt = settledAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getPaidBy() { return paidBy; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }

    public Long getPaidTo() { return paidTo; }
    public void setPaidTo(Long paidTo) { this.paidTo = paidTo; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCordaTxId() { return cordaTxId; }
    public void setCordaTxId(String cordaTxId) { this.cordaTxId = cordaTxId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSettledAt() { return settledAt; }
    public void setSettledAt(String settledAt) { this.settledAt = settledAt; }
}
