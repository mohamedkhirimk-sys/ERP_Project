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
public class SalesReport {
    private Summary summary;
    private List<OrderSummary> topOrders;
    private List<DailyRevenue> dailyRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalOrders;
        private BigDecimal totalRevenue;
        private BigDecimal averageOrderValue;
        private long paidInvoices;
        private long pendingInvoices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private String orderNumber;
        private String customerName;
        private BigDecimal totalAmount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private long orderCount;
        private BigDecimal revenue;
    }
}
