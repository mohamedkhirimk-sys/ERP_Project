import { useState, useCallback } from 'react'
import api from '@/lib/axios'

interface User {
  username: string
  role: string
}

const DEFAULT_USER: User = { username: 'Developer', role: 'ADMIN' }

export function useAuth() {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : DEFAULT_USER
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const login = useCallback(async (username: string, password: string) => {
    setLoading(true)
    setError(null)
    try {
      const res = await api.post('/api/auth/login', { username, password })
      const token = res.data
      localStorage.setItem('token', token)
      const payload = JSON.parse(atob(token.split('.')[1]))
      const user: User = { username: payload.sub, role: payload.role }
      localStorage.setItem('user', JSON.stringify(user))
      setUser(user)
    } catch {
      setError('Invalid credentials')
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }, [])

  return { user, loading, error, login, logout }
}
