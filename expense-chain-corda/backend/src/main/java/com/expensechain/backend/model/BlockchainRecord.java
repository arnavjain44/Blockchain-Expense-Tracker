package com.expensechain.backend.model;

import java.util.List;

public class BlockchainRecord {
    private String txId;
    private String stateType;
    private String referenceType; // EXPENSE or SETTLEMENT
    private String referenceId;
    private String notary;
    private List<String> signers;
    private List<String> participants;
    private String amountFormatted;
    private String timestamp;
    private String vaultStatus; // UNCONSUMED, CONSUMED
    private boolean verified;

    public BlockchainRecord() {}

    public BlockchainRecord(String txId, String stateType, String referenceType, String referenceId, String notary, List<String> signers, List<String> participants, String amountFormatted, String timestamp, String vaultStatus, boolean verified) {
        this.txId = txId;
        this.stateType = stateType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.notary = notary;
        this.signers = signers;
        this.participants = participants;
        this.amountFormatted = amountFormatted;
        this.timestamp = timestamp;
        this.vaultStatus = vaultStatus;
        this.verified = verified;
    }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }

    public String getStateType() { return stateType; }
    public void setStateType(String stateType) { this.stateType = stateType; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getNotary() { return notary; }
    public void setNotary(String notary) { this.notary = notary; }

    public List<String> getSigners() { return signers; }
    public void setSigners(List<String> signers) { this.signers = signers; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public String getAmountFormatted() { return amountFormatted; }
    public void setAmountFormatted(String amountFormatted) { this.amountFormatted = amountFormatted; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getVaultStatus() { return vaultStatus; }
    public void setVaultStatus(String vaultStatus) { this.vaultStatus = vaultStatus; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
