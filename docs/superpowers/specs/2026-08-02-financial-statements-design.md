# Bilan & Compte de résultat (Financial Statements) — Design

Date : 2026-08-02
Statut : approuvé

## Objectif

Générer les documents de synthèse comptable — **Balance Sheet (Bilan)** et **Income Statement (Compte de résultat)** — à partir de la balance (trial balance) déjà calculée par le reporting-service. Affichage à l'écran dans un premier temps ; export PDF en étape ultérieure (hors périmètre).

## Décisions validées

- **Forme** : affichage à l'écran uniquement (PDF plus tard).
- **Emplacement** : la page `Financial Report` gagne une barre d'onglets `Trial Balance | Balance Sheet | Income Statement`.
- **Résultat net** : ligne « Net income » distincte dans la section Equity du bilan, identique à la dernière ligne du compte de résultat (norme comptable : Actif = Passif + Capitaux propres + Résultat).
- **Langue de l'UI** : anglais (cohérence avec l'existant).

## Architecture

### Backend — reporting-service

Deux nouveaux endpoints, calculés dans `ReportService` (réutilisation de `fetchList` et des données `/finance-service/api/accounts` — comptes triés par `accountCode`, **comptes à solde non nul uniquement**) :

- `GET /api/reports/balance-sheet` → `BalanceSheetResponse`
- `GET /api/reports/income-statement` → `IncomeStatementResponse`

La route gateway `/api/reports/**` existe déjà (aucun changement gateway).

### Balance Sheet (`BalanceSheetResponse`)

| Champ | Définition |
|---|---|
| `assets[]` | comptes `ASSET` (solde débit, positif) — `accountCode`, `accountName`, `balance` |
| `liabilities[]` | comptes `LIABILITY` (solde crédit → affiché en positif) |
| `equity[]` | comptes `EQUITY` (solde crédit → positif) |
| `netIncome` | `−(Σ revenue + Σ expenses)` à partir des soldes (bénéfice si positif, perte si négatif) |
| `totalAssets`, `totalLiabilities`, `totalEquity` | sous-totaux |
| `totalLiabilitiesEquity` | `totalLiabilities + totalEquity + netIncome` |

Invariant : `totalAssets == totalLiabilitiesEquity`.

Données actuelles attendues : Actif 600.00 (1000 160 + 1010 320 + 1020 100 + 1100 20) = Passif 30.00 + Equity 470.00 + Net income 100.00.

### Income Statement (`IncomeStatementResponse`)

| Champ | Définition |
|---|---|
| `revenue[]` | comptes `REVENUE` (solde crédit → affiché en positif) |
| `expenses[]` | comptes `EXPENSE` (solde débit, positif) |
| `totalRevenue`, `totalExpenses` | sous-totaux |
| `netIncome` | `totalRevenue − totalExpenses` (perte = négatif) |

Données actuelles attendues : Revenue 150.00 − Expenses 50.00 = **Net income 100.00** (identique au bilan).

### Frontend — `FinancialReportPage.tsx`

- Barre d'onglets (pattern des onglets existants, ex. journal) : `Trial Balance | Balance Sheet | Income Statement`.
- L'onglet Trial Balance = contenu actuel de la page (déplacé tel quel).
- `BalanceSheetTab` : sections Assets / Liabilities / Equity (tables : code, name, balance) + ligne « Net income » dans Equity + lignes de totaux ; total Actif vs total Passif/Capitaux/Résultat.
- `IncomeStatementTab` : tables Revenue / Expenses + ligne Net income.
- Nouveaux fichiers : `frontend/src/features/reports/BalanceSheetTab.tsx`, `IncomeStatementTab.tsx` (+ interfaces). Appels axios directs via `@/lib/axios` (pattern existant), erreurs en console (pattern existant), états loading selon le pattern.
- Composants ajoutés sur la page existante — aucun changement de routage, aucun nouveau menu.

## Périmètre & non-périmètre

- Périmètre : 2 endpoints backend, 2 composants frontend, onglets, vérification E2E via gateway (équilibre 600/600, revenus 150 − charges 50 = résultat 100).
- Hors périmètre : filtre de périodes (données toutes périodes, comme le Trial Balance actuel), export PDF, détail par département/client, consolidation multi-établissements.

## Vérification

- Backend : `mvn.cmd clean compile` (reporting-service — pas de framework de test dans ce module, vérification E2E).
- E2E via gateway : `/api/reports/balance-sheet` (600 = 30 + 470 + 100) et `/api/reports/income-statement` (150 − 50 = 100) sur les données actuelles.
- Frontend : `npm run build` + `npm run lint` verts, vérification visuelle par l'utilisateur.
