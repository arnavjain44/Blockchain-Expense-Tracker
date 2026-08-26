package com.expensechain.states;

import com.expensechain.contracts.ExpenseContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * On-ledger record of a single shared expense.
 */
@CordaSerializable
@BelongsToContract(ExpenseContract.class)
public class ExpenseState implements ContractState {

    private final String expenseId;
    private final String groupId;
    private final Party payer;
    private final long amountMinorUnits;
    private final String currency;
    private final List<AbstractParty> participants;
    private final String splitType;
    private final Map<String, Long> splitDetails;
    private final Instant recordedAt;

    @ConstructorForDeserialization
    public ExpenseState(String expenseId,
                         String groupId,
                         Party payer,
                         long amountMinorUnits,
                         String currency,
                         List<AbstractParty> participants,
                         String splitType,
                         Map<String, Long> splitDetails,
                         Instant recordedAt) {
        this.expenseId = expenseId;
        this.groupId = groupId;
        this.payer = payer;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.participants = Collections.unmodifiableList(new ArrayList<>(participants));
        this.splitType = splitType;
        this.splitDetails = splitDetails;
        this.recordedAt = recordedAt;
    }

    public String getExpenseId() { return expenseId; }
    public String getGroupId() { return groupId; }
    public Party getPayer() { return payer; }
    public long getAmountMinorUnits() { return amountMinorUnits; }
    public String getCurrency() { return currency; }
    public String getSplitType() { return splitType; }
    public Map<String, Long> getSplitDetails() { return splitDetails; }
    public Instant getRecordedAt() { return recordedAt; }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }

    @Override
    public String toString() {
        return "ExpenseState{" +
                "expenseId='" + expenseId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", payer=" + payer.getName() +
                ", amount=" + amountMinorUnits + " " + currency +
                ", participants=" + participants.size() +
                ", splitType='" + splitType + '\'' +
                ", recordedAt=" + recordedAt +
                '}';
    }
}
