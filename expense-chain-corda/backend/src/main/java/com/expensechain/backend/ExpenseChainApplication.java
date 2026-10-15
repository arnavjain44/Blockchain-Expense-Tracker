package com.expensechain.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.class
})
public class ExpenseChainApplication {
    public static void main(String[] args) {
        loadDotEnvIfExists();
        SpringApplication.run(ExpenseChainApplication.class, args);
    }

    private static void loadDotEnvIfExists() {
        String[] potentialPaths = new String[] {
            ".env",
            "backend/.env",
            "../.env",
            "../../.env"
        };
        for (String p : potentialPaths) {
            java.io.File file = new java.io.File(p);
            if (file.exists() && file.isFile()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String key = line.substring(0, idx).trim();
                            String val = line.substring(idx + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
