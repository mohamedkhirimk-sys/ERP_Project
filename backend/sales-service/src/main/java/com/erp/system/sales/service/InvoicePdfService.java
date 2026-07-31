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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private static final BigDecimal TVA_RATE = new BigDecimal("0.20");
    private static final String SELLER_NAME = "Mon Entreprise";
    private static final String SELLER_ADDRESS = "22, Avenue Voltaire";
    private static final String SELLER_CITY = "13000 Marseille";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InvoiceRepository invoiceRepository;
    private final ProductClient productClient;

    @Transactional(readOnly = true)
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
            Font greyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            PdfPTable top = new PdfPTable(2);
            top.setWidths(new float[]{1f, 1f});

            PdfPCell vendeurCell = new PdfPCell();
            vendeurCell.setBorder(0);
            vendeurCell.addElement(new Paragraph("Vendeur", boldFont));
            vendeurCell.addElement(new Paragraph(SELLER_NAME, normalFont));
            vendeurCell.addElement(new Paragraph(SELLER_ADDRESS, normalFont));
            vendeurCell.addElement(new Paragraph(SELLER_CITY, normalFont));
            top.addCell(vendeurCell);

            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(0);
            Paragraph title = new Paragraph("FACTURE", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            metaCell.addElement(title);
            metaCell.addElement(rightAligned("Numéro de facture : " + invoice.getInvoiceNumber(), normalFont));
            metaCell.addElement(rightAligned("Date : " + (invoice.getIssuedAt() != null ? invoice.getIssuedAt().toLocalDate() : "—"), normalFont));
            metaCell.addElement(rightAligned("Échéance : " + (invoice.getDueDate() != null ? invoice.getDueDate().toLocalDate() : "—"), normalFont));
            metaCell.addElement(rightAligned("Statut : " + invoice.getStatus(), normalFont));
            top.addCell(metaCell);
            document.add(top);

            document.add(new Paragraph(" "));

            document.add(new Paragraph("Client", boldFont));
            document.add(new Paragraph(invoice.getCustomer().getName(), normalFont));
            document.add(new Paragraph(invoice.getCustomer().getEmail(), normalFont));
            if (invoice.getCustomer().getPhone() != null) {
                document.add(new Paragraph(invoice.getCustomer().getPhone(), normalFont));
            }
            if (invoice.getCustomer().getAddress() != null) {
                document.add(new Paragraph(invoice.getCustomer().getAddress(), normalFont));
            }
            document.add(new Paragraph(" "));

            BigDecimal totalHt = BigDecimal.ZERO;
            BigDecimal totalTva = BigDecimal.ZERO;
            BigDecimal totalTtc = BigDecimal.ZERO;
            PdfPTable table = new PdfPTable(7);
            table.setWidths(new float[]{5f, 1.5f, 1.5f, 3f, 1.5f, 2.5f, 2.5f});
            addCell(table, "Description", boldFont, Element.ALIGN_LEFT);
            addCell(table, "Quantité", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "Unité", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "Prix unitaire HT", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "% TVA", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "Total TVA", boldFont, Element.ALIGN_RIGHT);
            addCell(table, "Total TTC", boldFont, Element.ALIGN_RIGHT);

            for (InvoiceLineItem item : invoice.getLineItems()) {
                ProductDto product = null;
                try {
                    product = productClient.getProductBySku(item.getProductSku());
                } catch (Exception e) {
                    log.warn("Product lookup failed for SKU {}: {}", item.getProductSku(), e.getMessage());
                    product = null;
                }
                boolean hasPrice = product != null && product.getPrice() != null;
                BigDecimal lineHt = hasPrice
                        ? product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO;
                BigDecimal lineTva = hasPrice
                        ? lineHt.multiply(TVA_RATE).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal lineTtc = hasPrice ? lineHt.add(lineTva) : BigDecimal.ZERO;
                if (hasPrice) {
                    totalHt = totalHt.add(lineHt);
                    totalTva = totalTva.add(lineTva);
                    totalTtc = totalTtc.add(lineTtc);
                }

                addCell(table, product != null ? product.getName() : item.getProductSku(), normalFont, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(item.getQuantity()), normalFont, Element.ALIGN_RIGHT);
                addCell(table, "pcs", normalFont, Element.ALIGN_RIGHT);
                addCell(table, hasPrice ? "$" + product.getPrice().setScale(2, RoundingMode.HALF_UP) : "n/a", normalFont, Element.ALIGN_RIGHT);
                addCell(table, "20 %", normalFont, Element.ALIGN_RIGHT);
                addCell(table, hasPrice ? "$" + lineTva : "—", normalFont, Element.ALIGN_RIGHT);
                addCell(table, hasPrice ? "$" + lineTtc : "—", normalFont, Element.ALIGN_RIGHT);
            }
            document.add(table);

            document.add(new Paragraph(" "));

            PdfPTable totals = new PdfPTable(2);
            totals.setWidths(new float[]{1f, 1f});
            totals.setWidthPercentage(40f);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addCell(totals, "Total HT", normalFont, Element.ALIGN_LEFT);
            addCell(totals, "$" + totalHt.setScale(2, RoundingMode.HALF_UP), normalFont, Element.ALIGN_RIGHT);
            addCell(totals, "Total TVA", normalFont, Element.ALIGN_LEFT);
            addCell(totals, "$" + totalTva.setScale(2, RoundingMode.HALF_UP), normalFont, Element.ALIGN_RIGHT);
            addCell(totals, "Total TTC", boldFont, Element.ALIGN_LEFT);
            addCell(totals, "$" + totalTtc.setScale(2, RoundingMode.HALF_UP), boldFont, Element.ALIGN_RIGHT);
            document.add(totals);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Informations additionnelles :", boldFont));
            document.add(new Paragraph("Service Après Vente : Garantie 1 an.", greyFont));
            document.add(new Paragraph(" "));

            PdfPTable footer = new PdfPTable(3);
            footer.setWidths(new float[]{1f, 1f, 1f});
            footer.setWidthPercentage(100f);
            addCell(footer, "Mon Entreprise", boldFont, Element.ALIGN_LEFT);
            addCell(footer, "Coordonnées", boldFont, Element.ALIGN_LEFT);
            addCell(footer, "Détails bancaires", boldFont, Element.ALIGN_LEFT);
            addCell(footer, "22, Avenue Voltaire", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "Pierre Fournisseur", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "Banque NP Paribas", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "13000 Marseille", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "Téléphone : +33 4 92 99 99 99", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "IBAN FR23 4112 4098 4098 23", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "N° Siren ou Siret : 1234567-8", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "E-mail : pierre@macompagnie.fr", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "SWIFT/BIC FRHHCXX1001", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "N° TVA intra. : FRXX 999999999", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "www. macompagnie.com", greyFont, Element.ALIGN_LEFT);
            addCell(footer, "", greyFont, Element.ALIGN_LEFT);
            document.add(footer);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Généré le : " + LocalDateTime.now().format(DATE_FORMAT), greyFont));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF for invoice " + invoiceId, e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private Paragraph rightAligned(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
