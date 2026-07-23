import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface OrderItem {
  id: number
  productSku: string
  quantity: number
}

interface Order {
  id: number
  orderNumber: string
  customerName: string
  totalAmount: number
  status: string
  items: OrderItem[]
}

export default function OrderListPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/orders')
      .then((res) => setOrders(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Orders</h1>
        <Link to="/orders/new" className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700">+ New Order</Link>
      </div>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Order #</th>
            <th className="px-4 py-2">Customer</th>
            <th className="px-4 py-2">Amount</th>
            <th className="px-4 py-2">Status</th>
            <th className="px-4 py-2">Items</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2 font-mono text-sm">{o.orderNumber}</td>
              <td className="px-4 py-2">{o.customerName}</td>
              <td className="px-4 py-2">${o.totalAmount}</td>
              <td className="px-4 py-2">{o.status}</td>
              <td className="px-4 py-2 text-sm">{o.items?.map((i) => `${i.productSku} x${i.quantity}`).join(', ')}</td>
            </tr>
          ))}
          {orders.length === 0 && (
            <tr><td colSpan={5} className="px-4 py-4 text-center text-gray-500">No orders yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
