import { Fragment, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface JournalSummary {
  code: string
  label: string
  entryCount: number
  totalDebit: number
  totalCredit: number
}

interface JournalLine {
  id: number
  accountId: number
  accountName: string
  accountCode: string
  debit: number
  credit: number
}

interface JournalEntryDetail {
  id: number
  entryNumber: string
  description: string
  entryDate: string
  lines: JournalLine[]
}

interface JournalDetail {
  code: string
  label: string
  entries: JournalEntryDetail[]
  totalDebit: number
  totalCredit: number
}

const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'

export default function AuxiliaryJournalsPage() {
  const [summaries, setSummaries] = useState<JournalSummary[]>([])
  const [selected, setSelected] = useState('VTE')
  const [detail, setDetail] = useState<JournalDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [expanded, setExpanded] = useState<number | null>(null)

  useEffect(() => {
    api.get('/api/journals')
      .then((res) => setSummaries(res.data))
      .catch(console.error)
  }, [])

  useEffect(() => {
    setLoading(true)
    api.get(`/api/journals/${selected}`, { params: { from: from || undefined, to: to || undefined } })
      .then((res) => setDetail(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [selected, from, to])

  const current = summaries.find((s) => s.code === selected)

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
            <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Journals</h1>
            <p className="text-sm text-gray-500">Auxiliary journals — VTE, ENC, ACH, DEC, BNQ, OD</p>
          </div>
        </div>
        <Link to="/journal/new" className="bg-blue-600 text-white px-4 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 transition flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
          New Entry
        </Link>
      </div>

      <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-4 flex-wrap w-fit">
        {summaries.map((s) => (
          <button
            key={s.code}
            onClick={() => setSelected(s.code)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition ${
              selected === s.code ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {s.code} · {s.entryCount}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-4 mb-4">
        <div className="w-48">
          <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">From</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={inputClass} />
        </div>
        <div className="w-48">
          <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">To</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className={inputClass} />
        </div>
        {current && (
          <p className="text-sm text-gray-500 ml-auto">
            <span className="font-semibold text-gray-900">{current.label}</span> — {detail?.entries.length ?? 0} entries
          </p>
        )}
      </div>

      {loading ? (
        <p className="text-gray-500 py-12 text-center">Loading...</p>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50 text-left">
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Entry #</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Date</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Description</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-right">Debit</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-right">Credit</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {detail?.entries.map((e) => {
                const isOpen = expanded === e.id
                const totalDebit = e.lines.reduce((s, l) => s + l.debit, 0)
                const totalCredit = e.lines.reduce((s, l) => s + l.credit, 0)
                return (
                  <Fragment key={e.id}>
                    <tr onClick={() => setExpanded(isOpen ? null : e.id)} className="hover:bg-gray-50 transition cursor-pointer">
                      <td className="px-4 py-3 font-mono text-sm text-gray-600">{e.entryNumber}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{new Date(e.entryDate).toLocaleString()}</td>
                      <td className="px-4 py-3 text-sm text-gray-900">{e.description}</td>
                      <td className="px-4 py-3 font-mono text-sm text-gray-900 text-right">{totalDebit > 0 ? `$${totalDebit.toFixed(2)}` : '—'}</td>
                      <td className="px-4 py-3 font-mono text-sm text-gray-900 text-right">{totalCredit > 0 ? `$${totalCredit.toFixed(2)}` : '—'}</td>
                    </tr>
                    {isOpen && (
                      <tr>
                        <td colSpan={5} className="px-6 pb-3 bg-gray-50/50">
                          <table className="w-full text-sm">
                            <tbody className="divide-y divide-gray-100">
                              {e.lines.map((l) => (
                                <tr key={l.id}>
                                  <td className="py-1.5 px-2 font-mono text-gray-600 w-32">{l.accountCode}</td>
                                  <td className="py-1.5 px-2 text-gray-700">{l.accountName}</td>
                                  <td className="py-1.5 px-2 font-mono text-right w-32">{l.debit > 0 ? `$${l.debit.toFixed(2)}` : '—'}</td>
                                  <td className="py-1.5 px-2 font-mono text-right w-32">{l.credit > 0 ? `$${l.credit.toFixed(2)}` : '—'}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                )
              })}
              {detail && detail.entries.length === 0 && (
                <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-500">No entries in this journal{from || to ? ' for the selected period' : ''}</td></tr>
              )}
            </tbody>
            {detail && detail.entries.length > 0 && (
              <tfoot>
                <tr className="border-t border-gray-200 bg-gray-50">
                  <td colSpan={3} className="px-4 py-3 text-right text-sm font-semibold text-gray-700">Journal totals</td>
                  <td className="px-4 py-3 text-right font-mono text-sm font-semibold text-gray-900">${detail.totalDebit.toFixed(2)}</td>
                  <td className="px-4 py-3 text-right font-mono text-sm font-semibold text-gray-900">${detail.totalCredit.toFixed(2)}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      )}
    </div>
  )
}
