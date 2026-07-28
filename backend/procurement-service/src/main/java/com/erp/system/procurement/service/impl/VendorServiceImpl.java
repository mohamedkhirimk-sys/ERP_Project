package com.erp.system.procurement.service.impl;

import com.erp.system.procurement.dto.VendorRequest;
import com.erp.system.procurement.entity.Vendor;
import com.erp.system.procurement.repository.VendorRepository;
import com.erp.system.procurement.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    @Override
    public Vendor createVendor(VendorRequest request) {
        if (vendorRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Vendor with email " + request.getEmail() + " already exists");
        }
        Vendor vendor = Vendor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();
        return vendorRepository.save(vendor);
    }

    @Override
    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));
    }

    @Override
    public Page<Vendor> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable);
    }

    @Override
    public Vendor updateVendor(Long id, VendorRequest request) {
        Vendor vendor = getVendorById(id);
        vendor.setName(request.getName());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setAddress(request.getAddress());
        return vendorRepository.save(vendor);
    }

    @Override
    public void deleteVendor(Long id) {
        Vendor vendor = getVendorById(id);
        vendorRepository.delete(vendor);
    }
}
