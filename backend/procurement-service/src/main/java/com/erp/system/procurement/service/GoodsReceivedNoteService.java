package com.erp.system.procurement.service;

import com.erp.system.procurement.dto.GoodsReceivedNoteRequest;
import com.erp.system.procurement.dto.GoodsReceivedNoteResponse;
import java.util.List;

public interface GoodsReceivedNoteService {
    GoodsReceivedNoteResponse createGoodsReceivedNote(GoodsReceivedNoteRequest request);
    GoodsReceivedNoteResponse getGoodsReceivedNoteById(Long id);
    List<GoodsReceivedNoteResponse> getNotesByPurchaseOrder(Long purchaseOrderId);
    List<GoodsReceivedNoteResponse> getAllGoodsReceivedNotes();
}
