package com.erp.system.procurement.controller;

import com.erp.system.procurement.dto.GoodsReceivedNoteRequest;
import com.erp.system.procurement.dto.GoodsReceivedNoteResponse;
import com.erp.system.procurement.service.GoodsReceivedNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-received")
@RequiredArgsConstructor
public class GoodsReceivedNoteController {

    private final GoodsReceivedNoteService grnService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GoodsReceivedNoteResponse> createGoodsReceivedNote(@Valid @RequestBody GoodsReceivedNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grnService.createGoodsReceivedNote(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GoodsReceivedNoteResponse>> getAllGoodsReceivedNotes() {
        return ResponseEntity.ok(grnService.getAllGoodsReceivedNotes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GoodsReceivedNoteResponse> getGoodsReceivedNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(grnService.getGoodsReceivedNoteById(id));
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GoodsReceivedNoteResponse>> getNotesByPurchaseOrder(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(grnService.getNotesByPurchaseOrder(purchaseOrderId));
    }
}
