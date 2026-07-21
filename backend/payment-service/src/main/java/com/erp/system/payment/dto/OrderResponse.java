package com.erp.system.payment.dto;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private String status;
    private Double amount;
    private String customerId;
}