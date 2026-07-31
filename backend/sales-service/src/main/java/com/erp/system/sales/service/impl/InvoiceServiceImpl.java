package com.erp.system.sales.service.impl;

import com.erp.system.sales.client.InventoryClient;
import com.erp.system.sales.dto.InvoiceRequest;
import com.erp.system.sales.dto.InvoiceResponse;
import com.erp.system.sales.dto.OrderInvoiceRequest;
import com.erp.system.sales.dto.UpdateInvoiceRequest;
import com.erp.system.sales.entity.Customer;
import com.erp.system.sales.entity.Invoice;
import com.erp.system.sales.entity.InvoiceLineItem;
import com.erp.system.sales.repository.CustomerRepository;
import com.erp.system.sales.repository.InvoiceRepository;
import com.erp.system.sales.service.InvoiceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
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

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + System.currentTimeMillis())
                .customer(customer)
                .totalAmount(request.getTotalAmount())
                .status(request.getStatus())
                .dueDate(request.getDueDate() != null ? request.getDueDate().atStartOfDay() : null)
                .build();

        List<InvoiceLineItem> lineItems = request.getItems().stream()
                .map(item -> InvoiceLineItem.builder()
                        .invoice(invoice)
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
        invoice.setLineItems(lineItems);

        Invoice saved = invoiceRepository.save(invoice);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoiceFromOrder(OrderInvoiceRequest request) {
        Customer customer = customerRepository.findByName(request.getCustomerName())
                .orElseGet(() -> {
                    log.warn("Customer '{}' not found, creating placeholder", request.getCustomerName());
                    Customer newCustomer = Customer.builder()
                            .name(request.getCustomerName())
                            .email(request.getCustomerName().toLowerCase().replaceAll("\\s+", ".") + "@placeholder.com")
                            .phone("0000000000")
                            .address("Auto-created from order")
                            .build();
                    return customerRepository.save(newCustomer);
                });

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + System.currentTimeMillis())
                .customer(customer)
                .totalAmount(request.getTotalAmount())
                .status("PENDING")
                .build();

        List<InvoiceLineItem> lineItems = request.getItems().stream()
                .map(item -> InvoiceLineItem.builder()
                        .invoice(invoice)
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
        invoice.setLineItems(lineItems);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} auto-created from order", saved.getInvoiceNumber());
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

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(Long id, UpdateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        if (request.getStatus() != null) {
            invoice.setStatus(request.getStatus());
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate().atStartOfDay());
        }
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
