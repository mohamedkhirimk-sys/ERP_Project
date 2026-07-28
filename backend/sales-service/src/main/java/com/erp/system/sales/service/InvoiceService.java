package com.erp.system.sales.service;

import com.erp.system.sales.dto.InvoiceRequest;
import com.erp.system.sales.dto.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    InvoiceResponse getInvoiceById(Long id);
    Page<InvoiceResponse> getInvoicesByCustomer(Long customerId, Pageable pageable);
    Page<InvoiceResponse> getAllInvoices(Pageable pageable);
    InvoiceResponse updateInvoiceStatus(Long id, String status);
}
