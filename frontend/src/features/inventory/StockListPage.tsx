import { useState, useEffect } from 'react'
import api from '@/lib/axios'

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

interface Stock { id: number; productSku: string; quantity: number; warehouseLocation: string }

export default function StockListPage() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ productSku: '', quantity: '', warehouseLocation: '' })

  const fetchStocks = () => api.get('/api/inventory/stocks').then((res) => setStocks(res.data.content || res.data))

  useEffect(() => { fetchStocks().finally(() => setLoading(false)) }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.post('/api/inventory/stock', {
      productSku: form.productSku, quantity: Number(form.quantity), warehouseLocation: form.warehouseLocation || undefined,
    })
    setForm({ productSku: '', quantity: '', warehouseLocation: '' })
    setShowForm(false)
    fetchStocks()
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
          <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
            <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Stock Levels</h1>
            <p className="text-sm text-gray-500">{stocks.length} stock records</p>
          </div>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="bg-blue-600 text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 transition flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          {showForm ? 'Cancel' : 'Initialize Stock'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-5 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Initialize Stock</h2>
          <div className="grid grid-cols-3 gap-3">
            <input value={form.productSku} onChange={(e) => setForm({ ...form, productSku: e.target.value })} placeholder="Product SKU" className={inputClass} required />
            <input type="number" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} placeholder="Quantity" className={inputClass} required />
            <input value={form.warehouseLocation} onChange={(e) => setForm({ ...form, warehouseLocation: e.target.value })} placeholder="Warehouse (optional)" className={inputClass} />
          </div>
          <button type="submit" className="mt-3 bg-green-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-green-700 transition">Save</button>
        </form>
      )}

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 text-left">
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">SKU</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Quantity</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Warehouse</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {stocks.map((s) => (
              <tr key={s.id} className="hover:bg-gray-50 transition">
                <td className="px-4 py-3 font-mono text-sm text-gray-700">{s.productSku}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${s.quantity > 10 ? 'bg-green-100 text-green-800' : s.quantity > 0 ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'}`}>
                    {s.quantity}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">{s.warehouseLocation || '—'}</td>
              </tr>
            ))}
            {stocks.length === 0 && (
              <tr><td colSpan={3} className="px-4 py-8 text-center text-gray-500">No stock records</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
