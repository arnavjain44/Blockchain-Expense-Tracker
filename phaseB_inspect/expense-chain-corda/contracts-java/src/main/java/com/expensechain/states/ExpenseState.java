package com.expensechain.states;

import com.expensechain.contracts.ExpenseContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * On-ledger record of a single shared expense.
 *
 * Deliberately minimal: only the facts that need to be immutable and
 * multi-party-agreed live here. UI-only fields (category, notes, etc.)
 * stay in the application database — see DA2 prompt section 14.
 *
 * This mirrors what DA1's recordOnChain('EXPENSE', ...) call captured,
 * now as a real Corda state instead of a hash-chain entry.
 */
@BelongsToContract(ExpenseContract.class)
public class ExpenseState implements ContractState {

    private final String expenseId;      // links back to the app DB row
    private final String groupId;
    private final Party payer;           // who paid
    private final long amountMinorUnits; // e.g. paise, to avoid floating point
    private final String currency;       // e.g. "INR"
    private final List<Party> participants; // everyone the expense is split between
    private final String splitType;      // "EQUAL" | "CUSTOM"
    private final Map<String, Long> splitDetails; // X500 name -> owed amount (minor units)
    private final Instant recordedAt;

    public ExpenseState(String expenseId,
                         String groupId,
                         Party payer,
                         long amountMinorUnits,
                         String currency,
                         List<Party> participants,
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
    public List<Party> getParticipants() { return participants; }
    public String getSplitType() { return splitType; }
    public Map<String, Long> getSplitDetails() { return splitDetails; }
    public Instant getRecordedAt() { return recordedAt; }

    /**
     * Corda participants: every node whose Vault should hold a copy of
     * this state. This is what makes it a genuinely shared/multi-party
     * ledger fact rather than a single-node record — see DA2 prompt
     * section 10.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return new ArrayList<>(participants);
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
