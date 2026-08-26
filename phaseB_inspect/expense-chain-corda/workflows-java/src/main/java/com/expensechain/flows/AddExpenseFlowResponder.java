package com.expensechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.node.StatesToRecord;

/**
 * Runs automatically on every non-payer participant's node when
 * AddExpenseFlow finalizes a transaction that names them as a
 * participant. Records the ExpenseState into their Vault.
 */
@InitiatedBy(AddExpenseFlow.class)
public class AddExpenseFlowResponder extends FlowLogic<SignedTransaction> {

    private final FlowSession counterpartySession;

    public AddExpenseFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(counterpartySession, null, StatesToRecord.ALL_VISIBLE));
    }
}
