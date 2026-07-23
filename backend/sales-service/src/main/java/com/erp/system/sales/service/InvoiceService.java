package com.erp.system.sales.service;

import com.erp.system.sales.dto.InvoiceRequest;
import com.erp.system.sales.dto.InvoiceResponse;
import java.util.List;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    InvoiceResponse getInvoiceById(Long id);
    List<InvoiceResponse> getInvoicesByCustomer(Long customerId);
    List<InvoiceResponse> getAllInvoices();
    InvoiceResponse updateInvoiceStatus(Long id, String status);
}
