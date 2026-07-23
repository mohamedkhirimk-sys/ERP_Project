import { useState, useEffect, type FormEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '@/lib/axios'

const labelClass = 'block text-sm font-medium text-gray-700 mb-1'
const inputClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition'
const selectClass = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition bg-white'

export default function EmployeeFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState({
    employeeId: '', firstName: '', lastName: '', email: '',
    phone: '', department: '', position: '', salary: '', status: 'ACTIVE',
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (id) {
      api.get(`/api/employees/${id}`).then((res) => {
        const e = res.data
        setForm({
          employeeId: e.employeeId, firstName: e.firstName, lastName: e.lastName,
          email: e.email, phone: e.phone, department: e.department,
          position: e.position, salary: String(e.salary), status: e.status,
        })
      }).catch(console.error)
    }
  }, [id])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const payload = { ...form, salary: Number(form.salary) }
      if (isEdit) {
        await api.put(`/api/employees/${id}`, payload)
      } else {
        await api.post('/api/employees', payload)
      }
      navigate('/employees')
    } catch (err) {
      console.error(err)
      alert('Failed to save employee')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">{isEdit ? 'Edit Employee' : 'New Employee'}</h1>
          <p className="text-sm text-gray-500">{isEdit ? 'Update employee information' : 'Add a new employee to the system'}</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className={labelClass}>Employee ID</label>
            <input value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })} className={inputClass} placeholder="e.g. EMP-001" required />
          </div>
          <div>
            <label className={labelClass}>Status</label>
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} className={selectClass}>
              <option>ACTIVE</option>
              <option>INACTIVE</option>
              <option>TERMINATED</option>
            </select>
          </div>
          <div>
            <label className={labelClass}>First Name</label>
            <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} className={inputClass} placeholder="John" required />
          </div>
          <div>
            <label className={labelClass}>Last Name</label>
            <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} className={inputClass} placeholder="Doe" required />
          </div>
          <div>
            <label className={labelClass}>Email</label>
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className={inputClass} placeholder="john@company.com" required />
          </div>
          <div>
            <label className={labelClass}>Phone</label>
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} className={inputClass} placeholder="+1 234 567 890" />
          </div>
          <div>
            <label className={labelClass}>Department</label>
            <input value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} className={inputClass} placeholder="Engineering" />
          </div>
          <div>
            <label className={labelClass}>Position</label>
            <input value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })} className={inputClass} placeholder="Software Engineer" />
          </div>
          <div>
            <label className={labelClass}>Salary</label>
            <input type="number" step="0.01" value={form.salary} onChange={(e) => setForm({ ...form, salary: e.target.value })} className={inputClass} placeholder="50000" />
          </div>
        </div>

        <div className="flex items-center gap-3 pt-4 border-t">
          <button type="submit" disabled={loading} className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition flex items-center gap-2">
            {loading ? (
              <><svg className="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Saving...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>Save Employee</>
            )}
          </button>
          <button type="button" onClick={() => navigate('/employees')} className="px-6 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
