package com.erp.system.procurement.repository;

import com.erp.system.procurement.entity.GoodsReceivedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoodsReceivedNoteRepository extends JpaRepository<GoodsReceivedNote, Long> {
    List<GoodsReceivedNote> findByPurchaseOrderId(Long purchaseOrderId);
}
