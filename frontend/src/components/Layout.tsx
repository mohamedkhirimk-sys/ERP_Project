import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

const navItems = [
  { label: 'Dashboard', path: '/' },
  { label: 'Products', path: '/products' },
  { label: 'Stock', path: '/inventory' },
  { label: 'Orders', path: '/orders' },
  { label: 'Customers', path: '/customers' },
  { label: 'Invoices', path: '/invoices' },
  { label: 'Vendors', path: '/vendors' },
  { label: 'Purchase Orders', path: '/purchase-orders' },
  { label: 'Employees', path: '/employees' },
  { label: 'Accounts', path: '/accounts' },
  { label: 'Journal', path: '/journal' },
  { label: 'Payroll', path: '/payroll' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <aside className="w-64 bg-white border-r border-gray-200 p-4 flex flex-col">
        <h1 className="text-xl font-bold mb-8">ERP System</h1>
        <nav className="flex flex-col gap-2 flex-1">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`px-3 py-2 rounded-md text-sm ${
                location.pathname === item.path
                  ? 'bg-blue-100 text-blue-700 font-medium'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="border-t pt-4">
          <p className="text-sm text-gray-500 mb-2">{user?.username} ({user?.role})</p>
          <button onClick={handleLogout} className="text-sm text-red-600 hover:underline">
            Logout
          </button>
        </div>
      </aside>
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  )
}
