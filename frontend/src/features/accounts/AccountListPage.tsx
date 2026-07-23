import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface Account {
  id: number
  accountCode: string
  accountName: string
  accountType: string
  description: string
  balance: number
}

export default function AccountListPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/accounts')
      .then((res) => setAccounts(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Chart of Accounts</h1>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Code</th>
            <th className="px-4 py-2">Name</th>
            <th className="px-4 py-2">Type</th>
            <th className="px-4 py-2">Description</th>
            <th className="px-4 py-2">Balance</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((a) => (
            <tr key={a.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2 font-mono text-sm">{a.accountCode}</td>
              <td className="px-4 py-2">{a.accountName}</td>
              <td className="px-4 py-2">{a.accountType}</td>
              <td className="px-4 py-2">{a.description}</td>
              <td className="px-4 py-2">${a.balance}</td>
            </tr>
          ))}
          {accounts.length === 0 && (
            <tr><td colSpan={5} className="px-4 py-4 text-center text-gray-500">No accounts yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
