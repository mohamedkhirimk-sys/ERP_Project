package com.erp.system.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime createdAt;
}
