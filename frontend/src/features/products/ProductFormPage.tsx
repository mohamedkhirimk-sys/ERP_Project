import { useState, useEffect, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

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
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold mb-4">{isEdit ? 'Edit Product' : 'New Product'}</h1>
      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg border space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Name</label>
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">SKU</label>
          <input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Price</label>
          <input type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Stock Quantity</label>
          <input type="number" value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} className="w-full border rounded px-3 py-2" required />
        </div>
        <button type="submit" disabled={loading} className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {loading ? 'Saving...' : 'Save'}
        </button>
      </form>
    </div>
  )
}
