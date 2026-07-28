package com.erp.system.inventory.service;

import com.erp.system.inventory.dto.StockRequest;
import com.erp.system.inventory.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    Stock initializeStock(StockRequest request);
    Stock getStockBySku(String sku);
    Stock updateStockQuantity(String sku, Integer quantityChange);
    Page<Stock> getAllStocks(Pageable pageable);
}