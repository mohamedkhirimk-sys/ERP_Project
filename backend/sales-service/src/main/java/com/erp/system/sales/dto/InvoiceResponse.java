package com.erp.system.sales.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime dueDate;
}
