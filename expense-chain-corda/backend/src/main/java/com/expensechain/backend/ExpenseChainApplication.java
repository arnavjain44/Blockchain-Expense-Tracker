package com.expensechain.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.class
})
public class ExpenseChainApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpenseChainApplication.class, args);
    }
}
