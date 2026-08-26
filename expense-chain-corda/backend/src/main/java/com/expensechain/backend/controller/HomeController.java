package com.expensechain.backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class HomeController {

    private String indexHtmlContent = null;

    @GetMapping(value = {"/", "/index.html", "/dashboard", "/groups", "/blockchain", "/profile", "/login", "/register"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> getIndexHtml() {
        try {
            if (indexHtmlContent == null) {
                Resource resource = new ClassPathResource("static/index.html");
                indexHtmlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(indexHtmlContent);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error loading index.html: " + e.getMessage());
        }
    }
}
