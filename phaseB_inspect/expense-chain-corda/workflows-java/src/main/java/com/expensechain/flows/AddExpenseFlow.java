package com.expensechain.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.expensechain.contracts.ExpenseContract;
import com.expensechain.states.ExpenseState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Records one shared expense on the Corda ledger.
 *
 * Design choice for Phase B (kept deliberately simple — see DA2 prompt
 * section 4 "Development" constraints): the PAYER is the sole
 * transaction signer, since they are the one asserting "I paid this
 * amount". The other participants do not counter-sign, but they DO
 * receive and store the finalized state in their own Vaults via
 * FinalityFlow/ReceiveFinalityFlow — that's what makes this genuinely
 * multi-party rather than a single-node record (DA2 prompt section 10).
 *
 * Call from the payer's node shell, e.g. from Garvit's node:
 *
 *   flow start AddExpenseFlow expenseId: "42", groupId: "1", amountMinorUnits: 150000,
 *     currency: "INR", participants: [O=Garvit,L=New Delhi,C=IN, O=Arnav,L=Mumbai,C=IN, O=Mridul,L=Bengaluru,C=IN],
 *     splitType: "EQUAL", splitDetails: {"O=Garvit,L=New Delhi,C=IN": 50000, "O=Arnav,L=Mumbai,C=IN": 50000, "O=Mridul,L=Bengaluru,C=IN": 50000}
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
        // 1. Notary — required by every Corda transaction (see DA2 prompt section 11)
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // 2. The calling node is the payer
        Party payer = getOurIdentity();

        // 3. Construct the ExpenseState
        ExpenseState state = new ExpenseState(
                expenseId, groupId, payer, amountMinorUnits, currency,
                participants, splitType, splitDetails, Instant.now());

        // 4. Build the transaction: output state + Create command, payer as signer
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(state, ExpenseContract.ID)
                .addCommand(new ExpenseContract.Commands.Create(),
                        Collections.singletonList(payer.getOwningKey()));

        // 5. Verify against ExpenseContract BEFORE asking anyone to sign
        txBuilder.verify(getServiceHub());

        // 6. Payer signs (sole required signer in this design)
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // 7. Open sessions to every other participant so they receive the
        //    finalized state into their own Vault (not just ours)
        List<FlowSession> sessions = new ArrayList<>();
        for (Party p : participants) {
            if (!p.equals(payer)) {
                sessions.add(initiateFlow(p));
            }
        }

        // 8. Notarize + finalize + record in all participants' Vaults
        return subFlow(new FinalityFlow(signedTx, sessions));
    }
}
