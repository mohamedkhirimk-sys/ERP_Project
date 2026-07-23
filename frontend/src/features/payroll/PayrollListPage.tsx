import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface PayrollRecord {
  id: number
  employeeId: string
  employeeName: string
  grossSalary: number
  deductions: number
  netSalary: number
  payPeriodStart: string
  payPeriodEnd: string
  status: string
}

export default function PayrollListPage() {
  const [records, setRecords] = useState<PayrollRecord[]>([])
  const [loading, setLoading] = useState(true)

  const fetchRecords = () =>
    api.get('/api/payroll').then((res) => setRecords(res.data))

  useEffect(() => { fetchRecords().finally(() => setLoading(false)) }, [])

  const handlePay = async (id: number) => {
    if (!confirm('Process this payment?')) return
    await api.patch(`/api/payroll/${id}/pay`)
    fetchRecords()
  }

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Payroll Records</h1>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Employee</th>
            <th className="px-4 py-2">Gross</th>
            <th className="px-4 py-2">Deductions</th>
            <th className="px-4 py-2">Net</th>
            <th className="px-4 py-2">Period</th>
            <th className="px-4 py-2">Status</th>
            <th className="px-4 py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {records.map((r) => (
            <tr key={r.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2">{r.employeeName}</td>
              <td className="px-4 py-2">${r.grossSalary}</td>
              <td className="px-4 py-2">${r.deductions || 0}</td>
              <td className="px-4 py-2 font-semibold">${r.netSalary}</td>
              <td className="px-4 py-2 text-sm">{r.payPeriodStart} to {r.payPeriodEnd}</td>
              <td className="px-4 py-2">{r.status}</td>
              <td className="px-4 py-2">
                {r.status === 'PENDING' && (
                  <button onClick={() => handlePay(r.id)} className="text-green-600 text-sm hover:underline">Pay</button>
                )}
              </td>
            </tr>
          ))}
          {records.length === 0 && (
            <tr><td colSpan={7} className="px-4 py-4 text-center text-gray-500">No payroll records</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
