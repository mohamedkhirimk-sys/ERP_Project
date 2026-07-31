# Invoice PDF Export — Design

Date: 2026-07-31
Status: Approved by user

## Overview

Users can download any invoice as a professionally formatted PDF from the Invoices page. The PDF is generated server-side in sales-service using OpenPDF; the frontend only links to the endpoint.

## Architecture

- PDF generation in **sales-service** (backend) using **OpenPDF** (`com.github.librepdf:openpdf`), a pure-Java, Apache-licensed PDF library.
- New endpoint: `GET /api/invoices/{id}/pdf`
  - 200 with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="<invoiceNumber>.pdf"` on success
  - 404 if the invoice does not exist
- Frontend: a **PDF** button on each invoice row in `InvoiceListPage` that opens the endpoint URL (the attachment header triggers the download).

## Components

### product-service

Add `GET /api/products/by-sku/{sku}` in `ProductController`:
- Returns the existing `Product` entity (contains `sku`, `name`, `price`)
- 404 (`ResponseStatusException`/not-found response) when no product has that SKU

### sales-service

1. Add OpenPDF dependency to `pom.xml`.
2. New `ProductClient` Feign client (pattern of existing `InventoryClient`):
   - `GET /api/products/by-sku/{sku}` → `Product` response fields (sku, name, price)
3. New `InvoicePdfService`:
   - Loads invoice by id (with `customer` and `lineItems`)
   - For each line item SKU, calls `ProductClient` to get name + unit price
   - Generates the PDF containing:
     - Header: "ERP Invoice", invoice number, issue date, due date, status
     - Customer block: name, email, phone, address
     - Items table: SKU | Name | Qty | Unit Price | Line Total
     - Grand total row (from `invoice.totalAmount`)
4. `InvoiceController`:
   - New `GET /{id}/pdf` returning the PDF bytes with the headers above

### frontend

`InvoiceListPage.tsx`: add a **PDF** button (with download icon) in the Actions column next to Edit; clicking it downloads the PDF for that invoice.

## Error Handling

- Invoice not found → 404
- Product lookup fails for a SKU (product deleted or product-service unavailable) → that line item still renders with SKU + qty and "n/a" unit price; the PDF is still generated. Lookup failures must not abort the whole document.

## Data Flow

1. User clicks PDF button on an invoice row
2. Browser GETs `/api/invoices/{id}/pdf` (via gateway :8082 → sales-service :8095)
3. sales-service loads invoice + customer + line items from `sales_db`
4. Per line item SKU: Feign call to product-service `/api/products/by-sku/{sku}` for name + price
5. OpenPDF builds the document; bytes stream back with attachment headers
6. Browser saves the PDF file

## Verification

1. `mvn.cmd clean compile` in product-service and sales-service
2. `curl.exe` the PDF endpoint: expect 200, `Content-Type: application/pdf`, body starting with `%PDF`
3. 404 case: PDF endpoint for a nonexistent invoice id
4. Browser: click PDF button → file downloads and opens with correct content
5. Regression: existing invoice list/edit flows still work
