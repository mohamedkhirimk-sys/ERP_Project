import { useState } from 'react'
import type { FormEvent } from 'react'
import { type BankAccount, createBank } from './api'

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

export default function BanksTab({ banks, refresh }: { banks: BankAccount[]; refresh: () => void }) {
  const [name, setName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
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
