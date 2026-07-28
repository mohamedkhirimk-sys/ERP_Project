package com.erp.system.procurement.service.impl;

import com.erp.system.procurement.dto.PurchaseOrderRequest;
import com.erp.system.procurement.dto.PurchaseOrderResponse;
import com.erp.system.procurement.entity.PurchaseOrder;
import com.erp.system.procurement.entity.Vendor;
import com.erp.system.procurement.repository.PurchaseOrderRepository;
import com.erp.system.procurement.repository.VendorRepository;
import com.erp.system.procurement.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorRepository vendorRepository;

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
        return toResponse(purchaseOrderRepository.save(po));
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
