# Frontend Trésorerie & Journaux auxiliaires — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter au frontend la page Trésorerie (onglets Position/Operations/Movements/Banks) et remplacer `/journal` par la page Journaux auxiliaires (6 journaux + filtre de dates), en réparant au passage le formulaire d'écriture manuelle.

**Architecture:** Option A validée — pages React avec appels axios directs (pattern existant), module partagé typé unique `features/treasury/api.ts` (4 onglets partagent types + endpoints). Journaux : page autonome avec interfaces inline. Routes dans `App.tsx`, navigation dans `Layout.tsx`.

**Tech Stack:** React 19 + TypeScript, Vite, react-router-dom 7, Tailwind 4, axios (baseURL `http://localhost:8082` gateway). Aucune nouvelle dépendance. Pas de framework de test frontend : vérification = `npm run build` (tsc + vite) + `npm run lint` (oxlint) + vérification visuelle utilisateur.

## Global Constraints

- UI en **anglais** ; seuls les labels des journaux sont en français (fournis par le backend).
- Suivre le pattern des pages existantes : en-tête icône + titre + compteur, tableau dans une carte blanche `rounded-xl shadow-sm border border-gray-200`, spinner de chargement, état vide, montants préfixés `$`.
- Imports axios via `import api from '@/lib/axios'` uniquement.
- Backend répond 400 avec un **message texte** → l'afficher tel quel dans les erreurs de formulaire.
- Pas de test unitaire frontend : chaque tâche se vérifie par `npm run build` (attend BUILD SUCCESS).

## Contrats API backend (fiables, déjà livrés)

- `GET /api/bank-accounts` → `BankAccount[]`
- `POST /api/bank-accounts` `{name, accountNumber}` → `BankAccount` (400 texte : "Bank account with number ... already exists", "Bank name is required"...)
- `GET /api/treasury/position` → `{banks: BankBalance[], total}`
- `GET /api/treasury/movements?bankAccountId=` → `CashMovement[]`
- `POST /api/treasury/transfers` `{fromBankAccountId, toBankAccountId, amount, description}` → `CashMovement`
- `POST /api/treasury/expenses` `{bankAccountId, expenseAccountId, amount, description}` → `CashMovement`
- `POST /api/treasury/deposits` `{bankAccountId, amount, description}` → `CashMovement`
- `POST /api/treasury/withdrawals` `{bankAccountId, amount, description}` → `CashMovement`
- `GET /api/journals` → `JournalSummary[]` (6 éléments : code, label, entryCount, totalDebit, totalCredit)
- `GET /api/journals/{code}?from=YYYY-MM-DD&to=YYYY-MM-DD` → `JournalDetail` (400 texte : "Unknown journal code: XXX")
- `GET /api/accounts` → page Spring (`res.data.content` = `Account[]`, champs id/accountCode/accountName/accountType)
- `POST /api/journal-entries` `{description, journalCode, lines:[{accountId, debit, credit}]}` → `JournalEntryResponse`

## Structure des fichiers

- Créer `frontend/src/features/treasury/api.ts` — types + fonctions typées (seul fichier partagé).
- Créer `frontend/src/features/treasury/TreasuryPage.tsx` — coquille à onglets + état partagé (`banks`, `refreshKey`).
- Créer `frontend/src/features/treasury/PositionTab.tsx`, `OperationsTab.tsx`, `MovementsTab.tsx`, `BanksTab.tsx`.
- Créer `frontend/src/features/journal/AuxiliaryJournalsPage.tsx` — autonome (interfaces + axios inline).
- Modifier `frontend/src/App.tsx`, `frontend/src/components/Layout.tsx`, `frontend/src/features/journal/CreateJournalEntryPage.tsx`.
- Supprimer `frontend/src/features/journal/JournalEntryListPage.tsx`.

---

### Task 1: Module API trésorerie (`features/treasury/api.ts`)

**Files:**
- Create: `frontend/src/features/treasury/api.ts`

**Interfaces:**
- Consumes: `api` de `@/lib/axios`
- Produces (utilisé par Tasks 2-5) :
  - Types `BankAccount`, `BankBalance`, `TreasuryPosition`, `CashMovement`
  - `getBanks(): Promise<BankAccount[]>`
  - `createBank(req: {name: string; accountNumber: string}): Promise<BankAccount>`
  - `getPosition(): Promise<TreasuryPosition>`
  - `getMovements(bankAccountId: number | null): Promise<CashMovement[]>`
  - `createTransfer(req): Promise<CashMovement>` ; `createExpense(req)` ; `createDeposit(req)` ; `createWithdrawal(req)`

