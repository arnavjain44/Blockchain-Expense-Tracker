package com.expensechain.backend.controller;

import com.expensechain.backend.dto.UserDto;
import com.expensechain.backend.model.User;
import com.expensechain.backend.service.DataStoreService;
import org.springframework.http.HttpStatus;
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
        String identifier = body.get("email");
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = body.get("username");
        }
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = body.get("identifier");
        }
        String password = body.get("password");

        User user = dataStore.authenticate(isDemo, identifier, password);
        if (user == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Invalid email/username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        String token = dataStore.createSession(isDemo, user.getId());

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", UserDto.fromUser(user));
        resp.put("token", token);
        resp.put("isDemo", isDemo);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body,
                                      @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                      @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        String name = body.get("name");
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String phone = body.get("phone");
        String upiId = body.get("upiId");

        try {
            User user = dataStore.registerUser(isDemo, name, username, email, password, phone, upiId, null);
            String token = dataStore.createSession(isDemo, user.getId());

            Map<String, Object> resp = new HashMap<>();
            resp.put("user", UserDto.fromUser(user));
            resp.put("token", token);
            resp.put("isDemo", isDemo);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                            @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User user = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (user == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Unauthorized or session expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("user", UserDto.fromUser(user));
        resp.put("isDemo", isDemo);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                    @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        dataStore.revokeSession(authHeader, isDemo);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Logged out successfully");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/profile/upi")
    public ResponseEntity<?> getUpiId(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                      @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User user = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("upiId", user.getUpiId());
        resp.put("userId", user.getId());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/profile/upi")
    public ResponseEntity<?> updateUpiId(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                         @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User user = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        String upiId = body.get("upiId");
        try {
            User updated = dataStore.updateUpiId(isDemo, user.getId(), upiId);
            return ResponseEntity.ok(UserDto.fromUser(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Collections.singletonMap("error", "Global user directory enumeration is disabled for privacy"));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id,
                                     @RequestHeader(value = "Authorization", required = false) String authHeader,
                                     @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                     @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User current = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        if (!current.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error", "Access denied"));
        }
        return ResponseEntity.ok(UserDto.fromUser(current));
    }
}
