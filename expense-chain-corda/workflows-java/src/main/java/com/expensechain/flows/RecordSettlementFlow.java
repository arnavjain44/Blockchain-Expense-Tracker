package com.expensechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.expensechain.contracts.SettlementContract;
import com.expensechain.states.SettlementState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Initiates the flow to record a debt settlement on-ledger.
 * The payer (debtor) signs and notarizes the transaction, and
 * the payee (creditor) receives and stores the state in their Vault.
 */
@InitiatingFlow
@StartableByRPC
public class RecordSettlementFlow extends FlowLogic<SignedTransaction> {

    private final String settlementId;
    private final String groupId;
    private final Party payee;
    private final long amountMinorUnits;
    private final String currency;

    public RecordSettlementFlow(String settlementId,
                                String groupId,
                                Party payee,
                                long amountMinorUnits,
                                String currency) {
        this.settlementId = settlementId;
        this.groupId = groupId;
        this.payee = payee;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // 1. Obtain Notary from NetworkMapCache
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // 2. Caller node is the payer
        Party payer = getOurIdentity();

        // 3. Create the SettlementState
        SettlementState state = new SettlementState(
                settlementId, groupId, payer, payee, amountMinorUnits, currency, Instant.now());

        // 4. Build transaction: output state + Create command with payer key
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(state, SettlementContract.ID)
                .addCommand(new SettlementContract.Commands.Create(),
                        Collections.singletonList(payer.getOwningKey()));

        // 5. Verify contract rules
        txBuilder.verify(getServiceHub());

        // 6. Payer signs transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // 7. Initiate session with payee if different from payer
        List<FlowSession> sessions = new ArrayList<>();
        if (!payee.equals(payer)) {
            sessions.add(initiateFlow(payee));
        }

        // 8. Finalize with Notary and push to payee's vault
        return subFlow(new FinalityFlow(signedTx, sessions));
    }
}
