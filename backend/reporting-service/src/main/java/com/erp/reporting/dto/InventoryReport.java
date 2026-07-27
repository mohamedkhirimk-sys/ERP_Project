package com.erp.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReport {
    private Summary summary;
    private List<StockItem> stockItems;
    private List<StockItem> lowStockItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalProducts;
        private long totalStockItems;
        private long lowStockCount;
        private long outOfStockCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private String sku;
        private String productName;
        private int quantity;
        private String warehouseLocation;
    }
}