- [ ] **Step 1: Écrire le fichier complet**

```ts
import api from '@/lib/axios'
import axios from 'axios'

export interface BankAccount {
  id: number
  name: string
  accountNumber: string
  accountId: number
  accountCode: string
}

export interface BankBalance {
  bankId: number
  bankName: string
  accountNumber: string
  accountCode: string
  balance: number
}

export interface TreasuryPosition {
  banks: BankBalance[]
  total: number
}

export interface CashMovement {
  id: number
  movementType: 'TRANSFER' | 'EXPENSE' | 'DEPOSIT' | 'WITHDRAWAL'
  bankAccountId: number | null
  toBankAccountId: number | null
  amount: number
  description: string | null
  entryId: number | null
  createdAt: string | null
}

async function toError(err: unknown): Promise<never> {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data
    throw new Error(typeof data === 'string' && data ? data : `Request failed (${err.response?.status ?? 'network'})`)
  }
  throw err instanceof Error ? err : new Error('Request failed')
}

export async function getBanks(): Promise<BankAccount[]> {
  try {
    const res = await api.get('/api/bank-accounts')
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function createBank(req: { name: string; accountNumber: string }): Promise<BankAccount> {
  try {
    const res = await api.post('/api/bank-accounts', req)
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function getPosition(): Promise<TreasuryPosition> {
  try {
    const res = await api.get('/api/treasury/position')
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function getMovements(bankAccountId: number | null): Promise<CashMovement[]> {
  try {
    const res = await api.get('/api/treasury/movements', { params: bankAccountId ? { bankAccountId } : {} })
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function createTransfer(req: {
  fromBankAccountId: number
  toBankAccountId: number
  amount: number
  description: string | null
}): Promise<CashMovement> {
  try {
    const res = await api.post('/api/treasury/transfers', req)
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function createExpense(req: {
  bankAccountId: number
  expenseAccountId: number
  amount: number
  description: string | null
}): Promise<CashMovement> {
  try {
    const res = await api.post('/api/treasury/expenses', req)
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function createDeposit(req: {
  bankAccountId: number
  amount: number
  description: string | null
}): Promise<CashMovement> {
  try {
    const res = await api.post('/api/treasury/deposits', req)
    return res.data
  } catch (err) {
    return toError(err)
  }
}

export async function createWithdrawal(req: {
  bankAccountId: number
  amount: number
  description: string | null
}): Promise<CashMovement> {
  try {
    const res = await api.post('/api/treasury/withdrawals', req)
    return res.data
  } catch (err) {
    return toError(err)
  }
}
```

- [ ] **Step 2: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS` (tsc + vite)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/features/treasury/api.ts
git commit -m "feat: treasury api module with typed functions"
```

---

### Task 2: Coquille TreasuryPage + onglet Position + route + menu

**Files:**
- Create: `frontend/src/features/treasury/TreasuryPage.tsx`
- Create: `frontend/src/features/treasury/PositionTab.tsx`
- Modify: `frontend/src/App.tsx` (import + route `/treasury`)
- Modify: `frontend/src/components/Layout.tsx` (entrée menu "Treasury")

**Interfaces:**
- Consumes: `getBanks`, `getPosition`, `BankAccount`, `TreasuryPosition` (Task 1)
- Produces:
  - `TreasuryPage` : état partagé `banks: BankAccount[]`, `refreshKey: number`, `refresh(): void`, `reloadBanks(): void` ; rend les 4 onglets avec les props suivantes :
    - `PositionTab({ refreshKey }: { refreshKey: number })`
    - `OperationsTab({ banks, refresh }: { banks: BankAccount[]; refresh: () => void })` (Task 3)
    - `MovementsTab({ banks, refreshKey }: { banks: BankAccount[]; refreshKey: number })` (Task 4)
    - `BanksTab({ banks, refresh }: { banks: BankAccount[]; refresh: () => void })` (Task 5)

- [ ] **Step 1: Créer `TreasuryPage.tsx`**

