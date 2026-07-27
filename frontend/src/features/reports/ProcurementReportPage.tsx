import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface ProcurementReport {
  summary: { totalVendors: number; totalPurchaseOrders: number; pendingPOs: number; receivedPOs: number; totalPoAmount: number }
  pendingOrders: { poNumber: string; vendorName: string; totalAmount: number; status: string; orderedAt: string }[]
  topVendors: { vendorName: string; poCount: number; totalAmount: number }[]
}

export default function ProcurementReportPage() {
  const [report, setReport] = useState<ProcurementReport | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/procurement').then((res) => setReport(res.data)).catch(console.error).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-12"><svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg><span className="ml-3 text-gray-500">Loading...</span></div>
  if (!report) return null

  const { summary, pendingOrders, topVendors } = report

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-indigo-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Procurement Report</h1>
          <p className="text-sm text-gray-500">Purchase orders and vendor activity</p>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalVendors}</p><p className="text-xs text-gray-500">Vendors</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalPurchaseOrders}</p><p className="text-xs text-gray-500">Total POs</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-yellow-600">{summary.pendingPOs}</p><p className="text-xs text-gray-500">Pending</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-green-600">{summary.receivedPOs}</p><p className="text-xs text-gray-500">Received</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">${summary.totalPoAmount.toLocaleString()}</p><p className="text-xs text-gray-500">Total Amount</p></div>
      </div>

      {pendingOrders.length > 0 && (
        <div className="bg-yellow-50 rounded-xl border border-yellow-200 p-5 mb-6">
          <h2 className="text-sm font-semibold text-yellow-800 uppercase tracking-wider mb-3">⚠ Pending Purchase Orders</h2>
          <div className="space-y-2">
            {pendingOrders.map((po) => (
              <div key={po.poNumber} className="flex justify-between items-center">
                <div><p className="text-sm font-medium text-yellow-900">{po.poNumber} — {po.vendorName}</p><p className="text-xs text-yellow-700">Ordered: {po.orderedAt}</p></div>
                <p className="text-sm font-mono font-semibold text-yellow-900">${po.totalAmount.toLocaleString()}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">Top Vendors by Spend</h2>
          </div>
          <div className="divide-y divide-gray-100">
            {topVendors.map((v) => (
              <div key={v.vendorName} className="px-5 py-3 flex justify-between items-center hover:bg-gray-50 transition">
                <div>
                  <p className="text-sm font-medium text-gray-900">{v.vendorName}</p>
                  <p className="text-xs text-gray-500">{v.poCount} purchase orders</p>
                </div>
                <p className="text-sm font-mono font-semibold text-gray-900">${v.totalAmount.toLocaleString()}</p>
              </div>
            ))}
            {topVendors.length === 0 && <p className="px-5 py-4 text-sm text-gray-500">No data</p>}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-4">Quick Summary</h2>
          <div className="space-y-3">
            <div className="flex justify-between items-center"><span className="text-sm text-gray-600">Total Vendors</span><span className="text-sm font-semibold text-gray-900">{summary.totalVendors}</span></div>
            <div className="flex justify-between items-center"><span className="text-sm text-gray-600">Total POs</span><span className="text-sm font-semibold text-gray-900">{summary.totalPurchaseOrders}</span></div>
            <div className="flex justify-between items-center"><span className="text-sm text-gray-600">Pending POs</span><span className="text-sm font-semibold text-yellow-600">{summary.pendingPOs}</span></div>
            <div className="flex justify-between items-center"><span className="text-sm text-gray-600">Received POs</span><span className="text-sm font-semibold text-green-600">{summary.receivedPOs}</span></div>
            <div className="border-t pt-3 flex justify-between items-center"><span className="text-sm font-medium text-gray-700">Total PO Amount</span><span className="text-base font-bold text-gray-900">${summary.totalPoAmount.toLocaleString()}</span></div>
          </div>
        </div>
      </div>
    </div>
  )
}
