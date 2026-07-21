package com.erp.system.payment.client;

import lombok.Data;

public interface OrderClient {
    OrderResponse getOrderById(String orderId);

    @Data
    class OrderResponse {
        private Long id;
        private String status;
        private Double amount;
        private String customerId;
    }
}
