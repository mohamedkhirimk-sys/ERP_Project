import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '@/lib/axios'

interface Invoice {
  id: number
  invoiceNumber: string
  customerId: number
  customerName: string
  totalAmount: number
  status: string
  dueDate: string | null
}

export default function InvoiceListPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/invoices')
      .then((res) => setInvoices(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Invoices</h1>
        <Link to="/invoices/new" className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700">+ New Invoice</Link>
      </div>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Invoice #</th>
            <th className="px-4 py-2">Customer</th>
            <th className="px-4 py-2">Amount</th>
            <th className="px-4 py-2">Status</th>
            <th className="px-4 py-2">Due Date</th>
          </tr>
        </thead>
        <tbody>
          {invoices.map((inv) => (
            <tr key={inv.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2 font-mono text-sm">{inv.invoiceNumber}</td>
              <td className="px-4 py-2">{inv.customerName}</td>
              <td className="px-4 py-2">${inv.totalAmount}</td>
              <td className="px-4 py-2">{inv.status}</td>
              <td className="px-4 py-2">{inv.dueDate ? new Date(inv.dueDate).toLocaleDateString() : '-'}</td>
            </tr>
          ))}
          {invoices.length === 0 && (
            <tr><td colSpan={5} className="px-4 py-4 text-center text-gray-500">No invoices yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
