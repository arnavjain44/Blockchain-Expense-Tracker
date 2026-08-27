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

    private static final List<String[]> INDIAN_NAMES_POOL = Arrays.asList(
            new String[]{"Garvit", "New Delhi"},
            new String[]{"Arnav", "Mumbai"},
            new String[]{"Mridul", "Bengaluru"},
            new String[]{"Aarav", "Delhi"},
            new String[]{"Vivaan", "Ahmedabad"},
            new String[]{"Aditya", "Pune"},
            new String[]{"Vihaan", "Jaipur"},
            new String[]{"Arjun", "Kochi"},
            new String[]{"Sai", "Chennai"},
            new String[]{"Reyansh", "Indore"},
            new String[]{"Ayan", "Kolkata"},
            new String[]{"Krishna", "Hyderabad"},
            new String[]{"Ishaan", "Chandigarh"},
            new String[]{"Shaurya", "Lucknow"},
            new String[]{"Ananya", "Nagpur"},
            new String[]{"Diya", "Coimbatore"},
            new String[]{"Aarohi", "Nashik"},
            new String[]{"Advait", "Surat"},
            new String[]{"Kabir", "Bhopal"},
            new String[]{"Rohan", "Patna"}
    );

    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.expensechain.contracts", "com.expensechain.states"));
    private TestIdentity user1;
    private TestIdentity user2;
    private TestIdentity user3;

    @org.junit.Before
    public void setup() {
        List<String[]> pool = new ArrayList<>(INDIAN_NAMES_POOL);
        Collections.shuffle(pool, new Random());
        user1 = new TestIdentity(new CordaX500Name(pool.get(0)[0], pool.get(0)[1], "IN"));
        user2 = new TestIdentity(new CordaX500Name(pool.get(1)[0], pool.get(1)[1], "IN"));
        user3 = new TestIdentity(new CordaX500Name(pool.get(2)[0], pool.get(2)[1], "IN"));
    }

    @Test
    public void validExpenseTransactionMustPass() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(user1.getParty().getName().toString(), 50000L);
        splits.put(user2.getParty().getName().toString(), 50000L);
        splits.put(user3.getParty().getName().toString(), 50000L);

        ExpenseState state = new ExpenseState(
                "exp-1", "grp-1", user1.getParty(), 150000L, "INR",
                Arrays.asList(user1.getParty(), user2.getParty(), user3.getParty()),
                "EQUAL", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(user1.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void zeroAmountExpenseMustFail() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(user1.getParty().getName().toString(), 0L);
        splits.put(user2.getParty().getName().toString(), 0L);

        ExpenseState state = new ExpenseState(
                "exp-2", "grp-1", user1.getParty(), 0L, "INR",
                Arrays.asList(user1.getParty(), user2.getParty()),
                "EQUAL", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(user1.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.failsWith("Expense amount must be positive.");
            });
            return null;
        });
    }

    @Test
    public void splitSumMismatchMustFail() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(user1.getParty().getName().toString(), 40000L);
        splits.put(user2.getParty().getName().toString(), 40000L);

        ExpenseState state = new ExpenseState(
                "exp-3", "grp-1", user1.getParty(), 100000L, "INR",
                Arrays.asList(user1.getParty(), user2.getParty()),
                "CUSTOM", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(user1.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.failsWith("Sum of splitDetails must equal the total expense amount.");
            });
            return null;
        });
    }

    @Test
    public void missingPayerSignatureMustFail() {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(user1.getParty().getName().toString(), 50000L);
        splits.put(user2.getParty().getName().toString(), 50000L);

        ExpenseState state = new ExpenseState(
                "exp-4", "grp-1", user1.getParty(), 100000L, "INR",
                Arrays.asList(user1.getParty(), user2.getParty()),
                "EQUAL", splits, Instant.now()
        );

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(ExpenseContract.ID, state);
                tx.command(Collections.singletonList(user2.getPublicKey()), new ExpenseContract.Commands.Create());
                return tx.failsWith("Payer must sign the expense transaction.");
            });
            return null;
        });
    }
}
