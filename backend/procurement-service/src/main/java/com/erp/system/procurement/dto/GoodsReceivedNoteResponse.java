package com.erp.system.procurement.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceivedNoteResponse {
    private Long id;
    private String grnNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private String status;
    private LocalDateTime receivedAt;
}
