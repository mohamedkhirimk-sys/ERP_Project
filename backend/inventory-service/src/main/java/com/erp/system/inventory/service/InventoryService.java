package com.erp.system.inventory.service;

import com.erp.system.inventory.dto.StockRequest;
import com.erp.system.inventory.entity.Stock;
import java.util.List;

public interface InventoryService {
    Stock initializeStock(StockRequest request);
    Stock getStockBySku(String sku);
    Stock updateStockQuantity(String sku, Integer quantityChange);
    List<Stock> getAllStocks();
}