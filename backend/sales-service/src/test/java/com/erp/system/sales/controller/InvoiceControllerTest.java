package com.erp.system.sales.controller;

import com.erp.common.exception.GlobalExceptionHandler;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.system.sales.dto.InvoiceResponse;
import com.erp.system.sales.service.InvoicePdfService;
import com.erp.system.sales.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoicePdfService invoicePdfService;

    @InjectMocks
    private InvoiceController invoiceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getInvoiceById_shouldReturn200_whenFound() throws Exception {
        InvoiceResponse response = InvoiceResponse.builder()
                .id(1L).invoiceNumber("INV-001").build();
        when(invoiceService.getInvoiceById(1L)).thenReturn(response);
        mockMvc.perform(get("/api/invoices/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getInvoiceById_shouldReturn404_whenNotFound() throws Exception {
        when(invoiceService.getInvoiceById(1L))
                .thenThrow(new ResourceNotFoundException("Invoice", 1L));
        mockMvc.perform(get("/api/invoices/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInvoicePdf_shouldReturnPdf_whenFound() throws Exception {
        when(invoicePdfService.generatePdf(1L)).thenReturn("%PDF-1.4 test".getBytes());
        mockMvc.perform(get("/api/invoices/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"INV-1.pdf\""));
    }

    @Test
    void getInvoicePdf_shouldReturn404_whenNotFound() throws Exception {
        when(invoicePdfService.generatePdf(1L))
                .thenThrow(new ResourceNotFoundException("Invoice", 1L));
        mockMvc.perform(get("/api/invoices/1/pdf"))
                .andExpect(status().isNotFound());
    }
}
