package com.expensechain.backend.model;

public class ExpenseSplit {
    private Long id;
    private Long expenseId;
    private Long userId;
    private double shareAmount;

    public ExpenseSplit() {}

    public ExpenseSplit(Long id, Long expenseId, Long userId, double shareAmount) {
        this.id = id;
        this.expenseId = expenseId;
        this.userId = userId;
        this.shareAmount = shareAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getShareAmount() { return shareAmount; }
    public void setShareAmount(double shareAmount) { this.shareAmount = shareAmount; }
}
