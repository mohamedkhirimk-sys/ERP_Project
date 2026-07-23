import { useState, useEffect, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

const labelClass = 'block text-sm font-medium text-gray-700 mb-1'
const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

export default function ProductFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState({ name: '', sku: '', price: '', stockQuantity: '' })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (id) {
      api.get(`/api/products/${id}`).then((res) => {
        const p = res.data
        setForm({ name: p.name, sku: p.sku, price: String(p.price), stockQuantity: String(p.stockQuantity) })
      }).catch(console.error)
    }
  }, [id])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const payload = { ...form, price: Number(form.price), stockQuantity: Number(form.stockQuantity) }
      if (isEdit) {
        await api.put(`/api/products/${id}`, payload)
      } else {
        await api.post('/api/products', payload)
      }
      navigate('/products')
    } catch (err) {
      console.error(err)
      alert('Failed to save product')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{isEdit ? 'Edit Product' : 'New Product'}</h1>
          <p className="text-sm text-gray-500">{isEdit ? 'Update product details' : 'Add a new product to inventory'}</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <label className={labelClass}>Product Name</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className={inputClass} placeholder="Product name" required />
          </div>
          <div>
            <label className={labelClass}>SKU</label>
            <input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} className={inputClass} placeholder="e.g. SKU-001" required />
          </div>
          <div>
            <label className={labelClass}>Price</label>
            <input type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} className={inputClass} placeholder="0.00" required />
          </div>
          <div>
            <label className={labelClass}>Stock Quantity</label>
            <input type="number" value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} className={inputClass} placeholder="0" required />
          </div>
        </div>

        <div className="flex items-center gap-3 pt-4 border-t">
          <button type="submit" disabled={loading} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition flex items-center gap-2">
            {loading ? (
              <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Saving...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>Save Product</>
            )}
          </button>
          <button type="button" onClick={() => navigate('/products')} className="px-6 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
