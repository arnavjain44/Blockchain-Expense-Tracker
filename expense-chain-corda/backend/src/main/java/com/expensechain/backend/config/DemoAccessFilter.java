package com.expensechain.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Optional gatekeeper filter for public demos.
 * Activates ONLY when DEMO_ACCESS_CODE environment variable or property is configured.
 * /api/health always bypasses this filter.
 */
@Component
@Order(1)
public class DemoAccessFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DemoAccessFilter.class);
    private static final String COOKIE_NAME = "demo_access_code";
    private static final String HEADER_NAME = "X-Demo-Access-Code";

    @Value("${DEMO_ACCESS_CODE:}")
    private String demoAccessCode;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requiredCode = (demoAccessCode != null && !demoAccessCode.trim().isEmpty())
                ? demoAccessCode.trim()
                : System.getenv("DEMO_ACCESS_CODE");

        // If DEMO_ACCESS_CODE is not set, pass through completely
        if (requiredCode == null || requiredCode.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        requiredCode = requiredCode.trim();
        String uri = request.getRequestURI();

        // 1. Health check and favicon endpoints MUST remain accessible without access code
        if (uri.startsWith("/api/health") || uri.equals("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check X-Demo-Access-Code header
        String headerCode = request.getHeader(HEADER_NAME);
        if (requiredCode.equals(headerCode)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Check Cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName()) && requiredCode.equals(cookie.getValue())) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }

        // 4. Check query parameter: ?accessCode=... or ?demoCode=...
        String queryCode = request.getParameter("accessCode");
        if (queryCode == null) {
            queryCode = request.getParameter("demoCode");
        }
        if (requiredCode.equals(queryCode)) {
            Cookie cookie = new Cookie(COOKIE_NAME, requiredCode);
            cookie.setPath("/");
            cookie.setMaxAge(86400 * 7); // 7 days
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Check HTTP Basic Authentication: Authorization: Basic <base64(user:password)>
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("basic ")) {
            try {
                String base64Credentials = authHeader.substring(6).trim();
                byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String username = values.length > 0 ? values[0] : "";
                String password = values.length > 1 ? values[1] : "";
                if (requiredCode.equals(password) || requiredCode.equals(username)) {
                    Cookie cookie = new Cookie(COOKIE_NAME, requiredCode);
                    cookie.setPath("/");
                    cookie.setMaxAge(86400 * 7);
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                log.debug("Failed to decode Basic auth header: {}", e.getMessage());
            }
        }

        // Prompt for credentials via HTTP Basic Auth
        response.setHeader("WWW-Authenticate", "Basic realm=\"ExpenseChain Public Demo\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Valid DEMO_ACCESS_CODE required to access this instance.\"}");
    }
}
