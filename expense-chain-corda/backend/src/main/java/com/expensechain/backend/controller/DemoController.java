package com.expensechain.backend.controller;

import com.expensechain.backend.model.Group;
import com.expensechain.backend.model.User;
import com.expensechain.backend.service.DataStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DemoController {

    private final DataStoreService dataStore;

    public DemoController(DataStoreService dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Initializes or fetches active Demo Session and dataset
     */
    @GetMapping({"/demo/session", "/demo/data", "/demo"})
    public ResponseEntity<?> getDemoSession() {
        List<User> users = dataStore.getAllUsers(true);
        if (users.isEmpty()) {
            dataStore.resetDemoStore();
            users = dataStore.getAllUsers(true);
        }
        User activeUser = users.get(0);
        List<Group> groups = dataStore.getGroupsForUser(true, activeUser.getId());
        List<com.expensechain.backend.model.Expense> expenses = new ArrayList<>();
        for (Group g : groups) {
            expenses.addAll(dataStore.getExpensesForGroup(true, g.getId()));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("users", users);
        resp.put("activeUser", activeUser);
        resp.put("groups", groups);
        resp.put("expenses", expenses);
        resp.put("mode", "DEMO");
        return ResponseEntity.ok(resp);
    }

    /**
     * Re-seeds the demo partition with a brand new randomized dataset
     */
    @PostMapping("/demo/reset")
    public ResponseEntity<?> resetDemo() {
        dataStore.resetDemoStore();
        List<User> users = dataStore.getAllUsers(true);
        User activeUser = users.isEmpty() ? null : users.get(0);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Demo environment re-seeded with fresh randomized dataset");
        resp.put("users", users);
        resp.put("activeUser", activeUser);
        resp.put("groups", activeUser != null ? dataStore.getGroupsForUser(true, activeUser.getId()) : Collections.emptyList());
        return ResponseEntity.ok(resp);
    }

    /**
     * Full development environment reset (wipes main data and re-seeds demo data)
     */
    @PostMapping("/dev/reset")
    public ResponseEntity<?> resetDevEnvironment(@RequestParam(required = false, defaultValue = "true") boolean seedDemo) {
        dataStore.resetDevEnvironment(seedDemo);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Development environment reset successfully");
        resp.put("mainStoreClean", true);
        resp.put("demoSeeded", seedDemo);
        return ResponseEntity.ok(resp);
    }
}
