import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface Stock {
  id: number
  productSku: string
  quantity: number
  warehouseLocation: string
}

export default function StockListPage() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ productSku: '', quantity: '', warehouseLocation: '' })

  const fetchStocks = () =>
    api.get('/api/inventory/stocks').then((res) => setStocks(res.data))

  useEffect(() => { fetchStocks().finally(() => setLoading(false)) }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.post('/api/inventory/stock', {
      productSku: form.productSku,
      quantity: Number(form.quantity),
      warehouseLocation: form.warehouseLocation || undefined,
    })
    setForm({ productSku: '', quantity: '', warehouseLocation: '' })
    setShowForm(false)
    fetchStocks()
  }

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Stock Levels</h1>
        <button onClick={() => setShowForm(!showForm)} className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700">
          {showForm ? 'Cancel' : '+ Initialize Stock'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white p-4 rounded-lg border mb-4 grid grid-cols-3 gap-3">
          <input value={form.productSku} onChange={(e) => setForm({ ...form, productSku: e.target.value })} placeholder="Product SKU" className="border rounded px-3 py-2" required />
          <input type="number" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} placeholder="Quantity" className="border rounded px-3 py-2" required />
          <input value={form.warehouseLocation} onChange={(e) => setForm({ ...form, warehouseLocation: e.target.value })} placeholder="Warehouse (optional)" className="border rounded px-3 py-2" />
          <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded text-sm hover:bg-green-700 col-span-3">Save</button>
        </form>
      )}

      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">SKU</th>
            <th className="px-4 py-2">Quantity</th>
            <th className="px-4 py-2">Warehouse</th>
          </tr>
        </thead>
        <tbody>
          {stocks.map((s) => (
            <tr key={s.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2">{s.productSku}</td>
              <td className="px-4 py-2">{s.quantity}</td>
              <td className="px-4 py-2">{s.warehouseLocation}</td>
            </tr>
          ))}
          {stocks.length === 0 && (
            <tr><td colSpan={3} className="px-4 py-4 text-center text-gray-500">No stock records</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
