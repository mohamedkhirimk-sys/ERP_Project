import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'
const selectClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white'

interface OrderItem { productSku: string; quantity: number }

export default function CreateOrderPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ customerName: '', totalAmount: '', status: 'PENDING', paymentMethod: 'CREDIT_CARD' })
  const [items, setItems] = useState<OrderItem[]>([{ productSku: '', quantity: 1 }])
  const [saving, setSaving] = useState(false)

  const updateItem = (index: number, field: keyof OrderItem, value: string) => {
    const updated = [...items]
    if (field === 'quantity') updated[index] = { ...updated[index], quantity: Number(value) || 0 }
    else updated[index] = { ...updated[index], productSku: value }
    setItems(updated)
  }
  const addItem = () => setItems([...items, { productSku: '', quantity: 1 }])
  const removeItem = (index: number) => items.length > 1 && setItems(items.filter((_, i) => i !== index))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      await api.post('/api/orders', {
        customerName: form.customerName, totalAmount: Number(form.totalAmount),
        status: form.status, paymentMethod: form.paymentMethod,
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
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Create Order</h1>
          <p className="text-sm text-gray-500">Create a new customer order</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Customer Name</label>
            <input value={form.customerName} onChange={(e) => setForm({ ...form, customerName: e.target.value })} className={inputClass} placeholder="Customer name" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Total Amount</label>
            <input type="number" step="0.01" value={form.totalAmount} onChange={(e) => setForm({ ...form, totalAmount: e.target.value })} className={inputClass} placeholder="0.00" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} className={selectClass}>
              <option>PENDING</option><option>CONFIRMED</option><option>SHIPPED</option><option>DELIVERED</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Payment Method</label>
            <select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })} className={selectClass}>
              <option>CREDIT_CARD</option><option>BANK_TRANSFER</option><option>CASH</option>
            </select>
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-gray-700">Order Items</label>
            <button type="button" onClick={addItem} className="text-blue-600 text-sm font-medium hover:underline flex items-center gap-1">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>Add Item
            </button>
          </div>
          <div className="space-y-2">
            {items.map((item, i) => (
              <div key={i} className="flex gap-2">
                <input value={item.productSku} onChange={(e) => updateItem(i, 'productSku', e.target.value)} placeholder="SKU" className={`${inputClass} flex-1`} required />
                <input type="number" min="1" value={item.quantity} onChange={(e) => updateItem(i, 'quantity', e.target.value)} placeholder="Qty" className={`${inputClass} w-24`} required />
                <button type="button" onClick={() => removeItem(i)} className="p-2.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3 pt-4 border-t">
          <button type="submit" disabled={saving} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition flex items-center gap-2">
            {saving ? (
              <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Creating...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>Create Order</>
            )}
          </button>
          <button type="button" onClick={() => navigate('/orders')} className="px-6 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">Cancel</button>
        </div>
      </form>
    </div>
  )
}
