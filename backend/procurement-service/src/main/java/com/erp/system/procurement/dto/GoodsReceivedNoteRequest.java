package com.erp.system.procurement.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceivedNoteRequest {
    @NotNull(message = "Purchase Order ID is required")
    private Long purchaseOrderId;

    @NotBlank(message = "Status is required")
    private String status;

    @NotEmpty(message = "At least one item is required")
    private List<ItemRequest> items;
}
