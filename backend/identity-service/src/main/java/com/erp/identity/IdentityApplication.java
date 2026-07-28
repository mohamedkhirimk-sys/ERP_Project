package com.erp.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
@EnableMethodSecurity
public class IdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
