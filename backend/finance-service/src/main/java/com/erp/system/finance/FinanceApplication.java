package com.erp.system.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
@EnableMethodSecurity
public class FinanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
