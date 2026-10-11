package com.expensechain.backend.controller;

import com.expensechain.backend.model.*;
import com.expensechain.backend.service.DataStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final DataStoreService dataStore;

    public GroupController(DataStoreService dataStore) {
        this.dataStore = dataStore;
    }

    private boolean resolveDemo(Boolean headerDemo, Boolean queryDemo) {
        return Boolean.TRUE.equals(headerDemo) || Boolean.TRUE.equals(queryDemo);
    }

    @GetMapping
    public ResponseEntity<?> getGroups(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                       @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        return ResponseEntity.ok(dataStore.getGroupsForUser(isDemo, currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @RequestBody Map<String, Object> body,
                                         @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                         @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }

        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Group name is required"));
        }
        String description = (String) body.getOrDefault("description", "");
        Long creatorId = currentUser.getId();

        Group group = dataStore.createGroup(isDemo, name.trim(), description, creatorId);
        dataStore.addMember(isDemo, group.getId(), creatorId, "ADMIN");

        if (body.containsKey("memberIdentifiers") && body.get("memberIdentifiers") instanceof List) {
            List<?> identifiers = (List<?>) body.get("memberIdentifiers");
            for (Object item : identifiers) {
                if (item instanceof String) {
                    String ident = (String) item;
                    User u = dataStore.findUserByIdentifier(isDemo, ident);
                    if (u != null && !u.getId().equals(creatorId)) {
                        try {
                            dataStore.addMember(isDemo, group.getId(), u.getId(), "MEMBER");
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        return ResponseEntity.ok(group);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupDetail(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                            @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }

        Group group = dataStore.getGroup(isDemo, id);
        if (group == null) return ResponseEntity.notFound().build();

        // Enforce membership authorization
        if (!dataStore.isUserMemberOfGroup(isDemo, id, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("error", "You are not an authorized member of this group"));
        }

        List<GroupMember> members = dataStore.getGroupMembers(isDemo, id);
        List<Expense> expenses = dataStore.getExpensesForGroup(isDemo, id);
        List<Settlement> settlements = dataStore.getSettlementsForGroup(isDemo, id);
        Map<Long, Double> netBalances = dataStore.calculateNetBalances(isDemo, id);
        List<Map<String, Object>> simplifiedDebts = dataStore.simplifyDebts(isDemo, id);

        Map<String, Object> resp = new HashMap<>();
        resp.put("group", group);
        resp.put("members", members);
        resp.put("expenses", expenses);
        resp.put("settlements", settlements);
        resp.put("netBalances", netBalances);
        resp.put("simplifiedDebts", simplifiedDebts);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                       @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        User currentUser = dataStore.getAuthenticatedUser(authHeader, isDemo);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }

        // Verify group exists
        Group group = dataStore.getGroup(isDemo, id);
        if (group == null) return ResponseEntity.notFound().build();

        // Verify caller is member
        if (!dataStore.isUserMemberOfGroup(isDemo, id, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("error", "Only authorized group members can add participants"));
        }

        // Identifier resolution
        String identifier = null;
        if (body.containsKey("identifier") && body.get("identifier") != null) {
            identifier = String.valueOf(body.get("identifier")).trim();
        } else if (body.containsKey("username") && body.get("username") != null) {
            identifier = String.valueOf(body.get("username")).trim();
        } else if (body.containsKey("email") && body.get("email") != null) {
            identifier = String.valueOf(body.get("email")).trim();
        }

        User targetUser = null;
        if (identifier != null && !identifier.isEmpty()) {
            targetUser = dataStore.findUserByIdentifier(isDemo, identifier);
        } else if (body.containsKey("userId") && body.get("userId") instanceof Number) {
            Long uid = ((Number) body.get("userId")).longValue();
            targetUser = dataStore.getUserById(isDemo, uid);
        }

        if (targetUser == null) {
            String searched = (identifier != null && !identifier.isEmpty()) ? identifier : "specified";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error",
                    "User '" + searched + "' has not registered. They must create an account first."));
        }

        if (targetUser.getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "You are already in this group."));
        }

        if (dataStore.isUserMemberOfGroup(isDemo, id, targetUser.getId())) {
            String nameOrUsername = targetUser.getUsername() != null ? targetUser.getUsername() : targetUser.getName();
            return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                    "User '" + nameOrUsername + "' is already a member of this group."));
        }

        String role = (String) body.getOrDefault("role", "MEMBER");

        try {
            GroupMember member = dataStore.addMember(isDemo, id, targetUser.getId(), role);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
