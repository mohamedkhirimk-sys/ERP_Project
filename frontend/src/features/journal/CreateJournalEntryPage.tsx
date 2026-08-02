import { useEffect, useState, type FormEvent } from 'react'
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

  const handleSubmit = async (e: FormEvent) => {
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
