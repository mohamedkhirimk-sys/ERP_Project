import { useCallback, useEffect, useState } from 'react'
import { type BankAccount, getBanks } from './api'
import PositionTab from './PositionTab'
import OperationsTab from './OperationsTab'
import MovementsTab from './MovementsTab'
import BanksTab from './BanksTab'

const tabs = [
  { key: 'position', label: 'Position' },
  { key: 'operations', label: 'Operations' },
  { key: 'movements', label: 'Movements' },
  { key: 'banks', label: 'Banks' },
]

export default function TreasuryPage() {
  const [tab, setTab] = useState('position')
  const [banks, setBanks] = useState<BankAccount[]>([])
  const [refreshKey, setRefreshKey] = useState(0)

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  useEffect(() => {
    getBanks().then(setBanks).catch(console.error)
  }, [refreshKey])

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Treasury</h1>
          <p className="text-sm text-gray-500">Bank accounts, operations and cash position</p>
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

      {tab === 'position' && <PositionTab refreshKey={refreshKey} />}
      {tab === 'operations' && <OperationsTab banks={banks} refresh={refresh} />}
      {tab === 'movements' && <MovementsTab banks={banks} refreshKey={refreshKey} />}
      {tab === 'banks' && <BanksTab banks={banks} refresh={refresh} />}
    </div>
  )
}
