package com.erp.system.procurement.controller;

import com.erp.system.procurement.dto.GoodsReceivedNoteRequest;
import com.erp.system.procurement.dto.GoodsReceivedNoteResponse;
import com.erp.system.procurement.service.GoodsReceivedNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-received")
@RequiredArgsConstructor
public class GoodsReceivedNoteController {

    private final GoodsReceivedNoteService grnService;

    @PostMapping
    public ResponseEntity<GoodsReceivedNoteResponse> createGoodsReceivedNote(@Valid @RequestBody GoodsReceivedNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grnService.createGoodsReceivedNote(request));
    }

    @GetMapping
    public ResponseEntity<Page<GoodsReceivedNoteResponse>> getAllGoodsReceivedNotes(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(grnService.getAllGoodsReceivedNotes(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceivedNoteResponse> getGoodsReceivedNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(grnService.getGoodsReceivedNoteById(id));
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    public ResponseEntity<Page<GoodsReceivedNoteResponse>> getNotesByPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(grnService.getNotesByPurchaseOrder(purchaseOrderId, pageable));
    }
}
