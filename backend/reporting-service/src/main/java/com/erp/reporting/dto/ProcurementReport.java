package com.erp.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcurementReport {
    private Summary summary;
    private List<PurchaseOrderSummary> pendingOrders;
    private List<VendorActivity> topVendors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalVendors;
        private long totalPurchaseOrders;
        private long pendingPOs;
        private long receivedPOs;
        private BigDecimal totalPoAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderSummary {
        private String poNumber;
        private String vendorName;
        private BigDecimal totalAmount;
        private String status;
        private LocalDate orderedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorActivity {
        private String vendorName;
        private long poCount;
        private BigDecimal totalAmount;
    }
}
