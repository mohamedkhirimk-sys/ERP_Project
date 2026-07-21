package com.erp.system.inventory.service.impl;

import com.erp.system.inventory.dto.StockRequest;
import com.erp.system.inventory.entity.Stock;
import com.erp.system.inventory.repository.StockRepository;
import com.erp.system.inventory.service.InventoryService;
import lombok.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final StockRepository stockRepository;

    @Override
    public Stock initializeStock(StockRequest request) {
        if (stockRepository.findByProductSku(request.getProductSku()).isPresent()) {
            throw new RuntimeException("Stock already exists for SKU: " + request.getProductSku());
        }

        Stock stock = Stock.builder()
                .productSku(request.getProductSku())
                .quantity(request.getQuantity())
                .warehouseLocation(request.getWarehouseLocation() != null ? request.getWarehouseLocation() : "Default")
                .build();

        return stockRepository.save(stock);
    }

    @Override
    public Stock updateStockQuantity(String productSku, Integer quantityChange) {
        Stock stock = stockRepository.findByProductSku(productSku)
                .orElseThrow(() -> new RuntimeException("Stock not found for SKU: " + productSku));

        int newQuantity = stock.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock for SKU: " + productSku);
        }

        stock.setQuantity(newQuantity);
        return stockRepository.save(stock);
    }

    @Override
    public Stock getStockBySku(String productSku) {
        return stockRepository.findByProductSku(productSku)
                .orElseThrow(() -> new RuntimeException("Stock not found for SKU: " + productSku));
    }

    @Override
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }
}