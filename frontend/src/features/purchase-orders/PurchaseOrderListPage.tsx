import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface PurchaseOrder {
  id: number
  poNumber: string
  vendorId: number
  vendorName: string
  totalAmount: number
  status: string
  orderedAt: string
}

export default function PurchaseOrderListPage() {
  const [orders, setOrders] = useState<PurchaseOrder[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/purchase-orders')
      .then((res) => setOrders(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Purchase Orders</h1>
        <Link to="/purchase-orders/new" className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700">+ New PO</Link>
      </div>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">PO #</th>
            <th className="px-4 py-2">Vendor</th>
            <th className="px-4 py-2">Amount</th>
            <th className="px-4 py-2">Status</th>
            <th className="px-4 py-2">Ordered</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((po) => (
            <tr key={po.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2 font-mono text-sm">{po.poNumber}</td>
              <td className="px-4 py-2">{po.vendorName}</td>
              <td className="px-4 py-2">${po.totalAmount}</td>
              <td className="px-4 py-2">{po.status}</td>
              <td className="px-4 py-2">{new Date(po.orderedAt).toLocaleDateString()}</td>
            </tr>
          ))}
          {orders.length === 0 && (
            <tr><td colSpan={5} className="px-4 py-4 text-center text-gray-500">No purchase orders yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
