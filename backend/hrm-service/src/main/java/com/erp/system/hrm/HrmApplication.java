package com.erp.system.hrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
@EnableMethodSecurity
public class HrmApplication {
    public static void main(String[] args) {
        SpringApplication.run(HrmApplication.class, args);
    }
}
