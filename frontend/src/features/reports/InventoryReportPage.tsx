import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface InventoryReport {
  summary: { totalProducts: number; totalStockItems: number; lowStockCount: number; outOfStockCount: number }
  stockItems: { sku: string; productName: string; quantity: number; warehouseLocation: string }[]
  lowStockItems: { sku: string; productName: string; quantity: number; warehouseLocation: string }[]
}

export default function InventoryReportPage() {
  const [report, setReport] = useState<InventoryReport | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/inventory').then((res) => setReport(res.data)).catch(console.error).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-12"><svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg><span className="ml-3 text-gray-500">Loading...</span></div>
  if (!report) return null

  const { summary, stockItems, lowStockItems } = report

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Inventory Report</h1>
          <p className="text-sm text-gray-500">Stock levels and low stock alerts</p>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalProducts}</p><p className="text-xs text-gray-500">Products</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalStockItems}</p><p className="text-xs text-gray-500">Stock Records</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-yellow-600">{summary.lowStockCount}</p><p className="text-xs text-gray-500">Low Stock (≤10)</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-red-600">{summary.outOfStockCount}</p><p className="text-xs text-gray-500">Out of Stock</p></div>
      </div>

      {lowStockItems.length > 0 && (
        <div className="bg-red-50 rounded-xl border border-red-200 p-5 mb-6">
          <h2 className="text-sm font-semibold text-red-800 uppercase tracking-wider mb-3">⚠ Low Stock Alert</h2>
          <div className="space-y-2">
            {lowStockItems.map((item) => (
              <div key={item.sku} className="flex justify-between items-center">
                <div><p className="text-sm font-medium text-red-900">{item.productName}</p><p className="text-xs text-red-700">{item.sku}</p></div>
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${item.quantity === 0 ? 'bg-red-200 text-red-900' : 'bg-yellow-200 text-yellow-900'}`}>{item.quantity}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">All Stock Levels</h2>
        </div>
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50 text-left">
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">SKU</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Product</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Qty</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Location</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {stockItems.map((item) => (
              <tr key={item.sku} className="hover:bg-gray-50 transition">
                <td className="px-4 py-3 font-mono text-sm text-gray-600">{item.sku}</td>
                <td className="px-4 py-3 font-medium text-gray-900">{item.productName}</td>
                <td className="px-4 py-3">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${item.quantity > 10 ? 'bg-green-100 text-green-800' : item.quantity > 0 ? 'bg-yellow-100 text-yellow-800' : 'bg-red-100 text-red-800'}`}>{item.quantity}</span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">{item.warehouseLocation || '—'}</td>
              </tr>
            ))}
            {stockItems.length === 0 && <tr><td colSpan={4} className="px-4 py-8 text-center text-gray-500">No stock records</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
