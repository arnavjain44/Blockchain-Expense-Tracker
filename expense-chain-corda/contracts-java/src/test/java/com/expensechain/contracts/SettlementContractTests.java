package com.expensechain.contracts;

import com.expensechain.states.SettlementState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class SettlementContractTests {

    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.expensechain.contracts", "com.expensechain.states"));
    private final TestIdentity debtor = new TestIdentity(new CordaX500Name("Arnav", "Mumbai", "IN"));
    private final TestIdentity creditor = new TestIdentity(new CordaX500Name("Garvit", "New Delhi", "IN"));

    @Test
    public void validSettlementMustPass() {
        SettlementState state = new SettlementState(
                "stl-1", "grp-1", debtor.getParty(), creditor.getParty(), 50000L, "INR", Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(SettlementContract.ID, state);
                tx.command(Collections.singletonList(debtor.getPublicKey()), new SettlementContract.Commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void settlementZeroAmountMustFail() {
        SettlementState state = new SettlementState(
                "stl-2", "grp-1", debtor.getParty(), creditor.getParty(), 0L, "INR", Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(SettlementContract.ID, state);
                tx.command(Collections.singletonList(debtor.getPublicKey()), new SettlementContract.Commands.Create());
                return tx.failsWith("Settlement amount must be positive.");
            });
            return null;
        });
    }

    @Test
    public void samePayerAndPayeeMustFail() {
        SettlementState state = new SettlementState(
                "stl-3", "grp-1", debtor.getParty(), debtor.getParty(), 50000L, "INR", Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(SettlementContract.ID, state);
                tx.command(Collections.singletonList(debtor.getPublicKey()), new SettlementContract.Commands.Create());
                return tx.failsWith("Payer and payee must be different parties.");
            });
            return null;
        });
    }
}
