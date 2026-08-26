package com.expensechain.backend.service;

import com.expensechain.backend.config.CordaRpcConfig;
import com.expensechain.backend.model.BlockchainRecord;
import com.expensechain.flows.AddExpenseFlow;
import com.expensechain.flows.RecordSettlementFlow;
import com.expensechain.states.ExpenseState;
import com.expensechain.states.SettlementState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class CordaRpcService {

    private static final Logger log = LoggerFactory.getLogger(CordaRpcService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final CordaRpcConfig rpcConfig;
    private final Map<String, CordaRPCConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, CordaRPCOps> rpcProxies = new ConcurrentHashMap<>();

    // Thread pool for background asynchronous Corda Flow execution
    private final ExecutorService flowExecutor = Executors.newCachedThreadPool();

    // In-memory tracker for blockchain operations
    public static class OperationStatus {
        private final String operationId;
        private final String type;
        private volatile String status; // "PROCESSING", "CONFIRMED_ON_CORDA", "FAILED"
        private volatile String cordaTxId;
        private volatile String notary;
        private volatile String errorMessage;
        private volatile Object resultData;
        private final long createdAt;
        private volatile long completedAt;

        public OperationStatus(String operationId, String type) {
            this.operationId = operationId;
            this.type = type;
            this.status = "PROCESSING";
            this.createdAt = System.currentTimeMillis();
        }

        public String getOperationId() { return operationId; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCordaTxId() { return cordaTxId; }
        public void setCordaTxId(String cordaTxId) { this.cordaTxId = cordaTxId; }
        public String getNotary() { return notary; }
        public void setNotary(String notary) { this.notary = notary; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Object getResultData() { return resultData; }
        public void setResultData(Object resultData) { this.resultData = resultData; }
        public long getCreatedAt() { return createdAt; }
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    }

    private final Map<String, OperationStatus> operations = new ConcurrentHashMap<>();

    public CordaRpcService(CordaRpcConfig rpcConfig) {
        this.rpcConfig = rpcConfig;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Corda RPC connections...");
        Thread t = new Thread(() -> {
            for (Map.Entry<String, CordaRpcConfig.NodeProperties> entry : rpcConfig.getNodes().entrySet()) {
                String nodeKey = entry.getKey();
                CordaRpcConfig.NodeProperties props = entry.getValue();
                tryConnect(nodeKey, props);
            }
        }, "CordaRpcInitThread");
        t.setDaemon(true);
        t.start();
    }

    public synchronized CordaRPCOps getProxy(String nodeName) {
        CordaRPCOps proxy = rpcProxies.get(nodeName);
        if (proxy != null) {
            try {
                proxy.nodeInfo();
                return proxy;
            } catch (Exception e) {
                log.warn("RPC proxy for {} lost connection. Reconnecting...", nodeName);
                rpcProxies.remove(nodeName);
                CordaRPCConnection conn = connections.remove(nodeName);
                if (conn != null) {
                    try { conn.notifyServerAndClose(); } catch (Exception ignored) {}
                }
            }
        }

        CordaRpcConfig.NodeProperties props = rpcConfig.getNodes().get(nodeName);
        if (props != null) {
            tryConnect(nodeName, props);
        }
        return rpcProxies.get(nodeName);
    }

    private void tryConnect(String nodeKey, CordaRpcConfig.NodeProperties props) {
        try {
            NetworkHostAndPort hostAndPort = new NetworkHostAndPort(props.getHost(), props.getRpcPort());
            CordaRPCClient client = new CordaRPCClient(hostAndPort);
            CordaRPCConnection conn = client.start(props.getUsername(), props.getPassword());
            connections.put(nodeKey, conn);
            CordaRPCOps proxy = conn.getProxy();
            rpcProxies.put(nodeKey, proxy);
            log.info("Connected to Corda RPC for node: {} at port {}", nodeKey, props.getRpcPort());

            try {
                proxy.nodeInfo();
                proxy.getNetworkParameters();
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.warn("Could not connect to Corda node {} (port {}): {}",
                    nodeKey, props.getRpcPort(), e.getMessage());
        }
    }

    public boolean isNodeConnected(String nodeName) {
        CordaRPCOps proxy = getProxy(nodeName);
        if (proxy == null) return false;
        try {
            return proxy.nodeInfo() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isNodeConnectedQuick(String nodeName) {
        return rpcProxies.containsKey(nodeName);
    }

    public boolean areAllNodesConnected() {
        for (String nodeName : rpcConfig.getNodes().keySet()) {
            if (!isNodeConnected(nodeName)) return false;
        }
        return true;
    }

    public OperationStatus getOperationStatus(String operationId) {
        return operations.get(operationId);
    }

    /**
     * Resolves a node key ("Garvit", "Arnav", "Mridul") or X500 String into a live Corda Party.
     */
    public Party resolveParty(CordaRPCOps proxy, String nodeOrX500) {
        if (nodeOrX500 == null || proxy == null) return null;

        // 1. Try parsing as X500 name
        try {
            CordaX500Name x500 = CordaX500Name.parse(nodeOrX500);
            Party p = proxy.wellKnownPartyFromX500Name(x500);
            if (p != null) return p;
        } catch (Exception ignored) {}

        // 2. Try looking up through peer node configs
        for (Map.Entry<String, CordaRpcConfig.NodeProperties> entry : rpcConfig.getNodes().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(nodeOrX500) ||
                (entry.getValue().getX500Name() != null && entry.getValue().getX500Name().equalsIgnoreCase(nodeOrX500))) {
                try {
                    CordaX500Name x500 = CordaX500Name.parse(entry.getValue().getX500Name());
                    Party p = proxy.wellKnownPartyFromX500Name(x500);
                    if (p != null) return p;
                } catch (Exception ignored) {}

                // Try direct peer proxy nodeInfo
                CordaRPCOps peerProxy = getProxy(entry.getKey());
                if (peerProxy != null) {
                    try {
                        return peerProxy.nodeInfo().getLegalIdentities().get(0);
                    } catch (Exception ignored) {}
                }
            }
        }

        return null;
    }

    /**
     * Executes AddExpenseFlow on the payer node via Corda RPC with multi-party consensus and Notary finality.
     */
    public SignedTransaction executeAddExpenseFlow(String payerNodeName,
                                                  String expenseId,
                                                  String groupId,
                                                  long amountMinorUnits,
                                                  String currency,
                                                  List<String> participantNodeIdentifiers,
                                                  String splitType,
                                                  Map<String, Long> rawSplits) throws Exception {
        CordaRPCOps proxy = getProxy(payerNodeName);
        if (proxy == null) {
            throw new IllegalStateException("Node " + payerNodeName + " RPC is not connected. Please ensure Corda nodes are running.");
        }

        Party payer = proxy.nodeInfo().getLegalIdentities().get(0);
        Set<Party> participantsSet = new LinkedHashSet<>();
        participantsSet.add(payer);

        if (participantNodeIdentifiers != null) {
            for (String nodeIdent : participantNodeIdentifiers) {
                Party p = resolveParty(proxy, nodeIdent);
                if (p != null) {
                    participantsSet.add(p);
                }
            }
        }

        List<Party> participants = new ArrayList<>(participantsSet);

        // Build valid splitDetails for participants matching Contract rules
        Map<String, Long> validSplitDetails = new LinkedHashMap<>();
        if ("CUSTOM".equalsIgnoreCase(splitType) && rawSplits != null && !rawSplits.isEmpty()) {
            long currentSum = 0;
            for (Party p : participants) {
                String orgName = p.getName().getOrganisation();
                String fullX500 = p.getName().toString();

                Long amountVal = null;
                for (Map.Entry<String, Long> entry : rawSplits.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(orgName) || entry.getKey().equalsIgnoreCase(fullX500)) {
                        amountVal = entry.getValue();
                        break;
                    }
                }
                if (amountVal == null) {
                    amountVal = 0L;
                }
                currentSum += amountVal;
                validSplitDetails.put(fullX500, amountVal);
            }
            if (currentSum != amountMinorUnits && !participants.isEmpty()) {
                Party lastP = participants.get(participants.size() - 1);
                long diff = amountMinorUnits - currentSum;
                validSplitDetails.put(lastP.getName().toString(), validSplitDetails.get(lastP.getName().toString()) + diff);
            }
        } else {
            int n = participants.size();
            long shareMinor = amountMinorUnits / n;
            long remainder = amountMinorUnits - (shareMinor * n);
            for (int i = 0; i < n; i++) {
                Party p = participants.get(i);
                long share = shareMinor + (i == n - 1 ? remainder : 0);
                validSplitDetails.put(p.getName().toString(), share);
            }
        }

        log.info("Starting AddExpenseFlow on node {} (Payer: {}, Participants: {}, Amount: {} minor units)",
                payerNodeName, payer.getName().getOrganisation(), participants.size(), amountMinorUnits);

        FlowHandle<SignedTransaction> flowHandle = proxy.startFlowDynamic(
                AddExpenseFlow.class,
                expenseId,
                groupId,
                amountMinorUnits,
                currency,
                participants,
                splitType,
                validSplitDetails
        );

        return flowHandle.getReturnValue().get(20, TimeUnit.SECONDS);
    }

    /**
     * Executes RecordSettlementFlow on the payer node via Corda RPC.
     */
    public SignedTransaction executeRecordSettlementFlow(String payerNodeName,
                                                        String settlementId,
                                                        String groupId,
                                                        String payeeNodeIdentifier,
                                                        long amountMinorUnits,
                                                        String currency) throws Exception {
        CordaRPCOps proxy = getProxy(payerNodeName);
        if (proxy == null) {
            throw new IllegalStateException("Node " + payerNodeName + " RPC is not connected. Please ensure Corda nodes are running.");
        }

        Party payer = proxy.nodeInfo().getLegalIdentities().get(0);
        Party payee = resolveParty(proxy, payeeNodeIdentifier);

        if (payee == null || payee.equals(payer)) {
            // Find another distinct running peer node
            for (String peerName : Arrays.asList("Arnav", "Mridul", "Garvit")) {
                if (!peerName.equalsIgnoreCase(payerNodeName)) {
                    Party p = resolveParty(proxy, peerName);
                    if (p != null && !p.equals(payer)) {
                        payee = p;
                        break;
                    }
                }
            }
        }

        if (payee == null || payee.equals(payer)) {
            throw new IllegalStateException("Cannot record settlement on Corda: payee node is offline or identical to payer node.");
        }

        log.info("Starting RecordSettlementFlow on node {} (Payer: {}, Payee: {}, Amount: {} minor units)",
                payerNodeName, payer.getName().getOrganisation(), payee.getName().getOrganisation(), amountMinorUnits);

        FlowHandle<SignedTransaction> flowHandle = proxy.startFlowDynamic(
                RecordSettlementFlow.class,
                settlementId,
                groupId,
                payee,
                amountMinorUnits,
                currency
        );

        return flowHandle.getReturnValue().get(20, TimeUnit.SECONDS);
    }

    /**
     * Submits an asynchronous Expense operation and returns the OperationStatus with operationId.
     */
    public OperationStatus submitAsyncExpense(String operationId,
                                              String payerNodeName,
                                              String expenseId,
                                              String groupId,
                                              long amountMinorUnits,
                                              String currency,
                                              List<String> participantNodes,
                                              String splitType,
                                              Map<String, Long> rawSplits,
                                              java.util.function.Function<String, Object> onConfirmAction) {
        OperationStatus op = new OperationStatus(operationId, "EXPENSE");
        operations.put(operationId, op);

        flowExecutor.submit(() -> {
            try {
                SignedTransaction stx = executeAddExpenseFlow(
                        payerNodeName, expenseId, groupId, amountMinorUnits, currency, participantNodes, splitType, rawSplits);
                if (stx != null) {
                    String txId = stx.getId().toString();
                    op.setCordaTxId(txId);
                    op.setNotary(stx.getNotary() != null ? stx.getNotary().getName().toString() : "O=Notary,L=London,C=GB");
                    op.setStatus("CONFIRMED_ON_CORDA");
                    op.setCompletedAt(System.currentTimeMillis());
                    if (onConfirmAction != null) {
                        try { op.setResultData(onConfirmAction.apply(txId)); } catch (Exception ignored) {}
                    }
                    log.info("Async operation {} (Expense) completed successfully. Corda TxId: {}", operationId, op.getCordaTxId());
                } else {
                    op.setStatus("FAILED");
                    op.setErrorMessage("Flow returned null transaction");
                    op.setCompletedAt(System.currentTimeMillis());
                }
            } catch (Exception e) {
                log.error("Async operation {} (Expense) failed: {}", operationId, e.getMessage());
                op.setStatus("FAILED");
                op.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.toString());
                op.setCompletedAt(System.currentTimeMillis());
            }
        });

        return op;
    }

    /**
     * Submits an asynchronous Settlement operation and returns the OperationStatus with operationId.
     */
    public OperationStatus submitAsyncSettlement(String operationId,
                                                 String payerNodeName,
                                                 String settlementId,
                                                 String groupId,
                                                 String payeeNodeIdentifier,
                                                 long amountMinorUnits,
                                                 String currency,
                                                 java.util.function.Function<String, Object> onConfirmAction) {
        OperationStatus op = new OperationStatus(operationId, "SETTLEMENT");
        operations.put(operationId, op);

        flowExecutor.submit(() -> {
            try {
                SignedTransaction stx = executeRecordSettlementFlow(
                        payerNodeName, settlementId, groupId, payeeNodeIdentifier, amountMinorUnits, currency);
                if (stx != null) {
                    String txId = stx.getId().toString();
                    op.setCordaTxId(txId);
                    op.setNotary(stx.getNotary() != null ? stx.getNotary().getName().toString() : "O=Notary,L=London,C=GB");
                    op.setStatus("CONFIRMED_ON_CORDA");
                    op.setCompletedAt(System.currentTimeMillis());
                    if (onConfirmAction != null) {
                        try { op.setResultData(onConfirmAction.apply(txId)); } catch (Exception ignored) {}
                    }
                    log.info("Async operation {} (Settlement) completed successfully. Corda TxId: {}", operationId, op.getCordaTxId());
                } else {
                    op.setStatus("FAILED");
                    op.setErrorMessage("Flow returned null transaction");
                    op.setCompletedAt(System.currentTimeMillis());
                }
            } catch (Exception e) {
                log.error("Async operation {} (Settlement) failed: {}", operationId, e.getMessage());
                op.setStatus("FAILED");
                op.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.toString());
                op.setCompletedAt(System.currentTimeMillis());
            }
        });

        return op;
    }

    /**
     * Queries the Corda Vault for all ExpenseState and SettlementState records on a node.
     */
    public List<BlockchainRecord> getBlockchainRecordsForNode(String nodeName) {
        List<BlockchainRecord> records = new ArrayList<>();
        CordaRPCOps proxy = getProxy(nodeName);
        if (proxy == null) return records;

        try {
            // 1. Query ExpenseStates
            Vault.Page<ExpenseState> expensePage = proxy.vaultQuery(ExpenseState.class);
            for (StateAndRef<ExpenseState> sar : expensePage.getStates()) {
                ExpenseState st = sar.getState().getData();
                String txId = sar.getRef().getTxhash().toString();
                String notary = sar.getState().getNotary().getName().toString();
                List<String> signers = Collections.singletonList(st.getPayer().getName().toString());
                List<String> partNames = st.getParticipants().stream()
                        .map(AbstractParty::nameOrNull)
                        .filter(Objects::nonNull)
                        .map(CordaX500Name::toString)
                        .collect(Collectors.toList());

                records.add(new BlockchainRecord(
                        txId,
                        "ExpenseState",
                        "EXPENSE",
                        st.getExpenseId(),
                        notary,
                        signers,
                        partNames,
                        String.format("₹%.2f %s", st.getAmountMinorUnits() / 100.0, st.getCurrency()),
                        st.getRecordedAt() != null ? st.getRecordedAt().toString() : ISO_FORMATTER.format(new Date().toInstant()),
                        "CONFIRMED_ON_LEDGER",
                        true
                ));
            }

            // 2. Query SettlementStates
            Vault.Page<SettlementState> settlementPage = proxy.vaultQuery(SettlementState.class);
            for (StateAndRef<SettlementState> sar : settlementPage.getStates()) {
                SettlementState st = sar.getState().getData();
                String txId = sar.getRef().getTxhash().toString();
                String notary = sar.getState().getNotary().getName().toString();
                List<String> signers = Collections.singletonList(st.getPayer().getName().toString());
                List<String> partNames = Arrays.asList(
                        st.getPayer().getName().toString(),
                        st.getPayee().getName().toString()
                );

                boolean isRejected = st.getSettlementId() != null && st.getSettlementId().startsWith("REJECT");
                String vaultStatus = isRejected ? "REJECTED_ON_LEDGER" : "CONFIRMED_ON_LEDGER";

                records.add(new BlockchainRecord(
                        txId,
                        "SettlementState",
                        "SETTLEMENT",
                        st.getSettlementId(),
                        notary,
                        signers,
                        partNames,
                        String.format("₹%.2f %s", st.getAmountMinorUnits() / 100.0, st.getCurrency()),
                        st.getSettledAt() != null ? st.getSettledAt().toString() : ISO_FORMATTER.format(new Date().toInstant()),
                        vaultStatus,
                        true
                ));
            }

            records.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        } catch (Exception e) {
            log.warn("Error querying Corda vault for node {}: {}", nodeName, e.getMessage());
        }

        return records;
    }

    public boolean verifyLedgerState(String nodeName, String txId) {
        CordaRPCOps proxy = getProxy(nodeName);
        if (proxy == null) return false;
        try {
            Vault.Page<ExpenseState> expensePage = proxy.vaultQuery(ExpenseState.class);
            for (StateAndRef<ExpenseState> sar : expensePage.getStates()) {
                if (sar.getRef().getTxhash().toString().equalsIgnoreCase(txId)) return true;
            }
            Vault.Page<SettlementState> settlementPage = proxy.vaultQuery(SettlementState.class);
            for (StateAndRef<SettlementState> sar : settlementPage.getStates()) {
                if (sar.getRef().getTxhash().toString().equalsIgnoreCase(txId)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void cleanup() {
        flowExecutor.shutdownNow();
        for (Map.Entry<String, CordaRPCConnection> entry : connections.entrySet()) {
            try {
                entry.getValue().notifyServerAndClose();
            } catch (Exception ignored) {}
        }
    }
}
