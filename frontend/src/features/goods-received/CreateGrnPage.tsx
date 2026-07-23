import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'
const selectClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white'

interface PO { id: number; poNumber: string; vendorName: string }
interface GrnItem { productSku: string; quantity: number }

export default function CreateGrnPage() {
  const navigate = useNavigate()
  const [purchaseOrders, setPurchaseOrders] = useState<PO[]>([])
  const [poId, setPoId] = useState('')
  const [status, setStatus] = useState('RECEIVED')
  const [items, setItems] = useState<GrnItem[]>([{ productSku: '', quantity: 1 }])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/api/purchase-orders').then((res) => setPurchaseOrders(res.data)).catch(console.error)
  }, [])

  const updateItem = (index: number, field: keyof GrnItem, value: string) => {
    const updated = [...items]
    if (field === 'quantity') updated[index] = { ...updated[index], quantity: Number(value) || 0 }
    else updated[index] = { ...updated[index], productSku: value }
    setItems(updated)
  }

  const addItem = () => setItems([...items, { productSku: '', quantity: 1 }])
  const removeItem = (index: number) => items.length > 1 && setItems(items.filter((_, i) => i !== index))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!poId) { alert('Select a purchase order'); return }
    setSaving(true)
    try {
      await api.post('/api/goods-received', {
        purchaseOrderId: Number(poId), status,
        items: items.filter((i) => i.productSku.trim()),
      })
      navigate('/purchase-orders')
    } catch (err) {
      console.error(err)
      alert('Failed to create GRN')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Goods Received Note</h1>
          <p className="text-sm text-gray-500">Record goods received against a purchase order</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Purchase Order</label>
            <select value={poId} onChange={(e) => setPoId(e.target.value)} className={selectClass} required>
              <option value="">Select PO...</option>
              {purchaseOrders.map((po) => (
                <option key={po.id} value={po.id}>{po.poNumber} - {po.vendorName}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectClass}>
              <option>RECEIVED</option><option>PARTIAL</option><option>CANCELLED</option>
            </select>
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-gray-700">Items Received</label>
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
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>Create GRN</>
            )}
          </button>
          <button type="button" onClick={() => navigate('/purchase-orders')} className="px-6 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">Cancel</button>
        </div>
      </form>
    </div>
  )
}
