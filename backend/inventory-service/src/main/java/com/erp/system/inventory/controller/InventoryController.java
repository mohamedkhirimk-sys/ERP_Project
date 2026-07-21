package com.erp.system.inventory.controller;

import com.erp.system.inventory.dto.StockRequest;
import com.erp.system.inventory.entity.Stock;
import com.erp.system.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/stock")
    public ResponseEntity<Stock> initializeStock(@RequestBody StockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.initializeStock(request));
    }

    @GetMapping("/stock/{sku}")
    public ResponseEntity<Stock> getStockBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getStockBySku(sku));
    }

    @PutMapping("/stock/{sku}")
    public ResponseEntity<Stock> updateStockQuantity(@PathVariable String sku, @RequestParam Integer quantityChange) {
        return ResponseEntity.ok(inventoryService.updateStockQuantity(sku, quantityChange));
    }

    @GetMapping("/stocks")
    public ResponseEntity<List<Stock>> getAllStocks() {
        return ResponseEntity.ok(inventoryService.getAllStocks());
    }
}
