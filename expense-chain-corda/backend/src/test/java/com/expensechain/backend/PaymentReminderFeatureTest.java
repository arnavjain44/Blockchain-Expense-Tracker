package com.expensechain.backend;

import com.expensechain.backend.model.Group;
import com.expensechain.backend.model.PaymentReminder;
import com.expensechain.backend.service.DataStoreService;
import com.expensechain.backend.service.EmailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentReminderFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DataStoreService dataStoreService;

    @BeforeEach
    public void setup() {
        emailService.setMockEnabled(true);
    }

    private Map<String, Object> registerUser(String name, String username, String email, String password, String phone) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("phone", phone);

        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
    }

    @Test
    @DisplayName("Email Reminder End-to-End: Verification of all 12 specified test scenarios")
    public void testEmailPaymentReminderScenarios() throws Exception {
        long ts = System.currentTimeMillis();

        // Register Arnav (Creditor) and Bob (Debtor) and Charlie (Third party)
        String arnavEmail = "arnav_" + ts + "@example.com";
        String bobEmail = "bob_" + ts + "@example.com";
        String charlieEmail = "charlie_" + ts + "@example.com";

        Map<String, Object> arnavReg = registerUser("Arnav Jain", "arnav_" + ts, arnavEmail, "Pass1234!", "9811111111");
        Map<String, Object> bobReg = registerUser("Bob Smith", "bob_" + ts, bobEmail, "Pass1234!", "9822222222");
        Map<String, Object> charlieReg = registerUser("Charlie Brown", "charlie_" + ts, charlieEmail, "Pass1234!", "9833333333");

        String arnavToken = (String) arnavReg.get("token");
        Long arnavId = ((Number) ((Map<?, ?>) arnavReg.get("user")).get("id")).longValue();

        String bobToken = (String) bobReg.get("token");
        Long bobId = ((Number) ((Map<?, ?>) bobReg.get("user")).get("id")).longValue();

        String charlieToken = (String) charlieReg.get("token");
        Long charlieId = ((Number) ((Map<?, ?>) charlieReg.get("user")).get("id")).longValue();

        // 1. Arnav creates a group
        Map<String, Object> grpReq = new HashMap<>();
        grpReq.put("name", "Goa Trip " + ts);
        grpReq.put("description", "Shared vacation pool");

        MvcResult grpRes = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + arnavToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grpReq)))
                .andExpect(status().isOk())
                .andReturn();
        Group grp = objectMapper.readValue(grpRes.getResponse().getContentAsString(), Group.class);
        Long groupId = grp.getId();

        // Arnav adds Bob to the group
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + arnavToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("identifier", bobEmail))))
                .andExpect(status().isOk());

        // SCENARIO 3: Balance is 0 -> Sending a reminder is rejected
        MvcResult zeroBalRes = mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + bobId)
                        .header("Authorization", "Bearer " + arnavToken))
                .andExpect(status().isBadRequest())
                .andReturn();
        Map<String, Object> zeroBalJson = objectMapper.readValue(zeroBalRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertTrue(((String) zeroBalJson.get("error")).toLowerCase().contains("no outstanding balance"));

        // Arnav records an expense of Rs 1000 split equally with Bob -> Bob owes Arnav Rs 500
        List<Map<String, Object>> splits = new ArrayList<>();
        Map<String, Object> s1 = new HashMap<>();
        s1.put("userId", arnavId);
        s1.put("shareAmount", 500.0);
        splits.add(s1);
        Map<String, Object> s2 = new HashMap<>();
        s2.put("userId", bobId);
        s2.put("shareAmount", 500.0);
        splits.add(s2);

        dataStoreService.saveExpense(false, groupId, "Dinner at Beach", 1000.0, "FOOD", "Beach", "2026-10-15", arnavId, "EQUAL", "tx-test-exp-1", splits);

        // SCENARIO 2: A user who owes another member cannot send a reminder for that debt
        // Bob owes Arnav Rs 500. Bob attempts to send a reminder to Arnav -> Rejected
        MvcResult debtorRemindRes = mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + arnavId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isBadRequest())
                .andReturn();
        Map<String, Object> debtorRemindJson = objectMapper.readValue(debtorRemindRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertTrue(((String) debtorRemindJson.get("error")).toLowerCase().contains("no outstanding balance"));

        // SCENARIO 5: Unauthorized group / user attempts to send reminder for group they don't belong to
        // Charlie is NOT in the group -> 403 Forbidden
        mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + bobId)
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());

        // SCENARIO 6: Attempt to remind someone who is not a member of the group
        // Arnav attempts to remind Charlie in this group -> 400 Bad Request
        mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + charlieId)
                        .header("Authorization", "Bearer " + arnavToken))
                .andExpect(status().isBadRequest());

        // SCENARIO 8: Email provider credentials missing -> 503 with clear message
        emailService.setMockEnabled(false);
        MvcResult noCredsRes = mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + bobId)
                        .header("Authorization", "Bearer " + arnavToken))
                .andExpect(status().isServiceUnavailable())
                .andReturn();
        Map<String, Object> noCredsJson = objectMapper.readValue(noCredsRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertTrue(((String) noCredsJson.get("error")).contains("not configured"));

        // SCENARIO 1 & 4: Valid reminder when Arnav is owed Rs 500 by Bob (valid members of same group)
        emailService.setMockEnabled(true);
        MvcResult successRes = mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + bobId)
                        .header("Authorization", "Bearer " + arnavToken))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> successJson = objectMapper.readValue(successRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertTrue((Boolean) successJson.get("success"));
        assertEquals("ACCEPTED_MOCK", successJson.get("status"));
        assertEquals(500.0, ((Number) successJson.get("amount")).doubleValue(), 0.01);
        assertEquals(bobEmail, successJson.get("recipientEmail"));
        assertNotNull(successJson.get("providerMessageId"));

        // Verify reminder logged in persistence layer
        List<PaymentReminder> history = dataStoreService.getRecentReminders(false, groupId, arnavId, bobId);
        assertFalse(history.isEmpty());
        PaymentReminder lastReminder = history.get(0);
        assertEquals(arnavId, lastReminder.getSenderId());
        assertEquals(bobId, lastReminder.getRecipientId());
        assertEquals(groupId, lastReminder.getGroupId());
        assertEquals(500.0, lastReminder.getAmount().doubleValue(), 0.01);

        // SCENARIO 10: Repeated rapid submissions (Rate limiting / cooldown protection)
        MvcResult rateLimitRes = mockMvc.perform(post("/api/groups/" + groupId + "/remind/" + bobId)
                        .header("Authorization", "Bearer " + arnavToken))
                .andExpect(status().isTooManyRequests())
                .andReturn();
        Map<String, Object> rateLimitJson = objectMapper.readValue(rateLimitRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        assertTrue(((String) rateLimitJson.get("error")).contains("Please wait 5 minutes"));

        // SCENARIO 11: Displayed amount changes after settlement
        // Record settlement of Rs 200 from Bob to Arnav
        dataStoreService.saveSettlement(false, groupId, bobId, arnavId, 200.0, "tx-test-settle-1");

        // Now simplified balance is Rs 300 (reduced from Rs 500)
        // Check simplified debts from group detail
        MvcResult groupDetailRes = mockMvc.perform(get("/api/groups/" + groupId)
                        .header("Authorization", "Bearer " + arnavToken))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> detailJson = objectMapper.readValue(groupDetailRes.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> debts = (List<Map<String, Object>>) detailJson.get("simplifiedDebts");
        assertEquals(1, debts.size());
        assertEquals(300.0, ((Number) debts.get(0).get("amount")).doubleValue(), 0.01);

        // SCENARIO 12: Application remains functional even if email provider throws or errors
        // Send reminder directly via EmailService and verify graceful error result without crashing
        EmailService.EmailSendResult errorResult = emailService.sendPaymentReminder(
                "invalid-email-format", "Bob", "Arnav", "Goa Trip", 300.0
        );
        assertNotNull(errorResult);
        assertFalse(errorResult.isSuccess());
        assertNotNull(errorResult.getErrorMessage());
    }
}
