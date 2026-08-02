import { useState } from 'react'
import BalanceSheetTab from './BalanceSheetTab'
import IncomeStatementTab from './IncomeStatementTab'
import TrialBalanceTab from './TrialBalanceTab'

const tabs = [
  { key: 'trial', label: 'Trial Balance' },
  { key: 'balance', label: 'Balance Sheet' },
  { key: 'income', label: 'Income Statement' },
] as const

type TabKey = (typeof tabs)[number]['key']

export default function FinancialReportPage() {
  const [tab, setTab] = useState<TabKey>('trial')

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-orange-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Financial Report</h1>
          <p className="text-sm text-gray-500">Trial balance and journal activity</p>
        </div>
      </div>

      <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-6 w-fit">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition ${
              tab === t.key ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'trial' && <TrialBalanceTab />}
      {tab === 'balance' && <BalanceSheetTab />}
      {tab === 'income' && <IncomeStatementTab />}
    </div>
  )
}
