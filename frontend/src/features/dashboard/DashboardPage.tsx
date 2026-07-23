import { Link } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'

const cards = [
  { title: 'Products', desc: 'Manage product catalog', link: '/products' },
  { title: 'Inventory', desc: 'View stock levels', link: '/inventory' },
  { title: 'Orders', desc: 'Process customer orders', link: '/orders' },
  { title: 'Customers', desc: 'View customer records', link: '/customers' },
  { title: 'Invoices', desc: 'Manage invoices', link: '/invoices' },
  { title: 'Vendors', desc: 'Manage vendors', link: '/vendors' },
]

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <div>
      <h1 className="text-2xl font-bold mb-2">Welcome, {user?.username}</h1>
      <p className="text-gray-500 mb-6">Role: {user?.role}</p>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {cards.map((card) => (
          <Link
            key={card.title}
            to={card.link}
            className="block p-6 bg-white rounded-lg border hover:shadow-md transition-shadow"
          >
            <h2 className="text-lg font-semibold">{card.title}</h2>
            <p className="text-gray-500 text-sm mt-1">{card.desc}</p>
          </Link>
        ))}
      </div>
    </div>
  )
}
