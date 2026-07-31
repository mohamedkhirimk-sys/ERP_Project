# Invoice PDF Footer — Design

**Date:** 2026-07-31
**Status:** Implemented and committed (bb193f6)

## Context

The generated invoice PDF already ended with a small grey "Généré le : ..." line. The French
`modèle_de_facture` template (the visual model for the invoice) contains a bottom section
reading "Informations additionnelles : Service Après Vente : Garantie 1 an." The user asked to
add the footer "as in the invoice model".

## Design

### Existing block (kept)

After the totals table:

- **"Informations additionnelles :"** — bold, 10pt (same as section headings)
- **"Service Après Vente : Garantie 1 an."** — grey, 8pt (same style as the "Généré le" line)

### New 3-column footer block (below the additional-info block)

Full-width `PdfPTable(3)`, headers bold 10pt, data grey 8pt:

| Mon Entreprise | Coordonnées | Détails bancaires |
|---|---|---|
| 22, Avenue Voltaire | Pierre Fournisseur | Banque NP Paribas |
| 13000 Marseille | Téléphone : +33 4 92 99 99 99 | IBAN FR23 4112 4098 4098 23 |
| N° Siren ou Siret : 1234567-8 | E-mail : pierre@macompagnie.fr | SWIFT/BIC FRHHCXX1001 |
| N° TVA intra. : FRXX 999999999 | www. macompagnie.com | (empty) |

The "Généré le" line remains last (grey, 8pt).

Both blocks are content blocks at the end of the document (matching the template), not fixed
page-position footers. All text is hardcoded like the seller placeholder ("Mon Entreprise");
real company data can be configured later.

## Scope

- `backend/sales-service/.../service/InvoicePdfService.java` — add the footer paragraphs and table
- `backend/sales-service/.../service/InvoicePdfServiceTest.java` — assert footer strings in the
  layout test

## Out of scope

- Real page footer (PdfPageEventHelper), payment conditions block, configurable text
