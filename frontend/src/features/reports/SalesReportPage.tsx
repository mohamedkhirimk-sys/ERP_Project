import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface SalesReport {
  summary: { totalOrders: number; totalRevenue: number; averageOrderValue: number; paidInvoices: number; pendingInvoices: number }
  topOrders: { orderNumber: string; customerName: string; totalAmount: number; status: string }[]
  dailyRevenue: { date: string; orderCount: number; revenue: number }[]
}

export default function SalesReportPage() {
  const [report, setReport] = useState<SalesReport | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/sales').then((res) => setReport(res.data)).catch(console.error).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-12"><svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg><span className="ml-3 text-gray-500">Loading...</span></div>
  if (!report) return null

  const { summary, topOrders, dailyRevenue } = report

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Sales Report</h1>
          <p className="text-sm text-gray-500">Revenue, orders, and invoice summary</p>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalOrders}</p><p className="text-xs text-gray-500">Total Orders</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">${summary.totalRevenue.toLocaleString()}</p><p className="text-xs text-gray-500">Total Revenue</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">${summary.averageOrderValue.toLocaleString()}</p><p className="text-xs text-gray-500">Avg Order Value</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-green-600">{summary.paidInvoices}</p><p className="text-xs text-gray-500">Paid Invoices</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-yellow-600">{summary.pendingInvoices}</p><p className="text-xs text-gray-500">Pending Invoices</p></div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-4">Top Orders</h2>
          <div className="space-y-3">
            {topOrders.map((o) => (
              <div key={o.orderNumber} className="flex justify-between items-center">
                <div>
                  <p className="text-sm font-medium text-gray-900">{o.customerName}</p>
                  <p className="text-xs text-gray-500">{o.orderNumber} <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${o.status === 'DELIVERED' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>{o.status}</span></p>
                </div>
                <p className="text-sm font-mono font-semibold text-gray-900">${o.totalAmount.toLocaleString()}</p>
              </div>
            ))}
            {topOrders.length === 0 && <p className="text-sm text-gray-500">No orders yet</p>}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-4">Daily Revenue</h2>
          <div className="space-y-2">
            {dailyRevenue.map((d) => (
              <div key={d.date} className="flex justify-between items-center">
                <p className="text-sm text-gray-700">{d.date}</p>
                <div className="text-right">
                  <p className="text-sm font-mono font-semibold text-gray-900">${d.revenue.toLocaleString()}</p>
                  <p className="text-xs text-gray-500">{d.orderCount} orders</p>
                </div>
              </div>
            ))}
            {dailyRevenue.length === 0 && <p className="text-sm text-gray-500">No data</p>}
          </div>
        </div>
      </div>
    </div>
  )
}
