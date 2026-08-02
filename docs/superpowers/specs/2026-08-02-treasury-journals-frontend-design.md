# Frontend — Trésorerie & Journaux auxiliaires

Date: 2026-08-02
Status: Approuvé

## Objectif

Donner une interface utilisateur aux deux modules backend récemment livrés dans finance-service :

1. **Trésorerie** — comptes bancaires, virements, dépenses, dépôts, retraits, position de trésorerie et mouvements.
2. **Journaux auxiliaires** — consultation des 6 journaux (VTE, ENC, ACH, DEC, BNQ, OD) avec totaux, lignes d'écritures et filtre de dates.

## Décisions

- **Langue de l'UI : anglais** (cohérence avec l'existant). Les labels français des journaux (ex. "Journal des ventes") viennent du backend.
- **Trésorerie : une page unique à onglets** (`/treasury`) — Position / Operations / Movements / Banks.
- **Journaux : remplacement de `/journal`** — l'ancienne vue plate `JournalEntryListPage` est supprimée, sa route pointe vers la nouvelle page auxiliaires.
- **Architecture : option A** — appels axios directs dans les pages (pattern existant), avec un petit module partagé typé uniquement pour la trésorerie (`features/treasury/api.ts`) car 4 onglets partagent les mêmes types et endpoints.
- **Écriture manuelle** : `CreateJournalEntryPage` est conservée et enrichie d'un sélecteur de journal (`journalCode`, 6 codes, défaut OD).

## Trésorerie — page `/treasury` (onglets)

### Onglet Position
- Cartes par banque : nom, numéro de compte, code du compte de bilan lié (1010, 1020…), solde.
- Carte totale (somme des soldes).
- Bouton refresh.

### Onglet Operations
- Sélecteur de type : Transfer / Expense / Deposit / Withdrawal.
- Champs dynamiques selon le type :
  - Transfer : banque source + banque destination, montant, description.
  - Expense : banque + compte de charge (liste des comptes `accountType = EXPENSE` via `GET /api/accounts`), montant, description.
  - Deposit / Withdrawal : banque, montant, description.
- Erreurs inline (le backend répond en 400 avec un message texte).
- Succès → rafraîchissement de Position et Movements.

### Onglet Movements
- Tableau : date, type (badge), banque, montant, description, id d'écriture liée.
- Filtre par banque (select).

### Onglet Banks
- Tableau : nom, numéro, compte lié.
- Formulaire "New Bank" inline (nom + numéro). Pas de suppression (aucun endpoint DELETE backend).

## Journaux — page `/journal` (remplacée)

- Barre d'onglets des 6 journaux avec label français, compteur d'écritures et totaux débit/crédit (`GET /api/journals`).
- Détail du journal sélectionné : tableau des écritures (n°, date, description) avec lignes dépliables (compte, débit, crédit) + totaux ; filtre de dates from/to (défaut : tout) → `GET /api/journals/{code}?from=&to=`.
- Bouton "New Entry" → `CreateJournalEntryPage` enrichie.

## Navigation

- `Layout.tsx` : ajout des entrées de menu "Treasury" et "Journals" à côté des sections existantes.

## Fichiers

Créés :
- `frontend/src/features/treasury/api.ts` — types (BankAccount, CashMovement, TreasuryPosition…) et fonctions typées.
- `frontend/src/features/treasury/TreasuryPage.tsx` — onglets + état.
- `frontend/src/features/treasury/PositionTab.tsx`
- `frontend/src/features/treasury/OperationsTab.tsx`
- `frontend/src/features/treasury/MovementsTab.tsx`
- `frontend/src/features/treasury/BanksTab.tsx`
- `frontend/src/features/journal/AuxiliaryJournalsPage.tsx`

Modifiés :
- `frontend/src/App.tsx` — route `/treasury` ; `/journal` → `AuxiliaryJournalsPage` ; suppression de `JournalEntryListPage`.
- `frontend/src/components/Layout.tsx` — entrées de menu.
- `frontend/src/features/journal/CreateJournalEntryPage.tsx` — sélecteur `journalCode`.
- `frontend/src/features/journal/JournalEntryListPage.tsx` — supprimé.

## Vérification

- Pas de framework de test frontend : `npm run build` (typecheck TS) + vérification visuelle utilisateur.
- Vérification API via curl sur le gateway (8082).
- Dépendance runtime : le frontend appelle le gateway (baseURL `http://localhost:8082`) → le gateway doit être relancé (il est actuellement arrêté).

## Hors périmètre

- Suppression/modification de comptes bancaires (pas d'endpoint backend).
- Réconciliation et prévision de trésorerie (briques backend futures).
- Export PDF des journaux.
