package com.erp.system.procurement.service;

import com.erp.system.procurement.dto.GoodsReceivedNoteRequest;
import com.erp.system.procurement.dto.GoodsReceivedNoteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GoodsReceivedNoteService {
    GoodsReceivedNoteResponse createGoodsReceivedNote(GoodsReceivedNoteRequest request);
    GoodsReceivedNoteResponse getGoodsReceivedNoteById(Long id);
    Page<GoodsReceivedNoteResponse> getNotesByPurchaseOrder(Long purchaseOrderId, Pageable pageable);
    Page<GoodsReceivedNoteResponse> getAllGoodsReceivedNotes(Pageable pageable);
}
