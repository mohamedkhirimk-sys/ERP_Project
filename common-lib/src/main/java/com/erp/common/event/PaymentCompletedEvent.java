package com.erp.common.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private String orderNumber;
    private java.math.BigDecimal amount;
    private String paymentStatus;
}