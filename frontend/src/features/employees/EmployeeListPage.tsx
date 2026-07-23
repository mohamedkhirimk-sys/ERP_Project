import { useState, useEffect } from 'react'
import api from '@/lib/axios'

interface Employee {
  id: number
  employeeId: string
  firstName: string
  lastName: string
  email: string
  phone: string
  department: string
  position: string
  salary: number
  status: string
}

export default function EmployeeListPage() {
  const [employees, setEmployees] = useState<Employee[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/employees')
      .then((res) => setEmployees(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading...</p>

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Employees</h1>
      <table className="w-full bg-white rounded-lg border">
        <thead>
          <tr className="border-b bg-gray-50 text-left">
            <th className="px-4 py-2">ID</th>
            <th className="px-4 py-2">Name</th>
            <th className="px-4 py-2">Email</th>
            <th className="px-4 py-2">Department</th>
            <th className="px-4 py-2">Position</th>
            <th className="px-4 py-2">Salary</th>
            <th className="px-4 py-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((e) => (
            <tr key={e.id} className="border-b hover:bg-gray-50">
              <td className="px-4 py-2 font-mono text-sm">{e.employeeId}</td>
              <td className="px-4 py-2">{e.firstName} {e.lastName}</td>
              <td className="px-4 py-2">{e.email}</td>
              <td className="px-4 py-2">{e.department}</td>
              <td className="px-4 py-2">{e.position}</td>
              <td className="px-4 py-2">${e.salary}</td>
              <td className="px-4 py-2">{e.status}</td>
            </tr>
          ))}
          {employees.length === 0 && (
            <tr><td colSpan={7} className="px-4 py-4 text-center text-gray-500">No employees yet</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
