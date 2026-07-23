package com.erp.system.procurement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ProcureApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcureApplication.class, args);
    }
}
