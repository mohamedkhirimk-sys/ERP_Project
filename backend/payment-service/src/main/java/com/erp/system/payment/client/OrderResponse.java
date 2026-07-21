package com.erp.system.payment.client;

import lombok.Data;

@Data
public class OrderResponse {
    private String orderId;
    private String status;
}

