import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface Invoice { id: number; invoiceNumber: string; customerId: number; customerName: string; totalAmount: number; status: string; dueDate: string | null }

const statusBadge: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  PAID: 'bg-green-100 text-green-800',
  OVERDUE: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
}

const selectClass = 'border border-gray-300 rounded-lg px-2 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white'

export default function InvoiceListPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [draft, setDraft] = useState({ status: '', dueDate: '' })
  const [saving, setSaving] = useState(false)

  const load = () => api.get('/api/invoices')
    .then((res) => setInvoices(res.data.content || res.data))
    .catch(console.error)

  useEffect(() => {
    load().finally(() => setLoading(false))
  }, [])

  const startEdit = (inv: Invoice) => {
    setEditingId(inv.id)
    setDraft({ status: inv.status, dueDate: inv.dueDate ? inv.dueDate.slice(0, 10) : '' })
  }

  const cancelEdit = () => {
    setEditingId(null)
    setDraft({ status: '', dueDate: '' })
  }

  const saveEdit = async () => {
    if (editingId === null) return
    setSaving(true)
    try {
      await api.patch(`/api/invoices/${editingId}`, {
        status: draft.status,
        dueDate: draft.dueDate ? `${draft.dueDate}T00:00:00` : null,
      })
      cancelEdit()
      load()
    } catch (err) {
      console.error(err)
      alert('Failed to update invoice')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-12">
      <svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>
      <span className="ml-3 text-gray-500">Loading...</span>
    </div>
  )

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-pink-100 rounded-full flex items-center justify-center">
            <svg className="w-5 h-5 text-pink-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Invoices</h1>
            <p className="text-sm text-gray-500">{invoices.length} invoices</p>
          </div>
        </div>
        <Link to="/invoices/new" className="bg-blue-600 text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 transition flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          New Invoice
        </Link>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 text-left">
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Invoice #</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Customer</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Amount</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Status</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Due Date</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {invoices.map((inv) => (
              <tr key={inv.id} className="hover:bg-gray-50 transition">
                <td className="px-4 py-3 font-mono text-sm text-gray-600">{inv.invoiceNumber}</td>
                <td className="px-4 py-3 font-medium text-gray-900">{inv.customerName}</td>
                <td className="px-4 py-3 font-mono text-sm text-gray-900">${inv.totalAmount}</td>
                <td className="px-4 py-3">
                  {editingId === inv.id ? (
                    <select value={draft.status} onChange={(e) => setDraft({ ...draft, status: e.target.value })} className={selectClass}>
                      <option>PENDING</option><option>PAID</option><option>CANCELLED</option>
                    </select>
                  ) : (
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusBadge[inv.status] || 'bg-gray-100 text-gray-800'}`}>{inv.status}</span>
                  )}
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">
                  {editingId === inv.id ? (
                    <input type="date" value={draft.dueDate} onChange={(e) => setDraft({ ...draft, dueDate: e.target.value })} className={selectClass} />
                  ) : (
                    inv.dueDate ? new Date(inv.dueDate).toLocaleDateString() : '—'
                  )}
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  {editingId === inv.id ? (
                    <div className="flex justify-end gap-2">
                      <button onClick={saveEdit} disabled={saving} className="px-3 py-1.5 rounded-lg text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 transition disabled:opacity-50">
                        {saving ? 'Saving...' : 'Save'}
                      </button>
                      <button onClick={cancelEdit} className="px-3 py-1.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">Cancel</button>
                    </div>
                  ) : (
                    <div className="flex justify-end gap-2">
                      <a
                        href={`http://localhost:8082/api/invoices/${inv.id}/pdf`}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-100 transition"
                      >
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                        PDF
                      </a>
                      <button onClick={() => startEdit(inv)} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-blue-600 hover:bg-blue-50 transition">
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
                        Edit
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
            {invoices.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No invoices yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
