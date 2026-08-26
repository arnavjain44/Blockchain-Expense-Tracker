package com.expensechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.expensechain.contracts.ExpenseContract;
import com.expensechain.states.ExpenseState;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Records one shared expense on the Corda ledger.
 */
@InitiatingFlow
@StartableByRPC
public class AddExpenseFlow extends FlowLogic<SignedTransaction> {

    private final String expenseId;
    private final String groupId;
    private final long amountMinorUnits;
    private final String currency;
    private final List<Party> participants;
    private final String splitType;
    private final Map<String, Long> splitDetails;

    public AddExpenseFlow(String expenseId,
                           String groupId,
                           long amountMinorUnits,
                           String currency,
                           List<Party> participants,
                           String splitType,
                           Map<String, Long> splitDetails) {
        this.expenseId = expenseId;
        this.groupId = groupId;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.participants = participants;
        this.splitType = splitType;
        this.splitDetails = splitDetails;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // 1. Notary from NetworkMapCache
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // 2. Caller node is the payer
        Party payer = getOurIdentity();

        // 3. Construct ExpenseState
        List<AbstractParty> abstractParticipants = new ArrayList<>(participants);
        ExpenseState state = new ExpenseState(
                expenseId, groupId, payer, amountMinorUnits, currency,
                abstractParticipants, splitType, splitDetails, Instant.now());

        // 4. Build transaction: output state + Create command with payer key
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(state, ExpenseContract.ID)
                .addCommand(new ExpenseContract.Commands.Create(),
                        Collections.singletonList(payer.getOwningKey()));

        // 5. Verify contract rules
        txBuilder.verify(getServiceHub());

        // 6. Payer signs transaction
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // 7. Open sessions with all other participants (deduplicated by Party to avoid duplicate session deadlocks)
        Set<Party> otherParties = new HashSet<>();
        for (Party p : participants) {
            if (!p.equals(payer)) {
                otherParties.add(p);
            }
        }
        List<FlowSession> sessions = new ArrayList<>();
        for (Party p : otherParties) {
            sessions.add(initiateFlow(p));
        }

        // 8. Finalize with Notary and push state to all participants' vaults
        return subFlow(new FinalityFlow(signedTx, sessions));
    }
}
