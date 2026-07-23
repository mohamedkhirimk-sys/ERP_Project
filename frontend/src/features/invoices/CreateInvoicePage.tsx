import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

interface Customer { id: number; name: string }

export default function CreateInvoicePage() {
  const navigate = useNavigate()
  const [customers, setCustomers] = useState<Customer[]>([])
  const [customerId, setCustomerId] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [status, setStatus] = useState('PENDING')
  const [dueDate, setDueDate] = useState('')
  const [items, setItems] = useState([{ productSku: '', quantity: 1 }])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/api/customers').then((res) => setCustomers(res.data)).catch(console.error)
  }, [])

  const updateItem = (i: number, field: string, val: string) => {
    const updated = [...items]
    if (field === 'quantity') updated[i] = { ...updated[i], quantity: Number(val) || 0 }
    else updated[i] = { ...updated[i], productSku: val }
    setItems(updated)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!customerId) { alert('Select a customer'); return }
    setSaving(true)
    try {
      await api.post('/api/invoices', {
        customerId: Number(customerId),
        totalAmount: Number(totalAmount),
        status,
        dueDate: dueDate || null,
        items: items.filter((i) => i.productSku.trim()),
      })
      navigate('/invoices')
    } catch (err) {
      console.error(err)
      alert('Failed to create invoice')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">Create Invoice</h1>
      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg border space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Customer</label>
            <select value={customerId} onChange={(e) => setCustomerId(e.target.value)} className="w-full border rounded px-3 py-2" required>
              <option value="">Select customer...</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Total Amount</label>
            <input type="number" step="0.01" value={totalAmount} onChange={(e) => setTotalAmount(e.target.value)} className="w-full border rounded px-3 py-2" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Status</label>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className="w-full border rounded px-3 py-2">
              <option>PENDING</option>
              <option>PAID</option>
              <option>CANCELLED</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Due Date</label>
            <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} className="w-full border rounded px-3 py-2" />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium mb-2">Items</label>
          {items.map((item, i) => (
            <div key={i} className="flex gap-2 mb-2">
              <input value={item.productSku} onChange={(e) => updateItem(i, 'productSku', e.target.value)} placeholder="SKU" className="flex-1 border rounded px-3 py-2" required />
              <input type="number" min="1" value={item.quantity} onChange={(e) => updateItem(i, 'quantity', e.target.value)} className="w-24 border rounded px-3 py-2" required />
              <button type="button" onClick={() => items.length > 1 && setItems(items.filter((_, j) => j !== i))} className="px-3 py-2 text-red-600 hover:bg-red-50 rounded">X</button>
            </div>
          ))}
          <button type="button" onClick={() => setItems([...items, { productSku: '', quantity: 1 }])} className="text-blue-600 text-sm hover:underline">+ Add Item</button>
        </div>
        <button type="submit" disabled={saving} className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {saving ? 'Creating...' : 'Create Invoice'}
        </button>
      </form>
    </div>
  )
}
