package com.expensechain.contracts;

import com.expensechain.states.ExpenseState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ExpenseContractTests {

    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.expensechain.contracts", "com.expensechain.states"));
    private final TestIdentity garvit = new TestIdentity(new CordaX500Name("Garvit", "New Delhi", "IN"));
    private final TestIdentity arnav = new TestIdentity(new CordaX500Name("Arnav", "Mumbai", "IN"));
    private final TestIdentity mridul = new TestIdentity(new CordaX500Name("Mridul", "Bengaluru", "IN"));

    @Test
    public void validExpenseTransactionMustPass() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(garvit.getParty().getName().toString(), 50000L);
        splits.put(arnav.getParty().getName().toString(), 50000L);
        splits.put(mridul.getParty().getName().toString(), 50000L);

        ExpenseState state = new ExpenseState(
                "exp-1", "grp-1", garvit.getParty(), 150000L, "INR",
                Arrays.asList(garvit.getParty(), arnav.getParty(), mridul.getParty()),
                "EQUAL", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(garvit.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void zeroAmountExpenseMustFail() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(garvit.getParty().getName().toString(), 0L);
        splits.put(arnav.getParty().getName().toString(), 0L);

        ExpenseState state = new ExpenseState(
                "exp-2", "grp-1", garvit.getParty(), 0L, "INR",
                Arrays.asList(garvit.getParty(), arnav.getParty()),
                "EQUAL", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(garvit.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.failsWith("Expense amount must be positive.");
            });
            return null;
        });
    }

    @Test
    public void splitSumMismatchMustFail() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(garvit.getParty().getName().toString(), 40000L);
        splits.put(arnav.getParty().getName().toString(), 40000L);

        ExpenseState state = new ExpenseState(
                "exp-3", "grp-1", garvit.getParty(), 100000L, "INR",
                Arrays.asList(garvit.getParty(), arnav.getParty()),
                "CUSTOM", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(garvit.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.failsWith("Sum of splitDetails must equal the total expense amount.");
            });
            return null;
        });
    }

    @Test
    public void missingPayerSignatureMustFail() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(garvit.getParty().getName().toString(), 50000L);
        splits.put(arnav.getParty().getName().toString(), 50000L);

        ExpenseState state = new ExpenseState(
                "exp-4", "grp-1", garvit.getParty(), 100000L, "INR",
                Arrays.asList(garvit.getParty(), arnav.getParty()),
                "EQUAL", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(arnav.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.failsWith("Payer must sign the expense transaction.");
            });
            return null;
        });
    }
}
