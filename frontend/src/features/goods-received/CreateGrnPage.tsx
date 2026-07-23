import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

interface PurchaseOrder {
  id: number
  poNumber: string
  vendorName: string
}

interface GrnItem {
  productSku: string
  quantity: number
}

export default function CreateGrnPage() {
  const navigate = useNavigate()
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrder[]>([])
  const [poId, setPoId] = useState('')
  const [status, setStatus] = useState('RECEIVED')
  const [items, setItems] = useState<GrnItem[]>([{ productSku: '', quantity: 1 }])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/api/purchase-orders').then((res) => setPurchaseOrders(res.data)).catch(console.error)
  }, [])

  const updateItem = (index: number, field: keyof GrnItem, value: string) => {
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
    if (!poId) { alert('Select a purchase order'); return }
    setSaving(true)
    try {
      await api.post('/api/goods-received', {
        purchaseOrderId: Number(poId),
        status,
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
      <h1 className="text-2xl font-bold mb-4">Create Goods Received Note</h1>
      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg border space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Purchase Order</label>
            <select value={poId} onChange={(e) => setPoId(e.target.value)} className="w-full border rounded px-3 py-2" required>
              <option value="">Select PO...</option>
              {purchaseOrders.map((po) => (
                <option key={po.id} value={po.id}>{po.poNumber} - {po.vendorName}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Status</label>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className="w-full border rounded px-3 py-2">
              <option>RECEIVED</option>
              <option>PARTIAL</option>
              <option>CANCELLED</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium mb-2">Items Received</label>
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
          {saving ? 'Creating...' : 'Create GRN'}
        </button>
      </form>
    </div>
  )
}
