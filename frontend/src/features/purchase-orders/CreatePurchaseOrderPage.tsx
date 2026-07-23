import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

interface Vendor { id: number; name: string }

export default function CreatePurchaseOrderPage() {
  const navigate = useNavigate()
  const [vendors, setVendors] = useState<Vendor[]>([])
  const [vendorId, setVendorId] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [status, setStatus] = useState('PENDING')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.get('/api/vendors').then((res) => setVendors(res.data)).catch(console.error)
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!vendorId) { alert('Select a vendor'); return }
    setSaving(true)
    try {
      await api.post('/api/purchase-orders', {
        vendorId: Number(vendorId),
        totalAmount: Number(totalAmount),
        status,
      })
      navigate('/purchase-orders')
    } catch (err) {
      console.error(err)
      alert('Failed to create purchase order')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold mb-4">Create Purchase Order</h1>
      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg border space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Vendor</label>
          <select value={vendorId} onChange={(e) => setVendorId(e.target.value)} className="w-full border rounded px-3 py-2" required>
            <option value="">Select vendor...</option>
            {vendors.map((v) => <option key={v.id} value={v.id}>{v.name}</option>)}
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
            <option>APPROVED</option>
            <option>CANCELLED</option>
          </select>
        </div>
        <button type="submit" disabled={saving} className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {saving ? 'Creating...' : 'Create Purchase Order'}
        </button>
      </form>
    </div>
  )
}
