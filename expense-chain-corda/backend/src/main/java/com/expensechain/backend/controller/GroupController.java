package com.expensechain.backend.controller;

import com.expensechain.backend.model.*;
import com.expensechain.backend.service.DataStoreService;
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
    public ResponseEntity<List<Group>> getGroups(@RequestParam(required = false) Long userId,
                                                 @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                                 @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        if (userId != null) {
            return ResponseEntity.ok(dataStore.getGroupsForUser(isDemo, userId));
        }
        return ResponseEntity.ok(dataStore.getAllGroups(isDemo));
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> body,
                                         @RequestParam(required = false) Long userId,
                                         @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                         @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", "");
        Long creatorId = userId != null ? userId : (body.containsKey("userId") ? ((Number) body.get("userId")).longValue() : 1L);

        Group group = dataStore.createGroup(isDemo, name, description, creatorId);
        dataStore.addMember(isDemo, group.getId(), creatorId, "ADMIN");

        if (body.containsKey("memberIds") && body.get("memberIds") instanceof List) {
            List<?> mIds = (List<?>) body.get("memberIds");
            for (Object mId : mIds) {
                if (mId instanceof Number) {
                    Long mUid = ((Number) mId).longValue();
                    if (!mUid.equals(creatorId)) {
                        try {
                            dataStore.addMember(isDemo, group.getId(), mUid, "MEMBER");
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        return ResponseEntity.ok(group);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupDetail(@PathVariable Long id,
                                            @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                            @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        Group group = dataStore.getGroup(isDemo, id);
        if (group == null) return ResponseEntity.notFound().build();

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
                                       @RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "X-Demo-Mode", required = false) Boolean headerDemo,
                                       @RequestParam(value = "demo", required = false) Boolean queryDemo) {
        boolean isDemo = resolveDemo(headerDemo, queryDemo);
        Long memberUserId = ((Number) body.get("userId")).longValue();
        String role = (String) body.getOrDefault("role", "MEMBER");

        try {
            GroupMember member = dataStore.addMember(isDemo, id, memberUserId, role);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
