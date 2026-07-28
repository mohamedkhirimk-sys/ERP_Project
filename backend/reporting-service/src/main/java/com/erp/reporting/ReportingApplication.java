package com.erp.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
public class ReportingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingApplication.class, args);
    }
}