```tsx
import { useCallback, useEffect, useState } from 'react'
import { BankAccount, getBanks } from './api'
import PositionTab from './PositionTab'
import OperationsTab from './OperationsTab'
import MovementsTab from './MovementsTab'
import BanksTab from './BanksTab'

const tabs = [
  { key: 'position', label: 'Position' },
  { key: 'operations', label: 'Operations' },
  { key: 'movements', label: 'Movements' },
  { key: 'banks', label: 'Banks' },
]

export default function TreasuryPage() {
  const [tab, setTab] = useState('position')
  const [banks, setBanks] = useState<BankAccount[]>([])
  const [refreshKey, setRefreshKey] = useState(0)

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  useEffect(() => {
    getBanks().then(setBanks).catch(console.error)
  }, [refreshKey])

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Treasury</h1>
          <p className="text-sm text-gray-500">Bank accounts, operations and cash position</p>
        </div>
      </div>

      <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-6 w-fit">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition ${
              tab === t.key ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'position' && <PositionTab refreshKey={refreshKey} />}
      {tab === 'operations' && <OperationsTab banks={banks} refresh={refresh} />}
      {tab === 'movements' && <MovementsTab banks={banks} refreshKey={refreshKey} />}
      {tab === 'banks' && <BanksTab banks={banks} refresh={refresh} />}
    </div>
  )
}
```

- [ ] **Step 2: Créer `PositionTab.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { getPosition, TreasuryPosition } from './api'

export default function PositionTab({ refreshKey }: { refreshKey: number }) {
  const [position, setPosition] = useState<TreasuryPosition | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getPosition()
      .then(setPosition)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [refreshKey])

  if (loading) return <p className="text-gray-500 py-12 text-center">Loading...</p>
  if (!position || position.banks.length === 0) {
    return <p className="text-gray-500 py-12 text-center">No bank accounts yet — create one in the Banks tab</p>
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {position.banks.map((b) => (
        <div key={b.bankId} className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-semibold text-gray-900">{b.bankName}</p>
              <p className="text-xs text-gray-500 font-mono">{b.accountNumber}</p>
            </div>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
              {b.accountCode}
            </span>
          </div>
          <p className="mt-4 text-2xl font-bold text-gray-900">${b.balance.toFixed(2)}</p>
        </div>
      ))}
      <div className="bg-blue-600 rounded-xl shadow-sm p-5 text-white">
        <p className="text-sm font-medium opacity-80">Total cash</p>
        <p className="mt-4 text-2xl font-bold">${position.total.toFixed(2)}</p>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Modifier `App.tsx`** — ajouter après la ligne 18 (`import CreateJournalEntryPage...`) :

```tsx
import TreasuryPage from '@/features/treasury/TreasuryPage'
```

Et dans `<Routes>`, après la ligne 61 (`<Route path="/journal/new" .../>`) :

```tsx
<Route path="/treasury" element={<TreasuryPage />} />
```

- [ ] **Step 4: Modifier `Layout.tsx`** — dans `navItems`, après l'entrée `Journal` (ligne 15) :

```tsx
{ label: 'Treasury', path: '/treasury', icon: 'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z' },
```

- [ ] **Step 5: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/treasury/TreasuryPage.tsx frontend/src/features/treasury/PositionTab.tsx frontend/src/App.tsx frontend/src/components/Layout.tsx
git commit -m "feat: treasury page with position tab, route and menu entry"
```

---

### Task 3: Onglet Operations

**Files:**
- Create: `frontend/src/features/treasury/OperationsTab.tsx`

**Interfaces:**
- Consumes: `createTransfer`, `createExpense`, `createDeposit`, `createWithdrawal`, `BankAccount` (Task 1) ; props `{ banks, refresh }` (Task 2)
- Produces: rien (utilisé par `TreasuryPage`)

- [ ] **Step 1: Créer `OperationsTab.tsx`**

