package com.expensechain.contracts;

import com.expensechain.states.SettlementState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class SettlementContractTests {

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
    private TestIdentity debtor;
    private TestIdentity creditor;

    @org.junit.Before
    public void setup() {
        List<String[]> pool = new ArrayList<>(INDIAN_NAMES_POOL);
        Collections.shuffle(pool, new Random());
        debtor = new TestIdentity(new CordaX500Name(pool.get(0)[0], pool.get(0)[1], "IN"));
        creditor = new TestIdentity(new CordaX500Name(pool.get(1)[0], pool.get(1)[1], "IN"));
    }

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
