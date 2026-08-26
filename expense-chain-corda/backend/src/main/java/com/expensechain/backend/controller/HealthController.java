package com.expensechain.backend.controller;

import com.expensechain.backend.service.CordaRpcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for dev automation and readiness checks.
 * Returns non-blocking instant responses.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final CordaRpcService cordaRpcService;

    public HealthController(CordaRpcService cordaRpcService) {
        this.cordaRpcService = cordaRpcService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "ExpenseChain Backend");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, Boolean> nodeStatus = new HashMap<>();
        boolean allReady = true;
        for (String node : new String[]{"Garvit", "Arnav", "Mridul"}) {
            boolean connected = cordaRpcService.isNodeConnectedQuick(node);
            nodeStatus.put(node, connected);
            if (!connected) allReady = false;
        }

        Map<String, Object> cordaInfo = new HashMap<>();
        cordaInfo.put("nodes", nodeStatus);
        cordaInfo.put("allNodesReady", allReady);
        health.put("corda", cordaInfo);

        return ResponseEntity.ok(health);
    }
}
