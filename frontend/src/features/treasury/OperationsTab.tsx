import { useEffect, useState } from 'react'
import api from '@/lib/axios'
import { type BankAccount, createDeposit, createExpense, createTransfer, createWithdrawal } from './api'

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
      let movement: { id: number }
      if (type === 'Transfer') {
        if (!fromBank || !toBank) throw new Error('Select both banks')
        movement = await createTransfer({ fromBankAccountId: Number(fromBank), toBankAccountId: Number(toBank), amount: amountNum, description: desc })
      } else if (type === 'Expense') {
        if (!bankId || !expenseAccountId) throw new Error('Select a bank and an expense account')
        movement = await createExpense({ bankAccountId: Number(bankId), expenseAccountId: Number(expenseAccountId), amount: amountNum, description: desc })
      } else if (type === 'Deposit') {
        if (!bankId) throw new Error('Select a bank')
        movement = await createDeposit({ bankAccountId: Number(bankId), amount: amountNum, description: desc })
      } else {
        if (!bankId) throw new Error('Select a bank')
        movement = await createWithdrawal({ bankAccountId: Number(bankId), amount: amountNum, description: desc })
      }
      setSuccess(`${type} #${movement.id} of $${amountNum.toFixed(2)} recorded`)
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
