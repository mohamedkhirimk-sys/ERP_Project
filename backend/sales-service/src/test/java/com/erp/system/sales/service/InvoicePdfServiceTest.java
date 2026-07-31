package com.erp.system.sales.service;

import com.erp.common.exception.ResourceNotFoundException;
import com.erp.system.sales.client.ProductClient;
import com.erp.system.sales.dto.ProductDto;
import com.erp.system.sales.entity.Customer;
import com.erp.system.sales.entity.Invoice;
import com.erp.system.sales.entity.InvoiceLineItem;
import com.erp.system.sales.repository.InvoiceRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ProductClient productClient;

    private InvoicePdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new InvoicePdfService(invoiceRepository, productClient);
    }

    @Test
    void generatePdf_shouldReturnPdfBytes_whenInvoiceExists() throws Exception {
        Customer customer = Customer.builder().id(1L).name("Acme Corporation")
                .email("billing@acme.com").phone("555-0100").address("1 Main St").build();
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-123")
                .customer(customer).totalAmount(new BigDecimal("599.98")).status("PAID")
                .build();
        invoice.setLineItems(List.of(
                InvoiceLineItem.builder().productSku("CHAIR-001").quantity(2).build()));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(productClient.getProductBySku("CHAIR-001")).thenReturn(
                ProductDto.builder().sku("CHAIR-001").name("Ergonomic Office Chair")
                        .price(new BigDecimal("299.99")).build());

        byte[] pdf = pdfService.generatePdf(1L);

        assertThat(pdf).startsWith("%PDF".getBytes());
        String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(text).contains("INV-123", "Acme Corporation", "Ergonomic Office Chair", "599.98");
    }

    @Test
    void generatePdf_shouldFallbackToSku_whenProductLookupFails() throws Exception {
        Customer customer = Customer.builder().id(1L).name("Acme Corporation").build();
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-456").customer(customer)
                .totalAmount(new BigDecimal("10.00")).status("PENDING").build();
        invoice.setLineItems(List.of(
                InvoiceLineItem.builder().productSku("GONE-001").quantity(1).build()));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(productClient.getProductBySku("GONE-001"))
                .thenThrow(new RuntimeException("product-service down"));

        byte[] pdf = pdfService.generatePdf(1L);

        assertThat(pdf).startsWith("%PDF".getBytes());
        String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(text).contains("GONE-001", "n/a");
    }

    @Test
    void generatePdf_shouldThrow404_whenInvoiceMissing() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pdfService.generatePdf(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
