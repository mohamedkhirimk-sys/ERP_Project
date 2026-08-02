import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface FinancialReport {
  summary: { totalAccounts: number; totalJournalEntries: number; totalDebits: number; totalCredits: number }
  trialBalance: { accountCode: string; accountName: string; accountType: string; balance: number; totalDebits: number; totalCredits: number }[]
  recentEntries: { entryNumber: string; description: string; debit: number; credit: number; entryDate: string }[]
}

const typeColors: Record<string, string> = {
  ASSET: 'bg-blue-100 text-blue-800', LIABILITY: 'bg-yellow-100 text-yellow-800',
  EQUITY: 'bg-purple-100 text-purple-800', REVENUE: 'bg-green-100 text-green-800',
  EXPENSE: 'bg-red-100 text-red-800',
}

export default function TrialBalanceTab() {
  const [report, setReport] = useState<FinancialReport | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/financial').then((res) => setReport(res.data)).catch(console.error).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-12"><svg className="animate-spin h-6 w-6 text-blue-600" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg><span className="ml-3 text-gray-500">Loading...</span></div>
  if (!report) return null

  const { summary, trialBalance, recentEntries } = report

  return (
    <div>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalAccounts}</p><p className="text-xs text-gray-500">Accounts</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-gray-900">{summary.totalJournalEntries}</p><p className="text-xs text-gray-500">Journal Entries</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-blue-600">${summary.totalDebits.toLocaleString()}</p><p className="text-xs text-gray-500">Total Debits</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-blue-600">${summary.totalCredits.toLocaleString()}</p><p className="text-xs text-gray-500">Total Credits</p></div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">Trial Balance</h2>
          </div>
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50 text-left">
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Account</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Type</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Debits</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Credits</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {trialBalance.map((a) => (
                <tr key={a.accountCode} className="hover:bg-gray-50 transition">
                  <td className="px-4 py-3"><span className="font-mono text-sm text-gray-600">{a.accountCode}</span><span className="ml-2 text-sm font-medium text-gray-900">{a.accountName}</span></td>
                  <td className="px-4 py-3"><span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${typeColors[a.accountType] || 'bg-gray-100 text-gray-800'}`}>{a.accountType}</span></td>
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">${a.totalDebits.toLocaleString()}</td>
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">${a.totalCredits.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="border-t border-gray-200 bg-gray-50">
                <td colSpan={2} className="px-4 py-3 text-sm font-semibold text-gray-700">Total</td>
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${summary.totalDebits.toLocaleString()}</td>
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${summary.totalCredits.toLocaleString()}</td>
              </tr>
            </tfoot>
          </table>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h2 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">Recent Journal Entries</h2>
          </div>
          <div className="divide-y divide-gray-100 max-h-96 overflow-y-auto">
            {recentEntries.map((e) => (
              <div key={e.entryNumber} className="px-5 py-3 hover:bg-gray-50 transition">
                <div className="flex justify-between items-start">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{e.entryNumber}</p>
                    <p className="text-xs text-gray-500">{e.description}</p>
                  </div>
                  <p className="text-xs text-gray-400">{e.entryDate}</p>
                </div>
                <div className="flex gap-4 mt-1 text-sm">
                  {e.debit > 0 && <span className="font-mono text-red-600">DR ${e.debit.toLocaleString()}</span>}
                  {e.credit > 0 && <span className="font-mono text-green-600">CR ${e.credit.toLocaleString()}</span>}
                </div>
              </div>
            ))}
            {recentEntries.length === 0 && <p className="px-5 py-4 text-sm text-gray-500">No entries yet</p>}
          </div>
        </div>
      </div>
    </div>
  )
}
