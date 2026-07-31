package com.erp.system.sales.service;

import com.erp.common.exception.ResourceNotFoundException;
import com.erp.system.sales.client.ProductClient;
import com.erp.system.sales.dto.ProductDto;
import com.erp.system.sales.entity.Customer;
import com.erp.system.sales.entity.Invoice;
import com.erp.system.sales.entity.InvoiceLineItem;
import com.erp.system.sales.repository.InvoiceRepository;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

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
    void generatePdf_shouldRenderTemplateLayout_whenInvoiceExists() throws Exception {
        Customer customer = Customer.builder().id(1L).name("Acme Corporation")
                .email("billing@acme.com").phone("555-0100").address("1 Main St").build();
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-123")
                .customer(customer).totalAmount(new BigDecimal("719.98")).status("PAID")
                .build();
        invoice.setLineItems(List.of(
                InvoiceLineItem.builder().productSku("CHAIR-001").quantity(2).build()));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(productClient.getProductBySku("CHAIR-001")).thenReturn(
                ProductDto.builder().sku("CHAIR-001").name("Ergonomic Office Chair")
                        .price(new BigDecimal("299.99")).build());

        byte[] pdf = pdfService.generatePdf(1L);

        assertThat(pdf).startsWith("%PDF".getBytes());
        String text = pageText(pdf);
        assertThat(text).contains(
                "FACTURE",
                "Numéro de facture : INV-123",
                "Statut : PAID",
                "Vendeur",
                "Mon Entreprise",
                "Client",
                "Acme Corporation",
                "Ergonomic Office Chair",
                "Prix unitaire HT",
                "% TVA",
                "Total TVA",
                "Total TTC",
                "20 %",
                "pcs",
                "$299.99",
                "$599.98",
                "$120.00",
                "$719.98",
                "Total HT",
                "Total HT $599.98",
                "Total TVA $120.00",
                "Total TTC $719.98",
                "Généré le :");
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
        String text = pageText(pdf);
        assertThat(text).contains("GONE-001", "n/a", "—", "Total TTC",
                "Total HT $0.00", "Total TVA $0.00", "Total TTC $0.00");
    }

    @Test
    void generatePdf_shouldThrow404_whenInvoiceMissing() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pdfService.generatePdf(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * This decoder exists because OpenPDF 2.0.3's PdfTextExtractor cannot be
     * used here: (a) it has no static getTextFromPage(PdfReader, int), and
     * (b) it mangles WinAnsi accented characters (é/É/—) written with
     * standard Type1 fonts (known issue, LibrePDF/OpenPDF#618), so the
     * accented strings asserted below could never match. Byte-level
     * Windows-1252 decoding of the page content stream is therefore required,
     * and it validates the exact bytes the service writes to the PDF.
     */
    private static String pageText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfObject contents = PdfReader.getPdfObject(reader.getPageN(1).get(PdfName.CONTENTS));
            StringBuilder text = new StringBuilder();
            if (contents instanceof PRStream) {
                text.append(decodeContent(PdfReader.getStreamBytes((PRStream) contents)));
            } else if (contents instanceof PdfArray) {
                for (PdfObject obj : ((PdfArray) contents).getElements()) {
                    PdfObject resolved = PdfReader.getPdfObject(obj);
                    if (resolved instanceof PRStream) {
                        text.append(decodeContent(PdfReader.getStreamBytes((PRStream) resolved)));
                    }
                }
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static String decodeContent(byte[] content) throws Exception {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < content.length) {
            byte b = content[i];
            if (b == '(') {
                result.append(' ').append(decodeLiteralString(content, i + 1));
                i = skipLiteralString(content, i + 1);
            } else if (b == '<') {
                result.append(' ').append(decodeHexString(content, i + 1));
                i = skipHexString(content, i + 1);
            } else {
                i++;
            }
        }
        return result.toString();
    }

    private static String decodeLiteralString(byte[] content, int start) throws Exception {
        ByteArrayOutputStream literal = new ByteArrayOutputStream();
        int i = start;
        while (i < content.length && content[i] != ')') {
            if (content[i] == '\\') {
                i++;
                if (i >= content.length) {
                    break;
                }
                byte e = content[i];
                if (e >= '0' && e <= '7') {
                    int octal = 0;
                    int digits = 0;
                    while (digits < 3 && i < content.length && content[i] >= '0' && content[i] <= '7') {
                        octal = octal * 8 + (content[i] - '0');
                        i++;
                        digits++;
                    }
                    literal.write(octal);
                    continue;
                }
                switch (e) {
                    case 'n' -> literal.write('\n');
                    case 'r' -> literal.write('\r');
                    case 't' -> literal.write('\t');
                    case 'b' -> literal.write(8);
                    case 'f' -> literal.write(12);
                    case '(' -> literal.write('(');
                    case ')' -> literal.write(')');
                    case '\\' -> literal.write('\\');
                    default -> literal.write(e);
                }
            } else {
                literal.write(content[i]);
            }
            i++;
        }
        return new String(literal.toByteArray(), WIN_ANSI);
    }

    private static int skipLiteralString(byte[] content, int start) {
        int i = start;
        while (i < content.length) {
            if (content[i] == '\\') {
                i += 2;
            } else if (content[i] == ')') {
                return i + 1;
            } else {
                i++;
            }
        }
        return i;
    }

    private static String decodeHexString(byte[] content, int start) throws Exception {
        ByteArrayOutputStream hex = new ByteArrayOutputStream();
        int i = start;
        while (i < content.length && content[i] != '>') {
            while (i < content.length && Character.isWhitespace(content[i] & 0xFF)) {
                i++;
            }
            if (i >= content.length || content[i] == '>') {
                break;
            }
            int high = hexDigit(content[i]);
            i++;
            int low = 0;
            if (i < content.length && content[i] != '>' && !Character.isWhitespace(content[i] & 0xFF)) {
                low = hexDigit(content[i]);
                i++;
            }
            hex.write((high << 4) | low);
        }
        return new String(hex.toByteArray(), WIN_ANSI);
    }

    private static int skipHexString(byte[] content, int start) {
        int i = start;
        while (i < content.length && content[i] != '>') {
            i++;
        }
        return i < content.length ? i + 1 : i;
    }

    private static int hexDigit(byte b) {
        int c = b & 0xFF;
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return c - 'A' + 10;
    }
}
