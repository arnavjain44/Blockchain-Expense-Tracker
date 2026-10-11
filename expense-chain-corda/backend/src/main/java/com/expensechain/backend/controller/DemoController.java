package com.expensechain.backend.controller;

import com.expensechain.backend.dto.UserDto;
import com.expensechain.backend.model.Group;
import com.expensechain.backend.model.User;
import com.expensechain.backend.service.DataStoreService;
import org.springframework.http.HttpStatus;
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
     * Initializes or fetches active Demo Session and dataset.
     * Does NOT expose a directory of users or allow switching between accounts.
     */
    @GetMapping({"/demo/session", "/demo/data", "/demo"})
    public ResponseEntity<?> getDemoSession() {
        List<User> users = dataStore.getAllUsers(true);
        if (users.isEmpty()) {
            dataStore.resetDemoStore();
            users = dataStore.getAllUsers(true);
        }
        User activeUser = users.get(0);
        String token = dataStore.createSession(true, activeUser.getId());

        List<Group> groups = dataStore.getGroupsForUser(true, activeUser.getId());
        List<com.expensechain.backend.model.Expense> expenses = new ArrayList<>();
        for (Group g : groups) {
            expenses.addAll(dataStore.getExpensesForGroup(true, g.getId()));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("activeUser", UserDto.fromUser(activeUser));
        resp.put("token", token);
        resp.put("groups", groups);
        resp.put("expenses", expenses);
        resp.put("mode", "DEMO");
        return ResponseEntity.ok(resp);
    }

    /**
     * Re-seeds the demo partition with a brand new randomized dataset (in-memory only).
     */
    @PostMapping("/demo/reset")
    public ResponseEntity<?> resetDemo() {
        dataStore.resetDemoStore();
        List<User> users = dataStore.getAllUsers(true);
        User activeUser = users.isEmpty() ? null : users.get(0);
        String token = (activeUser != null) ? dataStore.createSession(true, activeUser.getId()) : null;

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Demo environment re-seeded with fresh randomized dataset");
        resp.put("activeUser", UserDto.fromUser(activeUser));
        resp.put("token", token);
        resp.put("groups", activeUser != null ? dataStore.getGroupsForUser(true, activeUser.getId()) : Collections.emptyList());
        return ResponseEntity.ok(resp);
    }

    /**
     * Destructive dev reset is permanently disabled to protect user and application data.
     */
    @PostMapping("/dev/reset")
    public ResponseEntity<?> resetDevEnvironment() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Collections.singletonMap("error", "Development reset endpoint is permanently disabled for security and data protection."));
    }
}
