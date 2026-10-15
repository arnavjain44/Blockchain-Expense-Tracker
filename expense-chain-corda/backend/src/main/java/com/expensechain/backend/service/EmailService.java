package com.expensechain.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.AuthenticationFailedException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${EMAIL_HOST:${spring.mail.host:smtp.gmail.com}}")
    private String emailHost;

    @Value("${EMAIL_PORT:${spring.mail.port:587}}")
    private String emailPort;

    @Value("${EMAIL_USER:${spring.mail.username:}}")
    private String emailUser;

    @Value("${EMAIL_APP_PASSWORD:${spring.mail.password:}}")
    private String emailAppPassword;

    @Value("${EMAIL_FROM:${spring.mail.from:}}")
    private String emailFrom;

    @Value("${EMAIL_MOCK_ENABLED:${email.mock.enabled:false}}")
    private boolean mockEnabled;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    public static class EmailSendResult {
        private final boolean success;
        private final String status;
        private final String providerMessageId;
        private final String errorMessage;

        public EmailSendResult(boolean success, String status, String providerMessageId, String errorMessage) {
            this.success = success;
            this.status = status;
            this.providerMessageId = providerMessageId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public String getProviderMessageId() { return providerMessageId; }
        public String getErrorMessage() { return errorMessage; }
    }

    public boolean isConfigured() {
        if (mockEnabled) return true;
        String user = getEffectiveUser();
        String pass = getEffectivePassword();
        return user != null && !user.trim().isEmpty() &&
               pass != null && !pass.trim().isEmpty();
    }

    public void setMockEnabled(boolean enabled) {
        this.mockEnabled = enabled;
    }

    public String getEffectiveHost() {
        return (emailHost != null && !emailHost.trim().isEmpty()) ? emailHost.trim() : "smtp.gmail.com";
    }

    public int getEffectivePort() {
        try {
            if (emailPort != null && !emailPort.trim().isEmpty()) {
                return Integer.parseInt(emailPort.trim());
            }
        } catch (NumberFormatException ignored) {}
        return 587;
    }

    public String getEffectiveUser() {
        return (emailUser != null && !emailUser.trim().isEmpty()) ? emailUser.trim() : null;
    }

    public String getEffectivePassword() {
        if (emailAppPassword == null) return null;
        String pass = emailAppPassword.trim();
        if (pass.isEmpty()) return null;
        // Strip any spaces from Google App Password (e.g. "azga jcxn rxfd ecyp" -> "azgajcxnrxfdecyp")
        return pass.replace(" ", "");
    }

    public String getEffectiveFrom() {
        if (emailFrom != null && !emailFrom.trim().isEmpty()) {
            return emailFrom.trim();
        }
        String user = getEffectiveUser();
        if (user != null && !user.trim().isEmpty()) {
            return user;
        }
        return "Blockchain Expense Tracker <noreply@expensechain.com>";
    }

    public synchronized JavaMailSender getMailSender() {
        if (javaMailSender instanceof JavaMailSenderImpl) {
            JavaMailSenderImpl impl = (JavaMailSenderImpl) javaMailSender;
            impl.setHost(getEffectiveHost());
            impl.setPort(getEffectivePort());
            impl.setUsername(getEffectiveUser());
            impl.setPassword(getEffectivePassword());

            Properties props = impl.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", getEffectiveHost());
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "10000");
            return impl;
        }

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(getEffectiveHost());
        impl.setPort(getEffectivePort());
        impl.setUsername(getEffectiveUser());
        impl.setPassword(getEffectivePassword());

        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", getEffectiveHost());
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "10000");
        return impl;
    }

    public EmailSendResult sendPaymentReminder(String recipientEmail,
                                              String recipientName,
                                              String senderName,
                                              String groupName,
                                              double amount) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || !recipientEmail.contains("@")) {
            return new EmailSendResult(false, "FAILED", null, "Recipient does not have a valid registered email address.");
        }

        String formattedAmount = String.format("₹%.2f", amount);
        String subject = "Payment Reminder: " + formattedAmount + " owed to " + sanitizeHeader(senderName);

        String htmlBody = buildHtmlEmail(recipientName, senderName, groupName, formattedAmount);
        String textBody = buildTextEmail(recipientName, senderName, groupName, formattedAmount);

        // 1. Mock / Test Mode
        if (mockEnabled) {
            String mockId = "mock-msg-" + System.currentTimeMillis();
            log.info("[MOCK EMAIL] Payment reminder simulated for {} <{}>. Amount: {}. Message ID: {}",
                    recipientName, recipientEmail, formattedAmount, mockId);
            return new EmailSendResult(true, "ACCEPTED_MOCK", mockId, null);
        }

        // 2. Missing Configuration Check
        if (!isConfigured()) {
            String errorMsg = "Email service is not configured. Please set EMAIL_HOST, EMAIL_PORT, EMAIL_USER, and EMAIL_APP_PASSWORD in your environment to enable real email notifications.";
            log.warn("Cannot send email to {}: {}", recipientEmail, errorMsg);
            return new EmailSendResult(false, "FAILED", null, errorMsg);
        }

        // 3. Gmail SMTP Delivery
        return sendViaSmtp(recipientEmail, subject, htmlBody, textBody);
    }

    private EmailSendResult sendViaSmtp(String recipientEmail, String subject, String htmlBody, String textBody) {
        try {
            JavaMailSender sender = getMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = getEffectiveFrom();
            if (from.contains("<") && from.contains(">")) {
                helper.setFrom(from);
            } else {
                helper.setFrom(new InternetAddress(from, "Blockchain Expense Tracker", "UTF-8").toString());
            }

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            sender.send(message);
            String msgId = message.getMessageID() != null ? message.getMessageID() : "smtp-" + System.currentTimeMillis();
            log.info("Payment reminder email accepted and sent via Gmail SMTP to {}. Message ID: {}", recipientEmail, msgId);
            return new EmailSendResult(true, "SENT", msgId, null);
        } catch (AuthenticationFailedException afe) {
            log.error("Gmail SMTP authentication failed: {}", afe.getMessage());
            return new EmailSendResult(false, "FAILED", null, "Gmail SMTP authentication failed. Please verify your EMAIL_USER and EMAIL_APP_PASSWORD.");
        } catch (Exception e) {
            log.error("Failed to deliver payment reminder email via SMTP: {}", e.getMessage(), e);
            return new EmailSendResult(false, "FAILED", null, "Failed to deliver email through SMTP server: " + e.getMessage());
        }
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String sanitizeHeader(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\r\\n]+", " ").trim();
    }

    private String buildHtmlEmail(String recipientName, String senderName, String groupName, String formattedAmount) {
        String safeRecipient = escapeHtml(recipientName);
        String safeSender = escapeHtml(senderName);
        String safeGroup = escapeHtml(groupName);
        String safeAmount = escapeHtml(formattedAmount);

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Payment Reminder</title>\n" +
                "  <style>\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; margin: 0; padding: 24px; color: #e2e8f0; }\n" +
                "    .card { max-width: 540px; margin: 0 auto; background: #161e2e; border-radius: 12px; border: 1px solid #2d3748; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }\n" +
                "    .header { border-bottom: 2px solid #5372b8; padding-bottom: 16px; margin-bottom: 22px; }\n" +
                "    .title { font-size: 20px; font-weight: 800; color: #ffffff; margin: 0 0 4px 0; letter-spacing: -0.3px; }\n" +
                "    .subtitle { font-size: 13px; color: #94a3b8; margin: 0; }\n" +
                "    .content { font-size: 15px; line-height: 1.6; color: #cbd5e1; }\n" +
                "    .amount-box { background: rgba(179, 80, 75, 0.12); border: 1px solid rgba(179, 80, 75, 0.35); border-left: 4px solid #b3504b; border-radius: 8px; padding: 16px 20px; margin: 22px 0; }\n" +
                "    .amount-label { font-size: 12px; font-weight: 700; color: #f28b82; text-transform: uppercase; letter-spacing: 0.6px; }\n" +
                "    .amount-val { font-size: 26px; font-weight: 800; color: #ff6b6b; margin-top: 4px; }\n" +
                "    .footer { margin-top: 30px; padding-top: 16px; border-top: 1px solid #2d3748; font-size: 12px; color: #64748b; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"card\">\n" +
                "    <div class=\"header\">\n" +
                "      <div class=\"title\">Blockchain Expense Tracker</div>\n" +
                "      <div class=\"subtitle\">Multi-Party Shared Expense Settlement</div>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <p>Hi <strong>" + safeRecipient + "</strong>,</p>\n" +
                "      <p>This is a friendly reminder that you have an outstanding balance of <strong>" + safeAmount + "</strong> owed to <strong>" + safeSender + "</strong> in the <strong>\"" + safeGroup + "\"</strong> expense group on Blockchain Expense Tracker.</p>\n" +
                "      <div class=\"amount-box\">\n" +
                "        <div class=\"amount-label\">Outstanding Balance</div>\n" +
                "        <div class=\"amount-val\">" + safeAmount + "</div>\n" +
                "      </div>\n" +
                "      <p>When convenient, please review the outstanding amount and arrange the payment.</p>\n" +
                "      <p style=\"margin-top: 24px;\">Thank you!<br><strong>" + safeSender + "</strong><br><span style=\"color:#94a3b8;\">Blockchain Expense Tracker</span></p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      This payment reminder was requested directly by " + safeSender + " via Blockchain Expense Tracker for group \"" + safeGroup + "\".\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String buildTextEmail(String recipientName, String senderName, String groupName, String formattedAmount) {
        return "Hi " + recipientName + ",\n\n" +
                "This is a friendly reminder that you have an outstanding balance of " + formattedAmount + " owed to " + senderName + " in the \"" + groupName + "\" expense group on Blockchain Expense Tracker.\n\n" +
                "When convenient, please review the outstanding amount and arrange the payment.\n\n" +
                "Thank you!\n" +
                senderName + "\n" +
                "Blockchain Expense Tracker\n";
    }
}
