package com.erp.system.procurement.service.impl;

import com.erp.system.procurement.client.InventoryClient;
import com.erp.system.procurement.dto.GoodsReceivedNoteRequest;
import com.erp.system.procurement.dto.GoodsReceivedNoteResponse;
import com.erp.system.procurement.entity.GRNLineItem;
import com.erp.system.procurement.entity.GoodsReceivedNote;
import com.erp.system.procurement.entity.PurchaseOrder;
import com.erp.system.procurement.repository.GoodsReceivedNoteRepository;
import com.erp.system.procurement.repository.PurchaseOrderRepository;
import com.erp.system.procurement.service.GoodsReceivedNoteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsReceivedNoteServiceImpl implements GoodsReceivedNoteService {

    private final GoodsReceivedNoteRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public GoodsReceivedNoteResponse createGoodsReceivedNote(GoodsReceivedNoteRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new RuntimeException("PurchaseOrder not found with id: " + request.getPurchaseOrderId()));

        request.getItems().forEach(item ->
                inventoryClient.addStock(item.getProductSku(), item.getQuantity()));

        List<GRNLineItem> lineItems = request.getItems().stream()
                .map(item -> GRNLineItem.builder()
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        GoodsReceivedNote grn = GoodsReceivedNote.builder()
                .grnNumber("GRN-" + System.currentTimeMillis())
                .purchaseOrder(po)
                .status(request.getStatus())
                .lineItems(lineItems)
                .build();

        GoodsReceivedNote saved = grnRepository.save(grn);
        return toResponse(saved);
    }

    @Override
    public GoodsReceivedNoteResponse getGoodsReceivedNoteById(Long id) {
        GoodsReceivedNote grn = grnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("GoodsReceivedNote not found with id: " + id));
        return toResponse(grn);
    }

    @Override
    public Page<GoodsReceivedNoteResponse> getNotesByPurchaseOrder(Long purchaseOrderId, Pageable pageable) {
        return grnRepository.findByPurchaseOrderId(purchaseOrderId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<GoodsReceivedNoteResponse> getAllGoodsReceivedNotes(Pageable pageable) {
        return grnRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private GoodsReceivedNoteResponse toResponse(GoodsReceivedNote grn) {
        return GoodsReceivedNoteResponse.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrder().getId())
                .poNumber(grn.getPurchaseOrder().getPoNumber())
                .status(grn.getStatus())
                .receivedAt(grn.getReceivedAt())
                .build();
    }
}
