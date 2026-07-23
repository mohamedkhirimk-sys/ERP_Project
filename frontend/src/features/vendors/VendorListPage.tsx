import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface Vendor {
  id: number
  name: string
  email: string
  phone: string
  address: string
}

export default function VendorListPage() {
  const [vendors, setVendors] = useState<Vendor[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', email: '', phone: '', address: '' })

  const fetchVendors = () =>
    api.get('/api/vendors').then((res) => setVendors(res.data))

  useEffect(() => { fetchVendors().finally(() => setLoading(false)) }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await api.post('/api/vendors', form)
    setForm({ name: '', email: '', phone: '', address: '' })
    setShowForm(false)
    fetchVendors()
  }

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Vendors</h1>
        <button onClick={() => setShowForm(!showForm)} className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700">
          {showForm ? 'Cancel' : '+ New Vendor'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white p-4 rounded-lg border mb-4 grid grid-cols-2 gap-3">
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Name" className="border rounded px-3 py-2" required />
          <input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="Email" type="email" className="border rounded px-3 py-2" required />
          <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} placeholder="Phone" className="border rounded px-3 py-2" />
          <input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} placeholder="Address" className="border rounded px-3 py-2" />
          <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded text-sm hover:bg-green-700 col-span-2">Save</button>
        </form>
      )}

      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">Name</th>
            <th className="px-4 py-2">Email</th>
            <th className="px-4 py-2">Phone</th>
            <th className="px-4 py-2">Address</th>
          </tr>
        </thead>
        <tbody>
          {vendors.map((v) => (
            <tr key={v.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2">{v.name}</td>
              <td className="px-4 py-2">{v.email}</td>
              <td className="px-4 py-2">{v.phone}</td>
              <td className="px-4 py-2">{v.address}</td>
            </tr>
          ))}
          {vendors.length === 0 && (
            <tr><td colSpan={4} className="px-4 py-4 text-center text-gray-500">No vendors yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
