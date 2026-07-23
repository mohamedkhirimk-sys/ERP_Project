import { useState, useEffect, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

const labelClass = 'block text-sm font-medium text-gray-700 mb-1'
const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'
const selectClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white'

export default function AccountFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState({
    accountCode: '', accountName: '', accountType: 'ASSET', description: '', balance: '',
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (id) {
      api.get(`/api/accounts/${id}`).then((res) => {
        const a = res.data
        setForm({
          accountCode: a.accountCode, accountName: a.accountName,
          accountType: a.accountType, description: a.description || '', balance: String(a.balance),
        })
      }).catch(console.error)
    }
  }, [id])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const payload = { ...form, balance: Number(form.balance) }
      if (isEdit) {
        await api.put(`/api/accounts/${id}`, payload)
      } else {
        await api.post('/api/accounts', payload)
      }
      navigate('/accounts')
    } catch (err) {
      console.error(err)
      alert('Failed to save account')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{isEdit ? 'Edit Account' : 'New Account'}</h1>
          <p className="text-sm text-gray-500">{isEdit ? 'Update account details' : 'Add a new account to the chart of accounts'}</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className={labelClass}>Account Code</label>
            <input value={form.accountCode} onChange={(e) => setForm({ ...form, accountCode: e.target.value })} className={inputClass} placeholder="e.g. 1000" required />
          </div>
          <div>
            <label className={labelClass}>Type</label>
            <select value={form.accountType} onChange={(e) => setForm({ ...form, accountType: e.target.value })} className={selectClass}>
              <option>ASSET</option>
              <option>LIABILITY</option>
              <option>EQUITY</option>
              <option>REVENUE</option>
              <option>EXPENSE</option>
            </select>
          </div>
          <div className="col-span-2">
            <label className={labelClass}>Account Name</label>
            <input value={form.accountName} onChange={(e) => setForm({ ...form, accountName: e.target.value })} className={inputClass} placeholder="e.g. Cash & Bank" required />
          </div>
          <div className="col-span-2">
            <label className={labelClass}>Description</label>
            <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className={inputClass} rows={3} placeholder="Optional description" />
          </div>
          <div>
            <label className={labelClass}>Opening Balance</label>
            <input type="number" step="0.01" value={form.balance} onChange={(e) => setForm({ ...form, balance: e.target.value })} className={inputClass} placeholder="0.00" />
          </div>
        </div>

        <div className="flex items-center gap-3 pt-4 border-t">
          <button type="submit" disabled={loading} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition flex items-center gap-2">
            {loading ? (
              <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Saving...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>Save Account</>
            )}
          </button>
          <button type="button" onClick={() => navigate('/accounts')} className="px-6 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
