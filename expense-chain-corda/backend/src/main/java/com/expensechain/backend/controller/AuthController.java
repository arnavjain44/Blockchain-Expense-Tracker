package com.expensechain.backend.controller;

import com.expensechain.backend.model.User;
import com.expensechain.backend.service.DataStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final DataStoreService dataStore;

    public AuthController(DataStoreService dataStore) {
        this.dataStore = dataStore;
    }

    private boolean resolveDemo(Boolean headerDemo, Boolean queryDemo) {
        return Boolean.TRUE.equals(headerDemo) || Boolean.TRUE.equals(queryDemo);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                   @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        String email = body.get("email");
        String password = body.get("password");

        User user = dataStore.authenticate(isDemo, email, password);
        if (user == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Invalid email or password");
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", user);
        resp.put("token", (isDemo ? "demo-token-" : "auth-token-") + user.getId());
        resp.put("isDemo", isDemo);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body,
                                      @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                      @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String phone = body.get("phone");

        try {
            User user = dataStore.registerUser(isDemo, name, email, password, phone, null);
            Map<String, Object> resp = new HashMap<>();
            resp.put("user", user);
            resp.put("token", (isDemo ? "demo-token-" : "auth-token-") + user.getId());
            resp.put("isDemo", isDemo);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                                  @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        return ResponseEntity.ok(dataStore.getAllUsers(isDemo));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id,
                                     @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                     @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User u = dataStore.getUserById(isDemo, id);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(u);
    }
}
