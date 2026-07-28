package com.erp.system.procurement.service;

import com.erp.system.procurement.dto.VendorRequest;
import com.erp.system.procurement.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorService {
    Vendor createVendor(VendorRequest request);
    Vendor getVendorById(Long id);
    Page<Vendor> getAllVendors(Pageable pageable);
    Vendor updateVendor(Long id, VendorRequest request);
    void deleteVendor(Long id);
}
