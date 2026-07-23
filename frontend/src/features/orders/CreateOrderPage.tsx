import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

interface OrderItem {
  productSku: string
  quantity: number
}

export default function CreateOrderPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    customerName: '',
    totalAmount: '',
    status: 'PENDING',
    paymentMethod: 'CREDIT_CARD',
  })
  const [items, setItems] = useState<OrderItem[]>([{ productSku: '', quantity: 1 }])
  const [saving, setSaving] = useState(false)

  const updateItem = (index: number, field: keyof OrderItem, value: string) => {
    const updated = [...items]
    if (field === 'quantity') {
      updated[index] = { ...updated[index], quantity: Number(value) || 0 }
    } else {
      updated[index] = { ...updated[index], productSku: value }
    }
    setItems(updated)
  }

  const addItem = () => setItems([...items, { productSku: '', quantity: 1 }])
  const removeItem = (index: number) => items.length > 1 && setItems(items.filter((_, i) => i !== index))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post('/api/orders', {
        customerName: form.customerName,
        totalAmount: Number(form.totalAmount),
        status: form.status,
        paymentMethod: form.paymentMethod,
        items: items.filter((i) => i.productSku.trim()),
      })
      navigate('/orders')
    } catch (err) {
      console.error(err)
      alert('Failed to create order')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">Create Order</h1>
      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg border space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Customer Name</label>
            <input value={form.customerName} onChange={(e) => setForm({ ...form, customerName: e.target.value })} className="w-full border rounded px-3 py-2" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Total Amount</label>
            <input type="number" step="0.01" value={form.totalAmount} onChange={(e) => setForm({ ...form, totalAmount: e.target.value })} className="w-full border rounded px-3 py-2" required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Status</label>
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} className="w-full border rounded px-3 py-2">
              <option>PENDING</option>
              <option>CONFIRMED</option>
              <option>SHIPPED</option>
              <option>DELIVERED</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Payment Method</label>
            <select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })} className="w-full border rounded px-3 py-2">
              <option>CREDIT_CARD</option>
              <option>BANK_TRANSFER</option>
              <option>CASH</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium mb-2">Items</label>
          {items.map((item, i) => (
            <div key={i} className="flex gap-2 mb-2">
              <input value={item.productSku} onChange={(e) => updateItem(i, 'productSku', e.target.value)} placeholder="SKU" className="flex-1 border rounded px-3 py-2" required />
              <input type="number" min="1" value={item.quantity} onChange={(e) => updateItem(i, 'quantity', e.target.value)} placeholder="Qty" className="w-24 border rounded px-3 py-2" required />
              <button type="button" onClick={() => removeItem(i)} className="px-3 py-2 text-red-600 hover:bg-red-50 rounded">X</button>
            </div>
          ))}
          <button type="button" onClick={addItem} className="text-blue-600 text-sm hover:underline">+ Add Item</button>
        </div>

        <button type="submit" disabled={saving} className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {saving ? 'Creating...' : 'Create Order'}
        </button>
      </form>
    </div>
  )
}
