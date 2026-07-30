package com.erp.system.sales.service.impl;

import com.erp.system.sales.client.InventoryClient;
import com.erp.system.sales.dto.InvoiceRequest;
import com.erp.system.sales.dto.InvoiceResponse;
import com.erp.system.sales.entity.Customer;
import com.erp.system.sales.entity.Invoice;
import com.erp.system.sales.entity.InvoiceLineItem;
import com.erp.system.sales.repository.CustomerRepository;
import com.erp.system.sales.repository.InvoiceRepository;
import com.erp.system.sales.service.InvoiceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + request.getCustomerId()));

        request.getItems().forEach(item ->
                inventoryClient.deductStock(item.getProductSku(), -item.getQuantity()));

        List<InvoiceLineItem> lineItems = request.getItems().stream()
                .map(item -> InvoiceLineItem.builder()
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + System.currentTimeMillis())
                .customer(customer)
                .totalAmount(request.getTotalAmount())
                .status(request.getStatus())
                .dueDate(request.getDueDate())
                .lineItems(lineItems)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        return toResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getInvoicesByCustomer(Long customerId, Pageable pageable) {
        return invoiceRepository.findByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    public InvoiceResponse updateInvoiceStatus(Long id, String status) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        invoice.setStatus(status);
        return toResponse(invoiceRepository.save(invoice));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerId(invoice.getCustomer().getId())
                .customerName(invoice.getCustomer().getName())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .issuedAt(invoice.getIssuedAt())
                .dueDate(invoice.getDueDate())
                .build();
    }
}
