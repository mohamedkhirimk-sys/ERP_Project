# Invoice PDF Footer — Design

**Date:** 2026-07-31
**Status:** Implemented and committed (bb193f6)

## Context

The generated invoice PDF already ended with a small grey "Généré le : ..." line. The French
`modèle_de_facture` template (the visual model for the invoice) contains a bottom section
reading "Informations additionnelles : Service Après Vente : Garantie 1 an." The user asked to
add the footer "as in the invoice model".

## Design

After the totals table (and before the "Généré le" line), add a footer block:

- **"Informations additionnelles :"** — bold, 10pt (same as section headings)
- **"Service Après Vente : Garantie 1 an."** — grey, 8pt (same style as the "Généré le" line)

Kept as a content block at the end of the document (matching the template), not a fixed
page-position footer. Text is hardcoded like the seller placeholder ("Mon Entreprise"); real
after-sales terms can replace it later.

## Scope

- `backend/sales-service/.../service/InvoicePdfService.java` — add the two paragraphs
- `backend/sales-service/.../service/InvoicePdfServiceTest.java` — assert both strings in the
  layout test

## Out of scope

- Real page footer (PdfPageEventHelper), payment conditions block, configurable text
