package com.erp.system.procurement.controller;

import com.erp.system.procurement.dto.PurchaseOrderRequest;
import com.erp.system.procurement.dto.PurchaseOrderResponse;
import com.erp.system.procurement.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.createPurchaseOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<PurchaseOrderResponse>> getPurchaseOrdersByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByVendor(vendorId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponse> updatePurchaseOrderStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(purchaseOrderService.updatePurchaseOrderStatus(id, status));
    }
}
