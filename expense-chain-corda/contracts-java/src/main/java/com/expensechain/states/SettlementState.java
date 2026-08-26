package com.expensechain.states;

import com.expensechain.contracts.SettlementContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * On-ledger record of a debt settlement between two parties.
 */
@CordaSerializable
@BelongsToContract(SettlementContract.class)
public class SettlementState implements ContractState {

    private final String settlementId;
    private final String groupId;
    private final Party payer;
    private final Party payee;
    private final long amountMinorUnits;
    private final String currency;
    private final Instant settledAt;

    @ConstructorForDeserialization
    public SettlementState(String settlementId,
                           String groupId,
                           Party payer,
                           Party payee,
                           long amountMinorUnits,
                           String currency,
                           Instant settledAt) {
        this.settlementId = settlementId;
        this.groupId = groupId;
        this.payer = payer;
        this.payee = payee;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.settledAt = settledAt;
    }

    public String getSettlementId() { return settlementId; }
    public String getGroupId() { return groupId; }
    public Party getPayer() { return payer; }
    public Party getPayee() { return payee; }
    public long getAmountMinorUnits() { return amountMinorUnits; }
    public String getCurrency() { return currency; }
    public Instant getSettledAt() { return settledAt; }

    @Override
    public List<AbstractParty> getParticipants() {
        return new ArrayList<>(Arrays.asList(payer, payee));
    }

    @Override
    public String toString() {
        return "SettlementState{" +
                "settlementId='" + settlementId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", payer=" + payer.getName() +
                ", payee=" + payee.getName() +
                ", amount=" + amountMinorUnits + " " + currency +
                ", settledAt=" + settledAt +
                '}';
    }
}
