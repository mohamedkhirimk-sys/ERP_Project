import { useEffect, useState } from 'react'
import api from '@/lib/axios'

interface AccountLine {
  accountCode: string
  accountName: string
  balance: number
}

interface IncomeStatement {
  revenue: AccountLine[]
  expenses: AccountLine[]
  totalRevenue: number
  totalExpenses: number
  netIncome: number
}

const sectionTitle = 'text-sm font-semibold text-gray-700 uppercase tracking-wider'
const thClass = 'px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-left'
const tdClass = 'px-4 py-3 text-sm text-gray-900'
const tdAmount = 'px-4 py-3 font-mono text-sm text-gray-900 text-right'

function AccountTable({ title, rows }: { title: string; rows: AccountLine[] }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <div className="px-5 py-4 border-b border-gray-100">
        <h2 className={sectionTitle}>{title}</h2>
      </div>
      <table className="w-full">
        <thead>
          <tr className="border-b border-gray-200 bg-gray-50">
            <th className={thClass}>Account</th>
            <th className={`${thClass} text-right`}>Amount</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {rows.map((a) => (
            <tr key={a.accountCode}>
              <td className={tdClass}><span className="font-mono text-gray-600">{a.accountCode}</span><span className="ml-2 font-medium">{a.accountName}</span></td>
              <td className={tdAmount}>${a.balance.toLocaleString()}</td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr><td colSpan={2} className="px-4 py-8 text-center text-gray-500">No accounts</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

export default function IncomeStatementTab() {
  const [statement, setStatement] = useState<IncomeStatement | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/income-statement')
      .then((res) => setStatement(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-gray-500 py-12 text-center">Loading...</p>
  if (!statement) return null

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <AccountTable title="Revenue" rows={statement.revenue} />
      <AccountTable title="Expenses" rows={statement.expenses} />
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <div className="flex justify-between items-center">
          <p className="text-sm font-medium text-gray-500">Total Revenue</p>
          <p className="font-mono text-sm font-semibold text-gray-900">${statement.totalRevenue.toLocaleString()}</p>
        </div>
        <div className="flex justify-between items-center mt-2">
          <p className="text-sm font-medium text-gray-500">Total Expenses</p>
          <p className="font-mono text-sm font-semibold text-gray-900">${statement.totalExpenses.toLocaleString()}</p>
        </div>
        <div className="flex justify-between items-center mt-4 pt-4 border-t border-gray-200">
          <p className="text-sm font-semibold text-gray-700">Net income</p>
          <p className={`font-mono text-lg font-bold ${statement.netIncome < 0 ? 'text-red-600' : 'text-gray-900'}`}>
            ${statement.netIncome.toLocaleString()}
          </p>
        </div>
      </div>
    </div>
  )
}
