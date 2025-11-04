import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createUserAccount,
  listUserAccounts,
  type CreateUserAccountPayload,
  type UpdateUserAccountPayload,
  type UpdateUserPasswordPayload,
  type UserAccount,
  updateUserAccount,
  updateUserPassword,
} from '../../services/client'
import Modal from '../dialogs/Modal'
import { parseProblemDetail } from '../../utils/problemDetail'

const ROLE_OPTIONS = [
  { value: 'ROLE_ADMIN', label: 'Administrador' },
  { value: 'ROLE_SALES', label: 'Ventas' },
  { value: 'ROLE_PURCHASES', label: 'Compras' },
  { value: 'ROLE_INVENTORY', label: 'Inventario' },
  { value: 'ROLE_FINANCE', label: 'Finanzas' },
  { value: 'ROLE_REPORTS', label: 'Reportes' },
  { value: 'ROLE_SETTINGS', label: 'Configuracion' },
] as const

const STATUS_OPTIONS = [
  { value: 'active', label: 'Activo' },
  { value: 'disabled', label: 'Suspendido' },
] as const

type UserFormSubmission = {
  email: string
  name: string
  roles: string[]
  status: string
  password?: string
}

function generatePassword(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'
  let value = ''
  for (let index = 0; index < 10; index += 1) {
    value += chars[Math.floor(Math.random() * chars.length)]
  }
  return value
}

