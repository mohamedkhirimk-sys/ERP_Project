package com.erp.system.payment.controller;

import lombok.Data;

@Data
public class WebhookRequest {
    private String orderId;
    private String status;
}
