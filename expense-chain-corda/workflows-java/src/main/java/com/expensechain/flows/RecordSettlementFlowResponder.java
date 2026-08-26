package com.expensechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;

/**
 * Responder flow executed on the payee node to record the finalized SettlementState in their Vault.
 */
@InitiatedBy(RecordSettlementFlow.class)
public class RecordSettlementFlowResponder extends FlowLogic<SignedTransaction> {

    private final FlowSession counterpartySession;

    public RecordSettlementFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(counterpartySession, null, StatesToRecord.ALL_VISIBLE));
    }
}
