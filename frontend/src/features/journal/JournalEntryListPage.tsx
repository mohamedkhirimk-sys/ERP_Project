import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface JournalEntry {
  id: number
  entryNumber: string
  accountName: string
  accountCode: string
  description: string
  debit: number
  credit: number
  entryDate: string
}

export default function JournalEntryListPage() {
  const [entries, setEntries] = useState<JournalEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/journal-entries')
      .then((res) => setEntries(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Journal Entries</h1>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Entry #</th>
            <th className="px-4 py-2">Account</th>
            <th className="px-4 py-2">Description</th>
            <th className="px-4 py-2">Debit</th>
            <th className="px-4 py-2">Credit</th>
            <th className="px-4 py-2">Date</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((e) => (
            <tr key={e.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2 font-mono text-sm">{e.entryNumber}</td>
              <td className="px-4 py-2">{e.accountName} ({e.accountCode})</td>
              <td className="px-4 py-2">{e.description}</td>
              <td className="px-4 py-2">${e.debit}</td>
              <td className="px-4 py-2">${e.credit}</td>
              <td className="px-4 py-2 text-sm">{e.entryDate}</td>
            </tr>
          ))}
          {entries.length === 0 && (
            <tr><td colSpan={6} className="px-4 py-4 text-center text-gray-500">No journal entries yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
