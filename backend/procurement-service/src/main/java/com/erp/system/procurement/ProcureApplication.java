package com.erp.system.procurement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
@EnableFeignClients
@EnableMethodSecurity
public class ProcureApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcureApplication.class, args);
    }
}
