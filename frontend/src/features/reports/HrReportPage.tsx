import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface HrReport {
  summary: { totalEmployees: number; activeEmployees: number; terminatedEmployees: number; pendingLeaves: number }
  byDepartment: { department: string; employeeCount: number; totalSalary: number; avgSalary: number }[]
  recentPayroll: { employeeName: string; grossSalary: number; deductions: number; netSalary: number; status: string; period: string }[]
}

export default function HrReportPage() {
  const [report, setReport] = useState<HrReport | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/hr').then((res) => setReport(res.data)).catch(console.error).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-12"><svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg><span className="ml-3 text-gray-500">Loading...</span></div>
  if (!report) return null

  const { summary, byDepartment, recentPayroll } = report

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">HR Report</h1>
          <p className="text-sm text-gray-500">Employee statistics and payroll summary</p>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalEmployees}</p><p className="text-xs text-gray-500">Total Employees</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-green-600">{summary.activeEmployees}</p><p className="text-xs text-gray-500">Active</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-red-600">{summary.terminatedEmployees}</p><p className="text-xs text-gray-500">Terminated</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-yellow-600">{summary.pendingLeaves}</p><p className="text-xs text-gray-500">Pending Leaves</p></div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">Employees by Department</h2>
          </div>
          <div className="divide-y divide-gray-100">
            {byDepartment.map((d) => (
              <div key={d.department} className="px-5 py-3 flex justify-between items-center hover:bg-gray-50 transition">
                <div>
                  <p className="text-sm font-medium text-gray-900">{d.department}</p>
                  <p className="text-xs text-gray-500">{d.employeeCount} employees</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-mono font-semibold text-gray-900">${d.avgSalary.toLocaleString()}</p>
                  <p className="text-xs text-gray-500">avg salary</p>
                </div>
              </div>
            ))}
            {byDepartment.length === 0 && <p className="px-5 py-4 text-sm text-gray-500">No data</p>}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">Recent Payroll</h2>
          </div>
          <div className="divide-y divide-gray-100 max-h-96 overflow-y-auto">
            {recentPayroll.map((p, i) => (
              <div key={i} className="px-5 py-3 hover:bg-gray-50 transition">
                <div className="flex justify-between items-start">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{p.employeeName}</p>
                    <p className="text-xs text-gray-500">{p.period}</p>
                  </div>
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${p.status === 'PAID' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>{p.status}</span>
                </div>
                <div className="flex gap-4 mt-1 text-sm">
                  <span className="text-gray-500">Gross: <span className="font-mono text-gray-900">${p.grossSalary.toLocaleString()}</span></span>
                  <span className="text-gray-500">Net: <span className="font-mono text-gray-900">${p.netSalary.toLocaleString()}</span></span>
                </div>
              </div>
            ))}
            {recentPayroll.length === 0 && <p className="px-5 py-4 text-sm text-gray-500">No payroll records</p>}
          </div>
        </div>
      </div>
    </div>
  )
}
