import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import type { AuthSession, LoginPayload, LoginResponse, RefreshPayload } from '../services/client'
import { clearSession, login as apiLogin, refreshAuth, setSession } from '../services/client'

const STORAGE_KEY = 'pymes.auth.session'

type AuthContextValue = {
  session: AuthSession | null
  isAuthenticated: boolean
  login: (payload: LoginPayload) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function loadSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as AuthSession
    const normalized: AuthSession = {
      ...parsed,
      modules: Array.isArray(parsed.modules) ? parsed.modules : [],
    }
    if (
      !normalized?.token ||
      !normalized?.expiresAt ||
      !normalized?.refreshToken ||
      !normalized?.refreshExpiresAt
    ) {
      return null
    }
    const now = Date.now()
    if (normalized.expiresAt <= now || normalized.refreshExpiresAt <= now) {
      return null
    }
    return normalized
  } catch (error) {
    return null
  }
}

function persistSession(session: AuthSession | null) {
  if (!session) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSessionState] = useState<AuthSession | null>(() => loadSession())

  useEffect(() => {
    if (session) {
      setSession({
        token: session.token,
        companyId: session.companyId,
        refreshToken: session.refreshToken,
      })
      persistSession(session)
    } else {
      clearSession()
      persistSession(null)
    }
  }, [session])

  const login = useCallback(async (payload: LoginPayload) => {
    const response: LoginResponse = await apiLogin(payload)
    const now = Date.now()
    const authSession: AuthSession = {
      ...response,
      modules: Array.isArray(response.modules) ? response.modules : [],
      expiresAt: now + response.expiresIn * 1000,
      refreshExpiresAt: now + response.refreshExpiresIn * 1000,
    }
    setSessionState(authSession)
  }, [])

  const logout = useCallback(() => {
    setSessionState(null)
  }, [])

  const refresh = useCallback(async () => {
    if (!session) {
      return
    }
    const payload: RefreshPayload = {
      refreshToken: session.refreshToken,
    }
    const response = await refreshAuth(payload)
    const now = Date.now()
    const updated: AuthSession = {
      ...response,
      modules: Array.isArray(response.modules) ? response.modules : [],
      expiresAt: now + response.expiresIn * 1000,
      refreshExpiresAt: now + response.refreshExpiresIn * 1000,
    }
    setSessionState(updated)
  }, [session])

  useEffect(() => {
    if (!session) {
      return
    }
    const now = Date.now()
    if (session.refreshExpiresAt <= now) {
      setSessionState(null)
      return
    }
    const refreshDelay = Math.max(session.expiresAt - now - 60_000, 5_000)
    const timer = window.setTimeout(() => {
      refresh().catch(() => setSessionState(null))
    }, refreshDelay)
    return () => window.clearTimeout(timer)
  }, [session, refresh])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: !!session,
      login,
      logout,
    }),
    [session, login, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
