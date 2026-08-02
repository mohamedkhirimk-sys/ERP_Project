import { useEffect, useState } from 'react'
import { type CashMovement, getMovements } from './api'
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
