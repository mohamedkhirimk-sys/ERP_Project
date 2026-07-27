import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface DashboardData {
  totalOrders: number; totalInvoices: number; paidInvoices: number; pendingInvoices: number
  lowStockItems: number; outOfStockItems: number
  activeEmployees: number; pendingPayroll: number; pendingPOs: number
  totalRevenue: number
}

const cards = [
  { key: 'totalOrders', label: 'Total Orders', link: '/reports/sales', color: 'bg-blue-500', icon: 'M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z' },
  { key: 'paidInvoices', label: 'Paid Invoices', link: '/reports/sales', color: 'bg-green-500', icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' },
  { key: 'pendingInvoices', label: 'Pending Invoices', link: '/reports/sales', color: 'bg-yellow-500', icon: 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
  { key: 'totalRevenue', label: 'Total Revenue', prefix: '$', link: '/reports/sales', color: 'bg-emerald-500', icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
  { key: 'activeEmployees', label: 'Active Employees', link: '/reports/hr', color: 'bg-purple-500', icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' },
  { key: 'pendingPayroll', label: 'Pending Payroll', link: '/reports/hr', color: 'bg-orange-500', icon: 'M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z' },
  { key: 'lowStockItems', label: 'Low Stock Items', link: '/reports/inventory', color: 'bg-yellow-600', icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10' },
  { key: 'pendingPOs', label: 'Pending POs', link: '/reports/procurement', color: 'bg-indigo-500', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
]

const reportLinks = [
  { title: 'Sales Report', desc: 'Revenue, orders, and invoice summary', link: '/reports/sales', icon: 'M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z', color: 'bg-blue-500' },
  { title: 'Inventory Report', desc: 'Stock levels and low stock alerts', link: '/reports/inventory', icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10', color: 'bg-green-500' },
  { title: 'Financial Report', desc: 'Trial balance and journal activity', link: '/reports/financial', icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z', color: 'bg-orange-500' },
  { title: 'HR Report', desc: 'Employee stats, departments, and payroll', link: '/reports/hr', icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z', color: 'bg-purple-500' },
  { title: 'Procurement Report', desc: 'Purchase orders and vendor activity', link: '/reports/procurement', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2', color: 'bg-indigo-500' },
]

export default function ReportsListPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [seeding, setSeeding] = useState(false)
  const [seedResult, setSeedResult] = useState<string | null>(null)
  const [seedError, setSeedError] = useState<string | null>(null)

  useEffect(() => {
    api.get('/api/reports/dashboard')
      .then((res) => setData(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const handleSeed = async () => {
    setSeeding(true)
    setSeedResult(null)
    setSeedError(null)
    try {
      const res = await api.post('/api/reports/seed')
      const items = Object.entries(res.data).map(([k, v]) => `${k}: ${v}`).join('\n')
      setSeedResult(items)
      const dash = await api.get('/api/reports/dashboard')
      setData(dash.data)
    } catch (err: any) {
      setSeedError(
        err?.response?.status === 503
          ? 'Reporting service is not running. Start it via run-all.ps1 or "mvn spring-boot:run" in backend/reporting-service.'
          : err?.response?.status === 401 || err?.response?.status === 403
            ? 'Authentication failed. Try logging out and back in.'
            : `Error ${err?.response?.status || '—'}: ${err?.response?.data?.message || err?.message || 'Unknown error'}. Make sure all 11 services are running (check http://localhost:8761).`
      )
    } finally {
      setSeeding(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center py-12">
      <svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>
      <span className="ml-3 text-gray-500">Loading reports...</span>
    </div>
  )

  return (
    <div>
      <div className="flex items-center gap-3 mb-8">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Reports Dashboard</h1>
          <p className="text-sm text-gray-500">Key metrics across all business areas</p>
        </div>
      </div>

      {data && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {cards.map((card) => {
            const val = data[card.key as keyof DashboardData]
            const display = typeof val === 'number' && card.key === 'totalRevenue'
              ? `$${Number(val).toLocaleString()}`
              : typeof val === 'number'
                ? val.toLocaleString()
                : val
            return (
              <Link key={card.key} to={card.link} className="group block bg-white rounded-xl shadow-sm border border-gray-200 p-4 hover:shadow-md transition-all">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 ${card.color} rounded-xl flex items-center justify-center shadow-sm`}>
                    <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={card.icon} /></svg>
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{display}</p>
                    <p className="text-xs text-gray-500">{card.label}</p>
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}

      <h2 className="text-lg font-semibold text-gray-900 mb-4">Detailed Reports</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        {reportLinks.map((r) => (
          <Link key={r.link} to={r.link} className="group block bg-white rounded-xl shadow-sm border border-gray-200 p-5 hover:shadow-md transition-all">
            <div className="flex items-center gap-4">
              <div className={`w-11 h-11 ${r.color} rounded-xl flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform`}>
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={r.icon} /></svg>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">{r.title}</h3>
                <p className="text-sm text-gray-500">{r.desc}</p>
              </div>
            </div>
          </Link>
        ))}
      </div>

      <div className="border-t pt-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">Demo Data</h2>
            <p className="text-xs text-gray-500 mt-0.5">Populate the system with sample data to test reports</p>
          </div>
          <button onClick={handleSeed} disabled={seeding} className="px-5 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition flex items-center gap-2">
            {seeding ? (
              <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Seeding...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>Seed Demo Data</>
            )}
          </button>
        </div>
        {seedError && (
          <div className="mt-3 bg-red-50 rounded-lg border border-red-200 p-4 text-sm text-red-700">{seedError}</div>
        )}
        {seedResult && (
          <pre className="mt-3 bg-gray-50 rounded-lg border border-gray-200 p-4 text-xs font-mono text-gray-700 whitespace-pre-wrap">{seedResult}</pre>
        )}
      </div>
    </div>
  )
}
