package com.expensechain.backend.controller;

import com.expensechain.backend.model.Settlement;
import com.expensechain.backend.model.User;
import com.expensechain.backend.service.CordaRpcService;
import com.expensechain.backend.service.DataStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/settlements")
@CrossOrigin(origins = "*")
public class SettlementController {

    private static final Logger log = LoggerFactory.getLogger(SettlementController.class);

    private final DataStoreService dataStore;
    private final CordaRpcService cordaRpc;

    public SettlementController(DataStoreService dataStore, CordaRpcService cordaRpc) {
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
    public ResponseEntity<?> recordSettlement(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                              @RequestBody Map<String, Object> body,
                                              @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                              @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Unauthorized"));
        }
        try {
            Long groupId = ((Number) body.get("groupId")).longValue();
            Long paidBy = currentUser.getId(); // Enforce authenticated user as payer
            Long paidTo = ((Number) body.get("paidTo")).longValue();
            double amount = ((Number) body.get("amount")).doubleValue();

            if (!dataStore.isUserMemberOfGroup(isDemo, groupId, paidBy)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("error", "You are not a member of this group"));
            }
            if (!dataStore.isUserMemberOfGroup(isDemo, groupId, paidTo)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Recipient must be a member of the group"));
            }
            if (paidBy.equals(paidTo)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Payer and payee cannot be the same user"));
            }
            if (amount <= 0.0) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Settlement amount must be positive"));
            }

            User payerUser = dataStore.getUserById(isDemo, paidBy);
            User payeeUser = dataStore.getUserById(isDemo, paidTo);

            if (payerUser == null || payeeUser == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Payer or Payee user not found"));
            }

            if (dataStore.hasPendingSettlement(isDemo, groupId, paidBy, paidTo)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "A settlement payment has already been initiated and is awaiting receiver verification."));
            }

            Settlement settlement = dataStore.savePendingSettlement(isDemo, groupId, paidBy, paidTo, amount);

            Map<String, Object> resp = new HashMap<>();
            resp.put("settlement", settlement);
            resp.put("status", "PENDING_VERIFICATION");
            resp.put("message", "Settlement initiated! Waiting for receiver (" + payeeUser.getName() + ") to verify payment.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Error recording settlement: ", e);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingSettlements(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                                   @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Unauthorized"));
        }
        // Always return pending settlements for the authenticated session user
        List<Settlement> pending = dataStore.getPendingSettlementsForUser(isDemo, currentUser.getId());
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifySettlement(@PathVariable Long id,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader,
                                              @RequestBody Map<String, Object> body,
                                              @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                              @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Unauthorized"));
        }
        try {
            boolean approved = Boolean.TRUE.equals(body.get("approved")) || "APPROVE".equalsIgnoreCase(String.valueOf(body.get("action")));

            Settlement settlement = dataStore.getSettlementById(isDemo, id);
            if (settlement == null) {
                return ResponseEntity.notFound().build();
            }

            // Verify caller is the intended recipient
            if (!settlement.getPaidTo().equals(currentUser.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(Collections.singletonMap("error", "Only the settlement recipient can verify or reject this payment"));
            }

            if (!"PENDING_VERIFICATION".equalsIgnoreCase(settlement.getStatus())) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Settlement is not pending verification"));
            }

            User payerUser = dataStore.getUserById(isDemo, settlement.getPaidBy());
            User payeeUser = dataStore.getUserById(isDemo, settlement.getPaidTo());

            long amountMinorUnits = Math.round(settlement.getAmount() * 100.0);
            String settlementIdStr = (approved ? "SETTLE-" : "REJECT-STL-") + settlement.getId() + "-" + System.currentTimeMillis();
            String operationId = "OP-STL-" + System.currentTimeMillis();

            String payerNode = resolveCordaNodeName(payerUser);
            String payeeNode = resolveCordaNodeName(payeeUser);

            if (payerNode.equalsIgnoreCase(payeeNode)) {
                for (String node : DataStoreService.CORDA_NODE_NAMES) {
                    if (!node.equalsIgnoreCase(payerNode)) {
                        payeeNode = node;
                        break;
                    }
                }
            }

            String targetStatus = approved ? "CONFIRMED_ON_CORDA" : "REJECTED";

            CordaRpcService.OperationStatus op = cordaRpc.submitAsyncSettlement(
                    operationId,
                    payerNode,
                    settlementIdStr,
                    String.valueOf(settlement.getGroupId()),
                    payeeNode,
                    amountMinorUnits,
                    "INR",
                    txId -> dataStore.updateSettlementStatus(isDemo, id, targetStatus, txId)
            );

            long waitStart = System.currentTimeMillis();
            while ("PROCESSING".equals(op.getStatus()) && (System.currentTimeMillis() - waitStart) < 4500) {
                Thread.sleep(150);
            }

            if ("CONFIRMED_ON_CORDA".equals(op.getStatus())) {
                dataStore.updateSettlementStatus(isDemo, id, targetStatus, op.getCordaTxId());
                Map<String, Object> resp = new HashMap<>();
                resp.put("settlement", settlement);
                resp.put("cordaTxId", op.getCordaTxId());
                resp.put("notary", op.getNotary());
                resp.put("liveCorda", true);
                resp.put("status", targetStatus);
                resp.put("message", approved ? "Payment verified & notarized on Corda Ledger" : "Settlement rejection notarized on Corda Ledger");
                return ResponseEntity.ok(resp);
            } else if ("FAILED".equals(op.getStatus())) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", op.getErrorMessage() != null ? op.getErrorMessage() : "Corda Flow execution failed");
                err.put("status", "FAILED");
                return ResponseEntity.badRequest().body(err);
            } else {
                dataStore.updateSettlementStatus(isDemo, id, targetStatus, null);
                Map<String, Object> resp = new HashMap<>();
                resp.put("operationId", operationId);
                resp.put("status", "PROCESSING");
                resp.put("liveCorda", true);
                resp.put("message", "Initiating Corda Settlement Flow & Notary Consensus in background...");
                return ResponseEntity.ok(resp);
            }
        } catch (Exception e) {
            log.error("Error verifying settlement: ", e);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<?> dismissSettlement(@PathVariable Long id,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader,
                                               @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                               @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Unauthorized"));
        }
        Settlement existing = dataStore.getSettlementById(isDemo, id);
        if (existing == null) return ResponseEntity.notFound().build();
        if (!existing.getPaidBy().equals(currentUser.getId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("error", "Only the payer can dismiss this rejected settlement"));
        }
        Settlement s = dataStore.dismissSettlementRejection(isDemo, id);
        return ResponseEntity.ok(s);
    }
}
