package com.expensechain.flows;

import com.expensechain.states.ExpenseState;
import com.expensechain.states.SettlementState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.expensechain.contracts"),
                TestCordapp.findCordapp("com.expensechain.flows")
        )));
        a = network.createPartyNode(new CordaX500Name("Garvit", "New Delhi", "IN"));
        b = network.createPartyNode(new CordaX500Name("Arnav", "Mumbai", "IN"));
        c = network.createPartyNode(new CordaX500Name("Mridul", "Bengaluru", "IN"));
        network.runNetwork();
    }

    @After
    public void tearDown() {
        if (network != null) {
            network.stopNodes();
        }
    }

    @Test
    public void testAddExpenseFlowMultiPartyVaultRecording() throws Exception {
        Map<String, Long> splits = new LinkedHashMap<>();
        splits.put(a.getInfo().getLegalIdentities().get(0).getName().toString(), 50000L);
        splits.put(b.getInfo().getLegalIdentities().get(0).getName().toString(), 50000L);
        splits.put(c.getInfo().getLegalIdentities().get(0).getName().toString(), 50000L);

        AddExpenseFlow flow = new AddExpenseFlow(
                "exp-100", "grp-1", 150000L, "INR",
                Arrays.asList(
                        a.getInfo().getLegalIdentities().get(0),
                        b.getInfo().getLegalIdentities().get(0),
                        c.getInfo().getLegalIdentities().get(0)
                ),
                "EQUAL", splits
        );

        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        assertNotNull(signedTx.getId());

        // Verify state is recorded in all 3 participants' vaults
        assertEquals(1, a.getServices().getVaultService().queryBy(ExpenseState.class).getStates().size());
        assertEquals(1, b.getServices().getVaultService().queryBy(ExpenseState.class).getStates().size());
        assertEquals(1, c.getServices().getVaultService().queryBy(ExpenseState.class).getStates().size());
    }

    @Test
    public void testRecordSettlementFlow() throws Exception {
        RecordSettlementFlow flow = new RecordSettlementFlow(
                "stl-100", "grp-1",
                a.getInfo().getLegalIdentities().get(0), // payee
                50000L, "INR"
        );

        CordaFuture<SignedTransaction> future = b.startFlow(flow); // b is payer
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        assertNotNull(signedTx.getId());

        // Verify settlement recorded in both payer and payee vaults
        assertEquals(1, a.getServices().getVaultService().queryBy(SettlementState.class).getStates().size());
        assertEquals(1, b.getServices().getVaultService().queryBy(SettlementState.class).getStates().size());
    }
}
