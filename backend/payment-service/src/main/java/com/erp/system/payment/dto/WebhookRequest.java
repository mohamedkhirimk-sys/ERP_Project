package com.erp.system.payment.dto;

import lombok.*;

@Data
public class WebhookRequest {
    private String orderId;
    private String status;

    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
}