```tsx
import { useEffect, useState } from 'react'
import api from '@/lib/axios'
import { BankAccount, createDeposit, createExpense, createTransfer, createWithdrawal } from './api'

interface Account {
  id: number
  accountCode: string
  accountName: string
  accountType: string
}

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'
const types = ['Transfer', 'Expense', 'Deposit', 'Withdrawal'] as const
type OpType = (typeof types)[number]

export default function OperationsTab({ banks, refresh }: { banks: BankAccount[]; refresh: () => void }) {
  const [type, setType] = useState<OpType>('Transfer')
  const [fromBank, setFromBank] = useState('')
  const [toBank, setToBank] = useState('')
  const [bankId, setBankId] = useState('')
  const [expenseAccountId, setExpenseAccountId] = useState('')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [expenseAccounts, setExpenseAccounts] = useState<Account[]>([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (type !== 'Expense') return
    api.get('/api/accounts')
      .then((res) => {
        const all: Account[] = res.data.content || res.data
        setExpenseAccounts(all.filter((a) => a.accountType === 'EXPENSE'))
      })
      .catch(console.error)
  }, [type])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    const amountNum = Number(amount)
    if (!amount || amountNum <= 0) {
      setError('Amount must be positive')
      return
    }
    setSubmitting(true)
    try {
      const desc = description || null
      if (type === 'Transfer') {
        if (!fromBank || !toBank) throw new Error('Select both banks')
        await createTransfer({ fromBankAccountId: Number(fromBank), toBankAccountId: Number(toBank), amount: amountNum, description: desc })
      } else if (type === 'Expense') {
        if (!bankId || !expenseAccountId) throw new Error('Select a bank and an expense account')
        await createExpense({ bankAccountId: Number(bankId), expenseAccountId: Number(expenseAccountId), amount: amountNum, description: desc })
      } else if (type === 'Deposit') {
        if (!bankId) throw new Error('Select a bank')
        await createDeposit({ bankAccountId: Number(bankId), amount: amountNum, description: desc })
      } else {
        if (!bankId) throw new Error('Select a bank')
        await createWithdrawal({ bankAccountId: Number(bankId), amount: amountNum, description: desc })
      }
      setSuccess(`${type} of $${amountNum.toFixed(2)} recorded`)
      setAmount('')
      setDescription('')
      refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-xl">
      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Operation type</label>
          <div className="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit">
            {types.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={`px-4 py-2 rounded-md text-sm font-medium transition ${
                  type === t ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        {type === 'Transfer' && (
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">From bank</label>
              <select value={fromBank} onChange={(e) => setFromBank(e.target.value)} className={inputClass}>
                <option value="">Select...</option>
                {banks.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">To bank</label>
              <select value={toBank} onChange={(e) => setToBank(e.target.value)} className={inputClass}>
                <option value="">Select...</option>
                {banks.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </div>
          </div>
        )}

        {(type === 'Expense' || type === 'Deposit' || type === 'Withdrawal') && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Bank</label>
            <select value={bankId} onChange={(e) => setBankId(e.target.value)} className={inputClass}>
              <option value="">Select...</option>
              {banks.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </select>
          </div>
        )}

        {type === 'Expense' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Expense account</label>
            <select value={expenseAccountId} onChange={(e) => setExpenseAccountId(e.target.value)} className={inputClass}>
              <option value="">Select...</option>
              {expenseAccounts.map((a) => <option key={a.id} value={a.id}>{a.accountCode} — {a.accountName}</option>)}
            </select>
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Amount</label>
            <input type="number" step="0.01" min="0" value={amount} onChange={(e) => setAmount(e.target.value)} className={inputClass} placeholder="0.00" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <input value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} placeholder="Optional" />
          </div>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">{error}</p>}
        {success && <p className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg px-3 py-2">{success}</p>}

        <button type="submit" disabled={submitting} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition">
          {submitting ? 'Recording...' : 'Record operation'}
        </button>
      </form>
    </div>
  )
}
```

- [ ] **Step 2: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/features/treasury/OperationsTab.tsx
git commit -m "feat: treasury operations tab"
```

---

### Task 4: Onglet Movements

**Files:**
- Create: `frontend/src/features/treasury/MovementsTab.tsx`

**Interfaces:**
- Consumes: `getMovements`, `CashMovement`, `BankAccount` (Task 1) ; props `{ banks, refreshKey }` (Task 2)

- [ ] **Step 1: Créer `MovementsTab.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { CashMovement, getMovements } from './api'
import type { BankAccount } from './api'

const typeStyles: Record<string, string> = {
  TRANSFER: 'bg-blue-100 text-blue-800',
  EXPENSE: 'bg-red-100 text-red-800',
  DEPOSIT: 'bg-green-100 text-green-800',
  WITHDRAWAL: 'bg-yellow-100 text-yellow-800',
}

export default function MovementsTab({ banks, refreshKey }: { banks: BankAccount[]; refreshKey: number }) {
  const [movements, setMovements] = useState<CashMovement[]>([])
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getMovements(filter ? Number(filter) : null)
      .then(setMovements)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [refreshKey, filter])

  const bankName = (id: number | null) => (id == null ? '—' : banks.find((b) => b.id === id)?.name ?? `#${id}`)

  if (loading) return <p className="text-gray-500 py-12 text-center">Loading...</p>

  return (
    <div>
      <div className="mb-4 w-56">
        <select value={filter} onChange={(e) => setFilter(e.target.value)} className="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition">
          <option value="">All banks</option>
          {banks.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 text-left">
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Date</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Type</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Bank</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Amount</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Description</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Entry</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {movements.map((m) => (
              <tr key={m.id} className="hover:bg-gray-50 transition">
                <td className="px-4 py-3 text-sm text-gray-600">{m.createdAt ? new Date(m.createdAt).toLocaleString() : '—'}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${typeStyles[m.movementType] || 'bg-gray-100 text-gray-800'}`}>{m.movementType}</span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-700">{bankName(m.bankAccountId)}</td>
                <td className="px-4 py-3 font-mono text-sm text-gray-900">${m.amount.toFixed(2)}</td>
                <td className="px-4 py-3 text-sm text-gray-600">{m.description || '—'}</td>
                <td className="px-4 py-3 font-mono text-sm text-gray-500">{m.entryId ?? '—'}</td>
              </tr>
            ))}
            {movements.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No movements yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/features/treasury/MovementsTab.tsx
git commit -m "feat: treasury movements tab with bank filter"
```

---

### Task 5: Onglet Banks

**Files:**
- Create: `frontend/src/features/treasury/BanksTab.tsx`

**Interfaces:**
- Consumes: `createBank`, `BankAccount` (Task 1) ; props `{ banks, refresh }` (Task 2)

- [ ] **Step 1: Créer `BanksTab.tsx`**

```tsx
import { useState } from 'react'
import { BankAccount, createBank } from './api'

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

export default function BanksTab({ banks, refresh }: { banks: BankAccount[]; refresh: () => void }) {
  const [name, setName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await createBank({ name, accountNumber })
      setName('')
      setAccountNumber('')
      refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 max-w-xl">
        <h2 className="text-sm font-semibold text-gray-900 mb-4">New bank account</h2>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Bank name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} className={inputClass} placeholder="e.g. BNP" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Account number</label>
            <input value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} className={inputClass} placeholder="e.g. FR300001" required />
          </div>
        </div>
        {error && <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2 mb-4">{error}</p>}
        <button type="submit" disabled={saving} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition">
          {saving ? 'Creating...' : 'Create bank account'}
        </button>
      </form>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 text-left">
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Name</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Account number</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Linked account</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {banks.map((b) => (
              <tr key={b.id} className="hover:bg-gray-50 transition">
                <td className="px-4 py-3 font-medium text-gray-900">{b.name}</td>
                <td className="px-4 py-3 font-mono text-sm text-gray-600">{b.accountNumber}</td>
                <td className="px-4 py-3">
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">{b.accountCode}</span>
                </td>
              </tr>
            ))}
            {banks.length === 0 && (
              <tr><td colSpan={3} className="px-4 py-8 text-center text-gray-500">No bank accounts yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/features/treasury/BanksTab.tsx
git commit -m "feat: treasury banks tab"
```

---

### Task 6: Page Journaux auxiliaires

**Files:**
- Create: `frontend/src/features/journal/AuxiliaryJournalsPage.tsx`

**Interfaces:**
- Consumes: `api` de `@/lib/axios`
- Produces: page autonome montée sur `/journal` (Task 8)

- [ ] **Step 1: Créer `AuxiliaryJournalsPage.tsx`**

```tsx
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface JournalSummary {
  code: string
  label: string
  entryCount: number
  totalDebit: number
  totalCredit: number
}

interface JournalLine {
  id: number
  accountId: number
  accountName: string
  accountCode: string
  debit: number
  credit: number
}

interface JournalEntryDetail {
  id: number
  entryNumber: string
  description: string
  entryDate: string
  lines: JournalLine[]
}

interface JournalDetail {
  code: string
  label: string
  entries: JournalEntryDetail[]
  totalDebit: number
  totalCredit: number
}

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

export default function AuxiliaryJournalsPage() {
  const [summaries, setSummaries] = useState<JournalSummary[]>([])
  const [selected, setSelected] = useState('VTE')
  const [detail, setDetail] = useState<JournalDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [expanded, setExpanded] = useState<number | null>(null)

  useEffect(() => {
    api.get('/api/journals')
      .then((res) => setSummaries(res.data))
      .catch(console.error)
  }, [])

  useEffect(() => {
    setLoading(true)
    api.get(`/api/journals/${selected}`, { params: { from: from || undefined, to: to || undefined } })
      .then((res) => setDetail(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [selected, from, to])

  const current = summaries.find((s) => s.code === selected)

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
            <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Journals</h1>
            <p className="text-sm text-gray-500">Auxiliary journals — VTE, ENC, ACH, DEC, BNQ, OD</p>
          </div>
        </div>
        <Link to="/journal/new" className="bg-blue-600 text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 transition flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          New Entry
        </Link>
      </div>

      <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-4 flex-wrap w-fit">
        {summaries.map((s) => (
          <button
            key={s.code}
            onClick={() => setSelected(s.code)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition ${
              selected === s.code ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {s.code} · {s.entryCount}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-4 mb-4">
        <div className="w-48">
          <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">From</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={inputClass} />
        </div>
        <div className="w-48">
          <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">To</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className={inputClass} />
        </div>
        {current && (
          <p className="text-sm text-gray-500 ml-auto">
            <span className="font-semibold text-gray-900">{current.label}</span> — {detail?.entries.length ?? 0} entries
          </p>
        )}
      </div>

      {loading ? (
        <p className="text-gray-500 py-12 text-center">Loading...</p>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50 text-left">
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Entry #</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Date</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Description</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-right">Debit</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-right">Credit</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {detail?.entries.map((e) => {
                const isOpen = expanded === e.id
                const totalDebit = e.lines.reduce((s, l) => s + l.debit, 0)
                const totalCredit = e.lines.reduce((s, l) => s + l.credit, 0)
                return (
                  <>
                    <tr key={e.id} onClick={() => setExpanded(isOpen ? null : e.id)} className="hover:bg-gray-50 transition cursor-pointer">
                      <td className="px-4 py-3 font-mono text-sm text-gray-600">{e.entryNumber}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{new Date(e.entryDate).toLocaleString()}</td>
                      <td className="px-4 py-3 text-sm text-gray-900">{e.description}</td>
                      <td className="px-4 py-3 font-mono text-sm text-gray-900 text-right">{totalDebit > 0 ? `$${totalDebit.toFixed(2)}` : '—'}</td>
                      <td className="px-4 py-3 font-mono text-sm text-gray-900 text-right">{totalCredit > 0 ? `$${totalCredit.toFixed(2)}` : '—'}</td>
                    </tr>
                    {isOpen && (
                      <tr key={`${e.id}-lines`}>
                        <td colSpan={5} className="px-6 pb-3 bg-gray-50/50">
                          <table className="w-full text-sm">
                            <tbody className="divide-y divide-gray-100">
                              {e.lines.map((l) => (
                                <tr key={l.id}>
                                  <td className="py-1.5 px-2 font-mono text-gray-600 w-32">{l.accountCode}</td>
                                  <td className="py-1.5 px-2 text-gray-700">{l.accountName}</td>
                                  <td className="py-1.5 px-2 font-mono text-right w-32">{l.debit > 0 ? `$${l.debit.toFixed(2)}` : '—'}</td>
                                  <td className="py-1.5 px-2 font-mono text-right w-32">{l.credit > 0 ? `$${l.credit.toFixed(2)}` : '—'}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </>
                )
              })}
              {detail && detail.entries.length === 0 && (
                <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-500">No entries in this journal{from || to ? ' for the selected period' : ''}</td></tr>
              )}
            </tbody>
            {detail && detail.entries.length > 0 && (
              <tfoot>
                <tr className="border-t border-gray-200 bg-gray-50">
                  <td colSpan={3} className="px-4 py-3 text-right text-sm font-semibold text-gray-700">Journal totals</td>
                  <td className="px-4 py-3 text-right font-mono text-sm font-semibold text-gray-900">${detail.totalDebit.toFixed(2)}</td>
                  <td className="px-4 py-3 text-right font-mono text-sm font-semibold text-gray-900">${detail.totalCredit.toFixed(2)}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      )}
    </div>
  )
}
```

Note : si la ligne `<td colSpan={5} ...>` de l'expansion produit un warning React (fragment avec `key` sur `<tr>` dans un fragment sans key), remplacer le fragment `<>...</>` par `<Fragment key={e.id}>...</Fragment>` (import `Fragment` de react).

- [ ] **Step 2: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/features/journal/AuxiliaryJournalsPage.tsx
git commit -m "feat: auxiliary journals page with 6 journals and date filter"
```

---

### Task 7: Réparer et enrichir le formulaire d'écriture manuelle

**Files:**
- Modify: `frontend/src/features/journal/CreateJournalEntryPage.tsx`

**Interfaces:**
- Consumes: `api` de `@/lib/axios` (GET `/api/accounts` paginé, POST `/api/journal-entries` avec `journalCode`)
- Produces: formulaire compatible backend : payload `{description, journalCode, lines: [{accountId, debit, credit}]}`

Note : le formulaire actuel envoie `entryDate`, `accountCode` et une description par ligne — incompatibles avec le backend (`JournalEntryLineRequest` attend `accountId`). La création manuelle échoue donc actuellement avec un 400. Cette tâche la répare ET ajoute le sélecteur de journal.

- [ ] **Step 1: Remplacer le contenu du fichier**

```tsx
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import api from '@/lib/axios'

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

const journals = [
  { code: 'VTE', label: 'Journal des ventes' },
  { code: 'ENC', label: 'Journal des encaissements' },
  { code: 'ACH', label: 'Journal des achats' },
  { code: 'DEC', label: 'Journal des décaissements' },
  { code: 'BNQ', label: 'Journal de banque' },
  { code: 'OD', label: 'Journal des opérations diverses' },
]

interface Account {
  id: number
  accountCode: string
  accountName: string
}

interface LineItem {
  accountId: string
  debit: string
  credit: string
}

export default function CreateJournalEntryPage() {
  const navigate = useNavigate()
  const [journalCode, setJournalCode] = useState('OD')
  const [description, setDescription] = useState('')
  const [accounts, setAccounts] = useState<Account[]>([])
  const [lines, setLines] = useState<LineItem[]>([{ accountId: '', debit: '', credit: '' }])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/api/accounts')
      .then((res) => setAccounts(res.data.content || res.data))
      .catch(console.error)
  }, [])

  const totalDebit = lines.reduce((s, l) => s + (Number(l.debit) || 0), 0)
  const totalCredit = lines.reduce((s, l) => s + (Number(l.credit) || 0), 0)
  const isBalanced = Math.abs(totalDebit - totalCredit) < 0.01

  const updateLine = (index: number, field: keyof LineItem, value: string) => {
    const updated = [...lines]
    updated[index] = { ...updated[index], [field]: value }
    setLines(updated)
  }

  const addLine = () => setLines([...lines, { accountId: '', debit: '', credit: '' }])
  const removeLine = (index: number) => lines.length > 1 && setLines(lines.filter((_, i) => i !== index))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isBalanced) { alert('Debit and Credit must be equal'); return }
    setSaving(true)
    try {
      await api.post('/api/journal-entries', {
        description,
        journalCode,
        lines: lines.filter((l) => l.accountId.trim()).map((l) => ({
          accountId: Number(l.accountId),
          debit: Number(l.debit) || 0,
          credit: Number(l.credit) || 0,
        })),
      })
      navigate('/journal')
    } catch (err) {
      const message =
        axios.isAxiosError(err) && typeof err.response?.data === 'string'
          ? err.response.data
          : 'Failed to create journal entry'
      alert(message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Create Journal Entry</h1>
          <p className="text-sm text-gray-500">Record a new journal entry with debits and credits</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Journal</label>
            <select value={journalCode} onChange={(e) => setJournalCode(e.target.value)} className={inputClass}>
              {journals.map((j) => <option key={j.code} value={j.code}>{j.code} — {j.label}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <input value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} placeholder="Brief description of the entry" required />
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-3">
            <label className="block text-sm font-medium text-gray-700">Journal Lines</label>
            <button type="button" onClick={addLine} className="text-blue-600 text-sm font-medium hover:underline flex items-center gap-1">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
              Add Line
            </button>
          </div>
          <div className="bg-gray-50 rounded-lg border border-gray-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-100">
                  <th className="px-3 py-2 text-left text-xs font-semibold text-gray-600 uppercase">Account</th>
                  <th className="px-3 py-2 text-right text-xs font-semibold text-gray-600 uppercase">Debit</th>
                  <th className="px-3 py-2 text-right text-xs font-semibold text-gray-600 uppercase">Credit</th>
                  <th className="px-3 py-2 w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {lines.map((line, i) => (
                  <tr key={i}>
                    <td className="px-3 py-1.5">
                      <select value={line.accountId} onChange={(e) => updateLine(i, 'accountId', e.target.value)} className={inputClass} required>
                        <option value="">Select account...</option>
                        {accounts.map((a) => <option key={a.id} value={a.id}>{a.accountCode} — {a.accountName}</option>)}
                      </select>
                    </td>
                    <td className="px-3 py-1.5">
                      <input type="number" step="0.01" value={line.debit} onChange={(e) => updateLine(i, 'debit', e.target.value)} className={`${inputClass} text-right w-32`} placeholder="0.00" />
                    </td>
                    <td className="px-3 py-1.5">
                      <input type="number" step="0.01" value={line.credit} onChange={(e) => updateLine(i, 'credit', e.target.value)} className={`${inputClass} text-right w-32`} placeholder="0.00" />
                    </td>
                    <td className="px-3 py-1.5">
                      <button type="button" onClick={() => removeLine(i)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-md transition" title="Remove line">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t border-gray-200 bg-gray-100">
                  <td className="px-3 py-2 text-right text-sm font-semibold text-gray-700">Totals</td>
                  <td className={`px-3 py-2 text-right font-mono text-sm font-semibold ${totalDebit > 0 ? (isBalanced ? 'text-green-600' : 'text-red-600') : 'text-gray-700'}`}>
                    ${totalDebit.toFixed(2)}
                  </td>
                  <td className={`px-3 py-2 text-right font-mono text-sm font-semibold ${totalCredit > 0 ? (isBalanced ? 'text-green-600' : 'text-red-600') : 'text-gray-700'}`}>
                    ${totalCredit.toFixed(2)}
                  </td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>
          {!isBalanced && lines.some((l) => l.accountId.trim()) && (
            <p className="flex items-center gap-1 text-red-600 text-sm mt-2">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              Debits and credits must be equal
            </p>
          )}
        </div>

        <div className="flex items-center gap-3 pt-2">
          <button type="submit" disabled={saving || !isBalanced} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition flex items-center gap-2">
            {saving ? (
              <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Creating...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>Create Entry</>
            )}
          </button>
          <button type="button" onClick={() => navigate('/journal')} className="px-6 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
```

- [ ] **Step 2: Vérifier la compilation**

Run: `npm run build`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/features/journal/CreateJournalEntryPage.tsx
git commit -m "fix: journal entry form matches backend (accountId, journalCode) with journal selector"
```

---

### Task 8: Routage final, suppression de l'ancienne page, vérifications

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/Layout.tsx`
- Delete: `frontend/src/features/journal/JournalEntryListPage.tsx`

**Interfaces:**
- Consumes: `AuxiliaryJournalsPage` (Task 6), `TreasuryPage` (Task 2)

- [ ] **Step 1: Modifier `App.tsx`** — remplacer l'import `JournalEntryListPage` (ligne 25) par :

```tsx
import AuxiliaryJournalsPage from '@/features/journal/AuxiliaryJournalsPage'
```

Remplacer la route ligne 60 (`<Route path="/journal" element={<JournalEntryListPage />} />`) par :

```tsx
<Route path="/journal" element={<AuxiliaryJournalsPage />} />
```

- [ ] **Step 2: Modifier `Layout.tsx`** — remplacer le label de l'entrée `Journal` (ligne 15) par `Journals` (path inchangé) :

```tsx
{ label: 'Journals', path: '/journal', icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
```

- [ ] **Step 3: Supprimer l'ancienne page**

```bash
git rm frontend/src/features/journal/JournalEntryListPage.tsx
```

- [ ] **Step 4: Vérifier build + lint**

Run: `npm run build`
Expected: `BUILD SUCCESS`

Run: `npm run lint`
Expected: aucune erreur (ou uniquement des warnings existants)

- [ ] **Step 5: Fumée API via le gateway**

Le frontend appelle le gateway 8082 (actuellement arrêté). Le démarrer (via votre terminal, ou demander à l'exécutant de le lancer avec l'argfile gateway), puis :

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/treasury/position"
Invoke-RestMethod -Uri "http://localhost:8082/api/journals"
```

Expected: position avec les banques BNP/CIC existantes (solde 320.00/100.00, total 420.00) ; journaux avec les 6 codes et les écritures BNQ/DEC présentes.

Si le gateway est indisponible, vérifier directement sur finance 8098 et le noter.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/App.tsx frontend/src/components/Layout.tsx
git commit -m "feat: wire auxiliary journals page, remove legacy flat journal list"
```

---

## Self-review

- **Couverture spec** : Trésorerie 4 onglets (Tasks 2-5) ✓ ; journaux 6 onglets + filtre dates + lignes dépliables (Task 6) ✓ ; New Entry avec sélecteur journal (Task 7) ✓ ; navigation Treasury + Journals (Tasks 2/8) ✓ ; suppression ancienne vue (Task 8) ✓ ; gateway à relancer (Task 8 step 5) ✓.
- **Placeholders** : aucun TBD/TODO ; chaque fichier a son code complet.
- **Cohérence des types** : `getPosition/getBanks/getMovements/create*` et types `BankAccount/TreasuryPosition/CashMovement` définis Task 1, utilisés Tasks 2-5 avec les mêmes noms ; props d'onglets (`refreshKey`, `refresh`, `banks`) identiques entre Task 2 et Tasks 3-5.
