package com.erp.system.sales.service;

import com.erp.common.exception.ResourceNotFoundException;
import com.erp.system.sales.client.ProductClient;
import com.erp.system.sales.dto.ProductDto;
import com.erp.system.sales.entity.Invoice;
import com.erp.system.sales.entity.InvoiceLineItem;
import com.erp.system.sales.repository.InvoiceRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final ProductClient productClient;

    @Transactional
    public byte[] generatePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("ERP Invoice", titleFont));
            document.add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), normalFont));
            document.add(new Paragraph("Issued: " + (invoice.getIssuedAt() != null ? invoice.getIssuedAt().toLocalDate() : "—"), normalFont));
            document.add(new Paragraph("Due: " + (invoice.getDueDate() != null ? invoice.getDueDate().toLocalDate() : "—"), normalFont));
            document.add(new Paragraph("Status: " + invoice.getStatus(), normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Bill To", boldFont));
            document.add(new Paragraph(invoice.getCustomer().getName(), normalFont));
            document.add(new Paragraph(invoice.getCustomer().getEmail(), normalFont));
            if (invoice.getCustomer().getPhone() != null) {
                document.add(new Paragraph(invoice.getCustomer().getPhone(), normalFont));
            }
            if (invoice.getCustomer().getAddress() != null) {
                document.add(new Paragraph(invoice.getCustomer().getAddress(), normalFont));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidths(new float[]{3f, 5f, 1.5f, 2.5f, 3f});
            addCell(table, "SKU", boldFont, Element.ALIGN_LEFT);
            addCell(table, "Description", boldFont, Element.ALIGN_LEFT);
            addCell(table, "Qty", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "Unit Price", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "Line Total", boldFont, Element.ALIGN_RIGHT);

            for (InvoiceLineItem item : invoice.getLineItems()) {
                ProductDto product = null;
                try {
                    product = productClient.getProductBySku(item.getProductSku());
                } catch (Exception e) {
                    product = null;
                }
                String name = product != null ? product.getName() : item.getProductSku();
                String unitPrice = product != null && product.getPrice() != null
                        ? "$" + product.getPrice() : "n/a";
                BigDecimal lineTotal = product != null && product.getPrice() != null
                        ? product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO;
                addCell(table, item.getProductSku(), normalFont, Element.ALIGN_LEFT);
                addCell(table, name, normalFont, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(item.getQuantity()), normalFont, Element.ALIGN_RIGHT);
                addCell(table, unitPrice, normalFont, Element.ALIGN_RIGHT);
                addCell(table, "$" + lineTotal.setScale(2, RoundingMode.HALF_UP), normalFont, Element.ALIGN_RIGHT);
            }

            addCell(table, "", normalFont, Element.ALIGN_LEFT);
            addCell(table, "", normalFont, Element.ALIGN_LEFT);
            addCell(table, "", normalFont, Element.ALIGN_LEFT);
            addCell(table, "Total", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "$" + invoice.getTotalAmount(), boldFont, Element.ALIGN_RIGHT);

            document.add(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF for invoice " + invoiceId, e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
