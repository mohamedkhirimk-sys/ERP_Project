package com.erp.system.procurement.service;

import com.erp.system.procurement.dto.VendorRequest;
import com.erp.system.procurement.entity.Vendor;
import java.util.List;

public interface VendorService {
    Vendor createVendor(VendorRequest request);
    Vendor getVendorById(Long id);
    List<Vendor> getAllVendors();
    Vendor updateVendor(Long id, VendorRequest request);
    void deleteVendor(Long id);
}