export default function UserManagementCard() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserAccount | null>(null)
  const [passwordUser, setPasswordUser] = useState<UserAccount | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)

  const usersQuery = useQuery<UserAccount[], Error>({
    queryKey: ['users'],
    queryFn: listUserAccounts,
    refetchOnWindowFocus: false,
  })

  const createMutation = useMutation({
    mutationFn: (payload: CreateUserAccountPayload) => createUserAccount(payload),
    onSuccess: created => {
      queryClient.invalidateQueries({ queryKey: ['users'], exact: false })
      setFeedback(
        `Usuario ${created.email} creado. Comparte el enlace de acceso con la nueva clave.`
      )
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateUserAccountPayload }) =>
      updateUserAccount(id, payload),
    onSuccess: updated => {
      queryClient.invalidateQueries({ queryKey: ['users'], exact: false })
      setFeedback(`Usuario ${updated.email} actualizado.`)
    },
  })

  const passwordMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateUserPasswordPayload }) =>
      updateUserPassword(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'], exact: false })
    },
  })

  const users = usersQuery.data ?? []
  const isLoading = usersQuery.isLoading
  const loadError = usersQuery.isError ? usersQuery.error : null

  const handleCreateSubmit = async (submission: UserFormSubmission) => {
    const payload: CreateUserAccountPayload = {
      email: submission.email.trim(),
      name: submission.name.trim(),
      roles: submission.roles,
      status: submission.status,
      password: submission.password ?? '',
      role: submission.roles[0] ?? undefined,
    }
    try {
      await createMutation.mutateAsync(payload)
      setCreateOpen(false)
    } catch (error) {
      const parsed = parseProblemDetail(error)
      throw new Error(parsed.message ?? 'No se pudo crear el usuario')
    }
  }

  const handleUpdateSubmit = async (submission: UserFormSubmission, userId: string) => {
    const payload: UpdateUserAccountPayload = {
      email: submission.email.trim(),
      name: submission.name.trim(),
      roles: submission.roles,
      status: submission.status,
      role: submission.roles[0] ?? undefined,
    }
    try {
      await updateMutation.mutateAsync({ id: userId, payload })
      setEditingUser(null)
    } catch (error) {
      const parsed = parseProblemDetail(error)
      throw new Error(parsed.message ?? 'No se pudo actualizar el usuario')
    }
  }

  const handlePasswordSubmit = async (password: string, userId: string) => {
    const payload: UpdateUserPasswordPayload = { password }
    try {
      const currentEmail = passwordUser?.email ?? ''
      await passwordMutation.mutateAsync({ id: userId, payload })
      setFeedback(
        currentEmail ? `Contrasena actualizada para ${currentEmail}.` : 'Contrasena actualizada.'
      )
      setPasswordUser(null)
    } catch (error) {
      const parsed = parseProblemDetail(error)
      throw new Error(parsed.message ?? 'No se pudo actualizar la contrasena')
    }
  }

  const roleDescriptions = useMemo(() => {
    const map = new Map<string, string>()
    for (const entry of ROLE_OPTIONS) {
      map.set(entry.value, entry.label)
    }
    return map
  }, [])

  return (
    <div className="card">
      <div className="card-header">
        <h2>Usuarios y accesos</h2>
        <div className="card-actions">
          <button
            className="btn-link"
            onClick={() => usersQuery.refetch()}
            disabled={usersQuery.isFetching}
          >
            {usersQuery.isFetching ? 'Actualizando...' : 'Actualizar'}
          </button>
          <button
            className="btn-link"
            onClick={() => setCreateOpen(true)}
            disabled={createMutation.isPending}
          >
            Nuevo usuario
          </button>
        </div>
      </div>
      <div className="card-body">
        {isLoading && <p>Cargando usuarios...</p>}
        {loadError && (
          <p className="error" aria-live="assertive">
            {loadError.message ?? 'No se pudieron cargar los usuarios'}
          </p>
        )}
        {!isLoading && !loadError && users.length === 0 && (
          <p className="muted">Aun no se registran usuarios para esta cuenta.</p>
        )}
        {!isLoading && !loadError && users.length > 0 && (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Nombre</th>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Correo</th>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Roles</th>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Estado</th>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Creado</th>
                  <th style={{ textAlign: 'left', padding: '8px' }} aria-label="Acciones" />
                </tr>
              </thead>
              <tbody>
                {users.map(user => {
                  const roleText =
                    user.roles.length > 0
                      ? user.roles.map(role => roleDescriptions.get(role) ?? role).join(', ')
                      : '-'
                  return (
                    <tr key={user.id} style={{ borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                      <td style={{ padding: '8px' }}>{user.name || 'Sin nombre'}</td>
                      <td style={{ padding: '8px', fontFamily: 'monospace', fontSize: '0.9rem' }}>
                        {user.email}
                      </td>
                      <td style={{ padding: '8px' }}>{roleText}</td>
                      <td style={{ padding: '8px' }}>
                        {user.status === 'active' ? 'Activo' : 'Suspendido'}
                      </td>
                      <td style={{ padding: '8px', fontSize: '0.9rem' }}>
                        {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/D'}
                      </td>
                      <td style={{ padding: '8px' }}>
                        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                          <button className="btn-link" onClick={() => setEditingUser(user)}>
                            Editar
                          </button>
                          <button className="btn-link" onClick={() => setPasswordUser(user)}>
                            Reset clave
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        {feedback ? (
          <p className="success" aria-live="polite">
            {feedback}
          </p>
        ) : null}
      </div>

      <UserFormDialog
        mode="create"
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSubmit={handleCreateSubmit}
        isSubmitting={createMutation.isPending}
      />

      <UserFormDialog
        mode="edit"
        open={!!editingUser}
        onClose={() => setEditingUser(null)}
        onSubmit={values =>
          editingUser ? handleUpdateSubmit(values, editingUser.id) : Promise.resolve()
        }
        initialUser={editingUser ?? undefined}
        isSubmitting={updateMutation.isPending}
      />

      <PasswordResetDialog
        open={!!passwordUser}
        onClose={() => setPasswordUser(null)}
        user={passwordUser ?? undefined}
        onSubmit={async password => {
          if (!passwordUser) {
            return
          }
          await handlePasswordSubmit(password, passwordUser.id)
        }}
        isSubmitting={passwordMutation.isPending}
      />
    </div>
  )
}

type UserFormDialogProps = {
  mode: 'create' | 'edit'
  open: boolean
  onClose: () => void
  initialUser?: UserAccount
  onSubmit: (submission: UserFormSubmission) => Promise<void>
  isSubmitting: boolean
}

function UserFormDialog({
  mode,
  open,
  onClose,
  initialUser,
  onSubmit,
  isSubmitting,
}: UserFormDialogProps) {
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [status, setStatus] = useState('active')
  const [roles, setRoles] = useState<Set<string>>(new Set(['ROLE_ADMIN']))
  const [password, setPassword] = useState(mode === 'create' ? generatePassword() : '')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)

  const resetState = () => {
    if (mode === 'create') {
      setEmail('')
      setName('')
      setStatus('active')
      setRoles(new Set(['ROLE_ADMIN']))
      setPassword(generatePassword())
    } else if (initialUser) {
      setEmail(initialUser.email ?? '')
      setName(initialUser.name ?? '')
      setStatus(initialUser.status ?? 'active')
      setRoles(new Set(initialUser.roles ?? []))
      setPassword('')
    }
    setFieldErrors({})
    setFormError(null)
  }

  useEffect(() => {
    if (!open) {
      return
    }
    if (mode === 'create') {
      setEmail('')
      setName('')
      setStatus('active')
      setRoles(new Set(['ROLE_ADMIN']))
      setPassword(generatePassword())
    } else if (initialUser) {
      setEmail(initialUser.email ?? '')
      setName(initialUser.name ?? '')
      setStatus(initialUser.status ?? 'active')
      setRoles(new Set(initialUser.roles ?? []))
      setPassword('')
    }
    setFieldErrors({})
    setFormError(null)
  }, [open, mode, initialUser])

  if (!open) {
    return null
  }

  const toggleRole = (value: string) => {
    setRoles(prev => {
      const next = new Set(prev)
      if (next.has(value)) {
        next.delete(value)
      } else {
        next.add(value)
      }
      return next
    })
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const nextFieldErrors: Record<string, string> = {}
    const trimmedEmail = email.trim()
    const trimmedName = name.trim()
    if (!trimmedEmail) {
      nextFieldErrors.email = 'El correo es obligatorio'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      nextFieldErrors.email = 'El formato del correo no es valido'
    }
    if (!trimmedName) {
      nextFieldErrors.name = 'El nombre es obligatorio'
    }
    const selectedRoles = Array.from(roles)
    if (selectedRoles.length === 0) {
      nextFieldErrors.roles = 'Selecciona al menos un rol'
    }
    if (mode === 'create') {
      const trimmedPassword = password.trim()
      if (!trimmedPassword) {
        nextFieldErrors.password = 'La contrasena es obligatoria'
      } else if (trimmedPassword.length < 8) {
        nextFieldErrors.password = 'La contrasena debe tener al menos 8 caracteres'
      }
    }

    setFieldErrors(nextFieldErrors)
    setFormError(null)
    if (Object.keys(nextFieldErrors).length > 0) {
      return
    }

    try {
      await onSubmit({
        email: trimmedEmail,
        name: trimmedName,
        status,
        roles: selectedRoles,
        password: mode === 'create' ? password.trim() : undefined,
      })
      resetState()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'No se pudo guardar el usuario')
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => {
        resetState()
        onClose()
      }}
      title={mode === 'create' ? 'Crear usuario' : 'Editar usuario'}
    >
      <form className="form-grid" onSubmit={handleSubmit} noValidate>
        <label className="full">
          <span>Correo *</span>
          <input
            className="input"
            value={email}
            onChange={event => {
              setEmail(event.target.value)
              setFieldErrors(prev => {
                if (!prev.email) return prev
                const next = { ...prev }
                delete next.email
                return next
              })
            }}
            disabled={isSubmitting}
            autoComplete="email"
          />
          {fieldErrors.email ? (
            <p className="error" aria-live="polite">
              {fieldErrors.email}
            </p>
          ) : null}
        </label>

        <label className="full">
          <span>Nombre *</span>
          <input
            className="input"
            value={name}
            onChange={event => {
              setName(event.target.value)
              setFieldErrors(prev => {
                if (!prev.name) return prev
                const next = { ...prev }
                delete next.name
                return next
              })
            }}
            disabled={isSubmitting}
            autoComplete="name"
          />
          {fieldErrors.name ? (
            <p className="error" aria-live="polite">
              {fieldErrors.name}
            </p>
          ) : null}
        </label>

        <fieldset className="full">
          <legend>Roles *</legend>
          <div
            style={{
              display: 'grid',
              gap: '8px',
              gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
            }}
          >
            {ROLE_OPTIONS.map(option => (
              <label key={option.value} className="checkbox">
                <input
                  type="checkbox"
                  checked={roles.has(option.value)}
                  onChange={() => {
                    toggleRole(option.value)
                    setFieldErrors(prev => {
                      if (!prev.roles) return prev
                      const next = { ...prev }
                      delete next.roles
                      return next
                    })
                  }}
                  disabled={isSubmitting && !roles.has(option.value)}
                />
                <span>{option.label}</span>
              </label>
            ))}
          </div>
          {fieldErrors.roles ? (
            <p className="error" aria-live="polite">
              {fieldErrors.roles}
            </p>
          ) : null}
        </fieldset>

        <label>
          <span>Estado</span>
          <select
            className="input"
            value={status}
            onChange={event => setStatus(event.target.value)}
            disabled={isSubmitting}
          >
            {STATUS_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        {mode === 'create' && (
          <label className="full">
            <span>Contrasena inicial *</span>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              <input
                className="input"
                value={password}
                onChange={event => {
                  setPassword(event.target.value)
                  setFieldErrors(prev => {
                    if (!prev.password) return prev
                    const next = { ...prev }
                    delete next.password
                    return next
                  })
                }}
                disabled={isSubmitting}
                autoComplete="new-password"
              />
              <button
                type="button"
                className="btn ghost"
                onClick={() => setPassword(generatePassword())}
                disabled={isSubmitting}
              >
                Generar
              </button>
            </div>
            {fieldErrors.password ? (
              <p className="error" aria-live="polite">
                {fieldErrors.password}
              </p>
            ) : null}
            <p className="muted small">
              Entrega este valor junto con el enlace de acceso: http://localhost:5173/login
            </p>
          </label>
        )}

        {formError ? (
          <p className="error" aria-live="assertive">
            {formError}
          </p>
        ) : null}

        <div className="buttons">
          <button className="btn" type="submit" disabled={isSubmitting}>
            {isSubmitting
              ? 'Guardando...'
              : mode === 'create'
                ? 'Crear usuario'
                : 'Guardar cambios'}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => {
              resetState()
              onClose()
            }}
            disabled={isSubmitting}
          >
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  )
}

type PasswordResetDialogProps = {
  open: boolean
  onClose: () => void
  user?: UserAccount
  onSubmit: (password: string) => Promise<void>
  isSubmitting: boolean
}

function PasswordResetDialog({
  open,
  onClose,
  user,
  onSubmit,
  isSubmitting,
}: PasswordResetDialogProps) {
  const [password, setPassword] = useState(generatePassword())
  const [formError, setFormError] = useState<string | null>(null)

  if (!open || !user) {
    return null
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmed = password.trim()
    if (trimmed.length < 8) {
      setFormError('La contrasena debe tener al menos 8 caracteres')
      return
    }
    try {
      await onSubmit(trimmed)
      setFormError(null)
      setPassword(generatePassword())
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'No se pudo actualizar la contrasena')
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => {
        setPassword(generatePassword())
        setFormError(null)
        onClose()
      }}
      title={`Restablecer contrasena`}
    >
      <form className="stack gap-sm" onSubmit={handleSubmit} noValidate>
        <p>
          Se generara una nueva contrasena para <strong>{user.email}</strong>. Comparte este valor y
          pide que la cambien al ingresar.
        </p>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <input
            className="input"
            value={password}
            onChange={event => setPassword(event.target.value)}
            disabled={isSubmitting}
            autoComplete="new-password"
          />
          <button
            type="button"
            className="btn ghost"
            onClick={() => setPassword(generatePassword())}
            disabled={isSubmitting}
          >
            Generar
          </button>
        </div>
        {formError ? (
          <p className="error" aria-live="assertive">
            {formError}
          </p>
        ) : null}
        <div className="buttons">
          <button className="btn" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Guardando...' : 'Guardar nueva contrasena'}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => {
              setPassword(generatePassword())
              setFormError(null)
              onClose()
            }}
            disabled={isSubmitting}
          >
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  )
}
