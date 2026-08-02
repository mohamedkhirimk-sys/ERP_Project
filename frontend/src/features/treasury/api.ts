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
  vaultBalance: number
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
