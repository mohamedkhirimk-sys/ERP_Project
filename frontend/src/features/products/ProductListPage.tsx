import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface Product {
  id: number
  name: string
  sku: string
  price: number
  stockQuantity: number
}

export default function ProductListPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/products')
      .then((res) => setProducts(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this product?')) return
    try {
      await api.delete(`/api/products/${id}`)
      setProducts((prev) => prev.filter((p) => p.id !== id))
    } catch (err) {
      console.error(err)
    }
  }

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Products</h1>
        <Link to="/products/new" className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700">
          + New Product
        </Link>
      </div>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Name</th>
            <th className="px-4 py-2">SKU</th>
            <th className="px-4 py-2">Price</th>
            <th className="px-4 py-2">Stock</th>
            <th className="px-4 py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2">{p.name}</td>
              <td className="px-4 py-2">{p.sku}</td>
              <td className="px-4 py-2">${p.price}</td>
              <td className="px-4 py-2">{p.stockQuantity}</td>
              <td className="px-4 py-2 flex gap-2">
                <Link to={`/products/${p.id}/edit`} className="text-blue-600 text-sm hover:underline">Edit</Link>
                <button onClick={() => handleDelete(p.id)} className="text-red-600 text-sm hover:underline">Delete</button>
              </td>
            </tr>
          ))}
          {products.length === 0 && (
            <tr><td colSpan={5} className="px-4 py-4 text-center text-gray-500">No products yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
