package com.erp.system.procurement.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime orderedAt;
}
