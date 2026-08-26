package com.expensechain.backend.controller;

import com.expensechain.backend.model.Expense;
import com.expensechain.backend.model.User;
import com.expensechain.backend.service.CordaRpcService;
import com.expensechain.backend.service.DataStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    private final DataStoreService dataStore;
    private final CordaRpcService cordaRpc;

    public ExpenseController(DataStoreService dataStore, CordaRpcService cordaRpc) {
        this.dataStore = dataStore;
        this.cordaRpc = cordaRpc;
    }

    private boolean resolveDemo(Boolean headerDemo, Boolean queryDemo) {
        return Boolean.TRUE.equals(headerDemo) || Boolean.TRUE.equals(queryDemo);
    }

    private String resolveCordaNodeName(User user) {
        if (user == null) return "Garvit";
        String x500 = user.getCordaX500Name();
        if (x500 != null) {
            if (x500.contains("O=Garvit")) return "Garvit";
            if (x500.contains("O=Arnav")) return "Arnav";
            if (x500.contains("O=Mridul")) return "Mridul";
        }
        int idx = (int) (Math.abs(user.getId()) % 3);
        return DataStoreService.CORDA_NODE_NAMES[idx];
    }

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody Map<String, Object> body,
                                        @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                        @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        try {
            Long groupId = ((Number) body.get("groupId")).longValue();
            String title = (String) body.get("title");
            double amount = ((Number) body.get("amount")).doubleValue();
            String category = (String) body.getOrDefault("category", "FOOD");
            String description = (String) body.getOrDefault("description", "");
            String expenseDate = (String) body.get("expenseDate");
            Long paidBy = ((Number) body.get("paidBy")).longValue();
            String splitType = (String) body.getOrDefault("splitType", "EQUAL");

            User payerUser = dataStore.getUserById(isDemo, paidBy);
            if (payerUser == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Payer user not found"));
            }

            // Extract participant user IDs
            List<?> rawParticipants = (List<?>) body.get("participants");
            List<Map<String, Object>> calculatedSplits = new ArrayList<>();
            List<String> participantNodeNames = new ArrayList<>();
            Map<String, Long> splitDetails = new LinkedHashMap<>();

            long totalMinorUnits = Math.round(amount * 100.0);

            if ("EQUAL".equalsIgnoreCase(splitType)) {
                List<Long> pUserIds = new ArrayList<>();
                for (Object p : rawParticipants) {
                    if (p instanceof Number) {
                        pUserIds.add(((Number) p).longValue());
                    } else if (p instanceof Map) {
                        pUserIds.add(((Number) ((Map<?, ?>) p).get("userId")).longValue());
                    }
                }

                int n = pUserIds.size();
                if (n == 0) throw new IllegalArgumentException("At least one participant required");
                long shareMinor = totalMinorUnits / n;
                long remainder = totalMinorUnits - (shareMinor * n);

                for (int i = 0; i < n; i++) {
                    Long uid = pUserIds.get(i);
                    User u = dataStore.getUserById(isDemo, uid);
                    if (u == null) continue;
                    long thisShare = shareMinor + (i == n - 1 ? remainder : 0);

                    String node = resolveCordaNodeName(u);
                    participantNodeNames.add(node);
                    splitDetails.put(node, thisShare);

                    Map<String, Object> splitItem = new HashMap<>();
                    splitItem.put("userId", uid);
                    splitItem.put("shareAmount", thisShare / 100.0);
                    calculatedSplits.add(splitItem);
                }
            } else {
                long sumMinor = 0;
                for (Object p : rawParticipants) {
                    Map<?, ?> pMap = (Map<?, ?>) p;
                    Long uid = ((Number) pMap.get("userId")).longValue();
                    double customAmt = ((Number) pMap.get("amount")).doubleValue();
                    long cMinor = Math.round(customAmt * 100.0);
                    sumMinor += cMinor;

                    User u = dataStore.getUserById(isDemo, uid);
                    if (u != null) {
                        String node = resolveCordaNodeName(u);
                        participantNodeNames.add(node);
                        splitDetails.put(node, cMinor);
                    }

                    Map<String, Object> splitItem = new HashMap<>();
                    splitItem.put("userId", uid);
                    splitItem.put("shareAmount", customAmt);
                    calculatedSplits.add(splitItem);
                }

                if (sumMinor != totalMinorUnits) {
                    throw new IllegalArgumentException(String.format("Custom split total (₹%.2f) does not match expense amount (₹%.2f)",
                            sumMinor / 100.0, amount));
                }
            }

            String payerNode = resolveCordaNodeName(payerUser);
            String expenseIdStr = "EXP-" + System.currentTimeMillis();
            String operationId = "OP-EXP-" + System.currentTimeMillis();

            // Submit async flow execution
            CordaRpcService.OperationStatus op = cordaRpc.submitAsyncExpense(
                    operationId,
                    payerNode,
                    expenseIdStr,
                    String.valueOf(groupId),
                    totalMinorUnits,
                    "INR",
                    participantNodeNames,
                    splitType,
                    splitDetails,
                    txId -> dataStore.saveExpense(
                            isDemo, groupId, title, amount, category, description,
                            expenseDate, paidBy, splitType, txId, calculatedSplits
                    )
            );

            // Fast Synchronous Window: wait up to 4.5 seconds for immediate response
            long waitStart = System.currentTimeMillis();
            while ("PROCESSING".equals(op.getStatus()) && (System.currentTimeMillis() - waitStart) < 4500) {
                Thread.sleep(150);
            }

            if ("CONFIRMED_ON_CORDA".equals(op.getStatus())) {
                Expense expense = (op.getResultData() instanceof Expense) ?
                        (Expense) op.getResultData() :
                        dataStore.saveExpense(
                                isDemo, groupId, title, amount, category, description,
                                expenseDate, paidBy, splitType, op.getCordaTxId(), calculatedSplits
                        );

                Map<String, Object> resp = new HashMap<>();
                resp.put("expense", expense);
                resp.put("cordaTxId", op.getCordaTxId());
                resp.put("notary", op.getNotary());
                resp.put("liveCorda", true);
                resp.put("status", "CONFIRMED_ON_CORDA");
                return ResponseEntity.ok(resp);
            } else if ("FAILED".equals(op.getStatus())) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", op.getErrorMessage() != null ? op.getErrorMessage() : "Corda Flow execution failed");
                err.put("status", "FAILED");
                return ResponseEntity.badRequest().body(err);
            } else {
                // If still processing after fast window, return operationId for async client polling
                Map<String, Object> resp = new HashMap<>();
                resp.put("operationId", operationId);
                resp.put("status", "PROCESSING");
                resp.put("liveCorda", true);
                resp.put("message", "Initiating Corda Flow & Notary Consensus in background...");
                return ResponseEntity.ok(resp);
            }
        } catch (Exception e) {
            log.error("Error adding expense: ", e);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
