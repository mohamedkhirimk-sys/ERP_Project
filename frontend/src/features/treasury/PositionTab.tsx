import { useEffect, useState } from 'react'
import { type TreasuryPosition, getPosition } from './api'

export default function PositionTab({ refreshKey }: { refreshKey: number }) {
  const [position, setPosition] = useState<TreasuryPosition | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getPosition()
      .then(setPosition)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [refreshKey])

  if (loading) return <p className="text-gray-500 py-12 text-center">Loading...</p>
  if (!position || position.banks.length === 0) {
    return <p className="text-gray-500 py-12 text-center">No bank accounts yet — create one in the Banks tab</p>
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {position.banks.map((b) => (
        <div key={b.bankId} className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-semibold text-gray-900">{b.bankName}</p>
              <p className="text-xs text-gray-500 font-mono">{b.accountNumber}</p>
            </div>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
              {b.accountCode}
            </span>
          </div>
          <p className="mt-4 text-2xl font-bold text-gray-900">${b.balance.toFixed(2)}</p>
        </div>
      ))}
      <div className="bg-blue-600 rounded-xl shadow-sm p-5 text-white">
        <p className="text-sm font-medium opacity-80">Total cash</p>
        <p className="mt-4 text-2xl font-bold">${position.total.toFixed(2)}</p>
      </div>
    </div>
  )
}
