package com.erp.system.procurement.service.impl;

import com.erp.system.procurement.client.AccountingClient;
import com.erp.system.procurement.dto.AccountingPostingRequest;
import com.erp.system.procurement.dto.PurchaseOrderRequest;
import com.erp.system.procurement.dto.PurchaseOrderResponse;
import com.erp.system.procurement.entity.PurchaseOrder;
import com.erp.system.procurement.entity.Vendor;
import com.erp.system.procurement.repository.PurchaseOrderRepository;
import com.erp.system.procurement.repository.VendorRepository;
import com.erp.system.procurement.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorRepository vendorRepository;

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private final AccountingClient accountingClient;

    @Override
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + request.getVendorId()));

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber("PO-" + System.currentTimeMillis())
                .vendor(vendor)
                .totalAmount(request.getTotalAmount())
                .status(request.getStatus())
                .build();

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return toResponse(saved);
    }

    @Override
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PurchaseOrder not found with id: " + id));
        return toResponse(po);
    }

    @Override
    public Page<PurchaseOrderResponse> getPurchaseOrdersByVendor(Long vendorId, Pageable pageable) {
        return purchaseOrderRepository.findByVendorId(vendorId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable) {
        return purchaseOrderRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public PurchaseOrderResponse updatePurchaseOrderStatus(Long id, String status) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PurchaseOrder not found with id: " + id));
        po.setStatus(status);
        PurchaseOrderResponse response = toResponse(purchaseOrderRepository.save(po));

        if ("RECEIVED".equalsIgnoreCase(status)) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("GOODS_RECEIVED")
                        .sourceId(po.getPoNumber())
                        .totalAmount(po.getTotalAmount())
                        .build());
            } catch (Exception e) {
                log.error("Failed to post accounting entry for PO {} (amount {}): {}",
                        po.getPoNumber(), po.getTotalAmount(), e.getMessage());
            }
        }
        return response;
    }

    @Override
    public Map<String, Object> backfillAccounting() {
        List<PurchaseOrder> received = purchaseOrderRepository.findAll().stream()
                .filter(po -> "RECEIVED".equalsIgnoreCase(po.getStatus()))
                .toList();
        int created = 0;
        for (PurchaseOrder po : received) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("GOODS_RECEIVED")
                        .sourceId(po.getPoNumber())
                        .totalAmount(po.getTotalAmount())
                        .build());
                created++;
            } catch (Exception e) {
                log.error("Failed to post accounting entry for PO {} (amount {}): {}",
                        po.getPoNumber(), po.getTotalAmount(), e.getMessage());
            }
        }
        return Map.of("processed", received.size(), "created", created);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendor().getId())
                .vendorName(po.getVendor().getName())
                .totalAmount(po.getTotalAmount())
                .status(po.getStatus())
                .orderedAt(po.getOrderedAt())
                .build();
    }
}
