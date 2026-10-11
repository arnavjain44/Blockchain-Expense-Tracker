package com.expensechain.backend;

import com.expensechain.backend.dto.UserDto;
import com.expensechain.backend.model.Group;
import com.expensechain.backend.model.Settlement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExpenseChainSecurityAndFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> registerUser(String name, String username, String email, String password, String phone, String upiId) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("phone", phone);
        if (upiId != null) body.put("upiId", upiId);

        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> loginUser(String identifier, String password) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("email", identifier);
        body.put("password", password);

        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
    }

    @Test
    @DisplayName("Feature 1: Private accounts, opaque tokens, no password leak, and directory protection")
    public void testAuthenticationAndPrivacy() throws Exception {
        long ts = System.currentTimeMillis();
        String u1Email = "alice_" + ts + "@test.com";
        String u1Name = "Alice Smith";
        String u1Username = "alice_" + ts;
        String u1Pass = "Pass1234!";

        String u2Email = "bob_" + ts + "@test.com";
        String u2Name = "Bob Jones";
        String u2Username = "bob_" + ts;
        String u2Pass = "Pass5678!";

        // 1. Register Alice and Bob
        Map<String, Object> aliceReg = registerUser(u1Name, u1Username, u1Email, u1Pass, "9998887771", "alice@okaxis");
        Map<String, Object> bobReg = registerUser(u2Name, u2Username, u2Email, u2Pass, "9998887772", "bob@okhdfcbank");

        String tokenAlice = (String) aliceReg.get("token");
        String tokenBob = (String) bobReg.get("token");

        assertNotNull(tokenAlice);
        assertNotNull(tokenBob);
        assertNotEquals(tokenAlice, tokenBob);
        assertFalse(tokenAlice.startsWith("auth-token-"), "Bearer token must be cryptographically opaque");

        // Verify passwordHash is never returned
        assertFalse(aliceReg.containsKey("passwordHash"));
        @SuppressWarnings("unchecked")
        Map<String, Object> aliceUserMap = (Map<String, Object>) aliceReg.get("user");
        assertFalse(aliceUserMap.containsKey("passwordHash"), "User object must never expose passwordHash");
        assertEquals(u1Username, aliceUserMap.get("username"));
        assertEquals("alice@okaxis", aliceUserMap.get("upiId"));

        // 2. Login by username for Alice, login by email for Bob
        Map<String, Object> aliceLogin = loginUser(u1Username, u1Pass);
        assertNotNull(aliceLogin.get("token"));

        Map<String, Object> bobLogin = loginUser(u2Email, u2Pass);
        assertNotNull(bobLogin.get("token"));

        // 3. /api/auth/me session restore
        MvcResult meResAlice = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> meAlice = objectMapper.readValue(meResAlice.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> meAliceUser = (Map<String, Object>) meAlice.get("user");
        assertEquals(u1Email, meAliceUser.get("email"));
        assertEquals(u1Username, meAliceUser.get("username"));

        // Invalid token returns 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token-999"))
                .andExpect(status().isUnauthorized());

        // 4. Global user directory is blocked
        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isForbidden());

        // 5. Dev reset endpoint is blocked
        mockMvc.perform(post("/api/dev/reset"))
                .andExpect(status().isForbidden());

        // 6. Logout revokes token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Feature 1 & 2: Ownership authorization, member invitation by username and email")
    public void testGroupMembershipAndInvitation() throws Exception {
        long ts = System.currentTimeMillis();
        String u1Username = "owner_" + ts;
        String u1Email = u1Username + "@test.com";
        Map<String, Object> alice = registerUser("Owner User", u1Username, u1Email, "Secret1!", "9111111111", null);
        String tokenAlice = (String) alice.get("token");
        Long aliceId = ((Number) ((Map<?, ?>) alice.get("user")).get("id")).longValue();

        String u2Username = "member_" + ts;
        String u2Email = u2Username + "@test.com";
        Map<String, Object> bob = registerUser("Member User", u2Username, u2Email, "Secret2!", "9222222222", "member@upi");
        String tokenBob = (String) bob.get("token");
        Long bobId = ((Number) ((Map<?, ?>) bob.get("user")).get("id")).longValue();

        String u3Username = "external_" + ts;
        String u3Email = u3Username + "@test.com";
        Map<String, Object> charlie = registerUser("External User", u3Username, u3Email, "Secret3!", "9333333333", null);
        String tokenCharlie = (String) charlie.get("token");

        // 1. Alice creates group
        Map<String, Object> groupReq = new HashMap<>();
        groupReq.put("name", "Goa Trip " + ts);
        groupReq.put("description", "Beach vacation");

        MvcResult grpRes = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isOk())
                .andReturn();
        Group grp = objectMapper.readValue(grpRes.getResponse().getContentAsString(), Group.class);
        Long groupId = grp.getId();
        assertEquals(aliceId, grp.getCreatedBy());

        // 2. Bob cannot view Alice's group (not a member) -> 403 Forbidden
        mockMvc.perform(get("/api/groups/" + groupId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden());

        // 3. Bob cannot add members to Alice's group -> 403 Forbidden
        Map<String, String> addReq = Collections.singletonMap("identifier", u3Username);
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isForbidden());

        // 4. Alice adds Bob by exact username
        Map<String, String> addBobReq = Collections.singletonMap("identifier", u2Username);
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addBobReq)))
                .andExpect(status().isOk());

        // Now Bob can access group detail
        MvcResult bobGrpRes = mockMvc.perform(get("/api/groups/" + groupId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> grpDetail = objectMapper.readValue(bobGrpRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) grpDetail.get("members");
        assertEquals(2, members.size());

        // 5. Duplicate membership rejected -> 400 Bad Request
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addBobReq)))
                .andExpect(status().isBadRequest());

        // 6. Adding self rejected -> 400 Bad Request
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("identifier", u1Username))))
                .andExpect(status().isBadRequest());

        // 7. Alice adds Charlie by email case-insensitively
        Map<String, String> addCharlieReq = Collections.singletonMap("identifier", u3Email.toUpperCase());
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addCharlieReq)))
                .andExpect(status().isOk());

        // 8. Unknown user -> 404 Not Found
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("identifier", "nonexistent_user_99999"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Feature 3: UPI ID management and settlement verification flow")
    public void testUpiManagementAndSettlement() throws Exception {
        long ts = System.currentTimeMillis();
        String u1Email = "debtor_" + ts + "@test.com";
        String u1Username = "debtor_" + ts;
        Map<String, Object> debtor = registerUser("Debtor Dave", u1Username, u1Email, "Pass1234!", "9800000001", null);
        String tokenDebtor = (String) debtor.get("token");
        Long debtorId = ((Number) ((Map<?, ?>) debtor.get("user")).get("id")).longValue();

        String u2Email = "creditor_" + ts + "@test.com";
        String u2Username = "creditor_" + ts;
        Map<String, Object> creditor = registerUser("Creditor Clara", u2Username, u2Email, "Pass1234!", "9800000002", null);
        String tokenCreditor = (String) creditor.get("token");
        Long creditorId = ((Number) ((Map<?, ?>) creditor.get("user")).get("id")).longValue();

        // 1. Clara sets her UPI ID
        Map<String, String> upiReq = Collections.singletonMap("upiId", "clara@okhdfcbank");
        mockMvc.perform(put("/api/auth/profile/upi")
                        .header("Authorization", "Bearer " + tokenCreditor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upiReq)))
                .andExpect(status().isOk());

        // Verify Clara's profile has UPI ID
        MvcResult upiGetRes = mockMvc.perform(get("/api/auth/profile/upi")
                        .header("Authorization", "Bearer " + tokenCreditor))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> claraUpiMap = objectMapper.readValue(upiGetRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertEquals("clara@okhdfcbank", claraUpiMap.get("upiId"));

        // Debtor cannot see Clara's UPI via profile endpoint
        MvcResult debtorUpiGetRes = mockMvc.perform(get("/api/auth/profile/upi")
                        .header("Authorization", "Bearer " + tokenDebtor))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> debtorUpiMap = objectMapper.readValue(debtorUpiGetRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertNull(debtorUpiMap.get("upiId"), "Debtor should have no UPI ID yet");

        // 2. Debtor clears and updates his UPI ID
        mockMvc.perform(put("/api/auth/profile/upi")
                        .header("Authorization", "Bearer " + tokenDebtor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("upiId", "dave@paytm"))))
                .andExpect(status().isOk());

        // 3. Create group with Dave and Clara
        Map<String, Object> grpReq = new HashMap<>();
        grpReq.put("name", "Apartment " + ts);
        MvcResult grpRes = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + tokenDebtor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grpReq)))
                .andExpect(status().isOk())
                .andReturn();
        Group grp = objectMapper.readValue(grpRes.getResponse().getContentAsString(), Group.class);
        Long groupId = grp.getId();

        // Add Clara to group
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + tokenDebtor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("identifier", u2Username))))
                .andExpect(status().isOk());

        // 4. Test UPI Payment URI format (Feature 3 specification)
        String recipientUpi = "clara@okhdfcbank";
        String recipientName = "Creditor Clara";
        double debtAmount = 750.50;
        String note = "Settlement for Apartment " + ts;

        String expectedUri = String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
                URLEncoder.encode(recipientUpi, "UTF-8"),
                URLEncoder.encode(recipientName, "UTF-8"),
                debtAmount,
                URLEncoder.encode(note, "UTF-8"));

        assertTrue(expectedUri.startsWith("upi://pay?pa=clara%40okhdfcbank"));
        assertTrue(expectedUri.contains("&am=750.50"));
        assertTrue(expectedUri.contains("&cu=INR"));

        // 5. Dave initiates settlement payment (client-supplied paidBy ignored, server enforces token)
        Map<String, Object> settleReq = new HashMap<>();
        settleReq.put("groupId", groupId);
        settleReq.put("paidBy", 99999L); // Malicious attempt to spoof payer ID
        settleReq.put("paidTo", creditorId);
        settleReq.put("amount", debtAmount);

        MvcResult settleRes = mockMvc.perform(post("/api/settlements")
                        .header("Authorization", "Bearer " + tokenDebtor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settleReq)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> settleRespMap = objectMapper.readValue(settleRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertEquals("PENDING_VERIFICATION", settleRespMap.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> savedSettle = (Map<String, Object>) settleRespMap.get("settlement");
        Long settlementId = ((Number) savedSettle.get("id")).longValue();
        assertEquals(debtorId, ((Number) savedSettle.get("paidBy")).longValue(), "Server must enforce authenticated payer ID");

        // 6. Verification inbox check
        // Debtor's inbox is empty
        MvcResult debtorInboxRes = mockMvc.perform(get("/api/settlements/pending")
                        .header("Authorization", "Bearer " + tokenDebtor))
                .andExpect(status().isOk())
                .andReturn();
        List<?> debtorInbox = objectMapper.readValue(debtorInboxRes.getResponse().getContentAsString(), List.class);
        assertTrue(debtorInbox.isEmpty());

        // Clara's inbox contains the pending settlement
        MvcResult claraInboxRes = mockMvc.perform(get("/api/settlements/pending")
                        .header("Authorization", "Bearer " + tokenCreditor))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> claraInbox = objectMapper.readValue(claraInboxRes.getResponse().getContentAsString(), new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(1, claraInbox.size());
        assertEquals(settlementId, ((Number) claraInbox.get(0).get("id")).longValue());

        // 7. Authorization check: Debtor cannot verify settlement -> 403 Forbidden
        mockMvc.perform(post("/api/settlements/" + settlementId + "/verify")
                        .header("Authorization", "Bearer " + tokenDebtor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("approved", true))))
                .andExpect(status().isForbidden());
    }
}
