import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import Modal from '../../../components/dialogs/Modal'
import {
  createLocation,
  InventoryLocation,
  InventoryLocationPayload,
  LocationType,
  updateLocation,
} from '../../../services/inventory'

type Props = {
  open: boolean
  location?: InventoryLocation | null
  onClose: () => void
  onSaved?: (location: InventoryLocation) => void
  existingCodes?: string[]
  existingNames?: string[]
}

type FormState = {
  code: string
  name: string
  description: string
  enabled: boolean
  type: LocationType
}

const CODE_PATTERN = /^[A-Z0-9-_]{2,32}$/
const TYPE_OPTIONS: { value: LocationType; label: string }[] = [
  { value: 'BODEGA', label: 'Bodega' },
  { value: 'LOCAL', label: 'Local' },
  { value: 'CONTAINER', label: 'Container' },
  { value: 'CAMION', label: 'Camión' },
  { value: 'CAMIONETA', label: 'Camioneta' },
]

const EMPTY_FORM: FormState = {
  code: '',
  name: '',
  description: '',
  enabled: true,
  type: 'BODEGA',
}

export default function LocationForm({
  open,
  location,
  onClose,
  onSaved,
  existingCodes = [],
  existingNames = [],
}: Props) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [errors, setErrors] = useState<Partial<Record<keyof FormState | 'form', string>>>({})

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM)
      setErrors({})
      return
    }
    if (location) {
      setForm({
        code: location.code ?? '',
        name: location.name ?? '',
        description: location.description ?? '',
        enabled: location.enabled,
        type: location.type ?? 'BODEGA',
      })
    } else {
      setForm(EMPTY_FORM)
    }
    setErrors({})
  }, [open, location])

  const normalizedCodes = useMemo(
    () => existingCodes.map(code => code.trim().toUpperCase()).filter(Boolean),
    [existingCodes]
  )
  const normalizedNames = useMemo(
    () => existingNames.map(name => name.trim().toLowerCase()).filter(Boolean),
    [existingNames]
  )

  const mutation = useMutation({
    mutationFn: (payload: InventoryLocationPayload) =>
      location ? updateLocation(location.id, payload) : createLocation(payload),
    onSuccess: saved => {
      onSaved?.(saved)
      onClose()
    },
    onError: error => {
      setErrors(prev => ({
        ...prev,
        form: error instanceof Error ? error.message : 'No se pudo guardar la ubicación',
      }))
    },
  })

  const handleChange = <K extends keyof FormState>(field: K, value: FormState[K]) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setErrors(prev => {
      if (!prev[field]) {
        return prev
      }
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const nextErrors: typeof errors = {}
    const code = form.code.trim().toUpperCase()
    const name = form.name.trim()

    if (!name) {
      nextErrors.name = 'El nombre es obligatorio'
    } else if (name.length < 2 || name.length > 80) {
      nextErrors.name = 'El nombre debe tener entre 2 y 80 caracteres'
    } else if (
      normalizedNames.includes(name.toLowerCase()) &&
      (!location || location.name.toLowerCase() !== name.toLowerCase())
    ) {
      nextErrors.name = 'Ya existe una ubicación con ese nombre'
    }

    if (!code) {
      nextErrors.code = 'El código es obligatorio'
    } else if (!CODE_PATTERN.test(code)) {
      nextErrors.code = 'Código inválido (solo A-Z, 0-9, -, _ ; 2-32 caracteres)'
    } else if (
      normalizedCodes.includes(code) &&
      (!location || (location.code ?? '').toUpperCase() !== code)
    ) {
      nextErrors.code = 'Ya existe una ubicación con ese código'
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors)
      return
    }

    const payload: InventoryLocationPayload = {
      code,
      name,
      description: form.description.trim() || undefined,
      enabled: form.enabled,
      type: form.type,
      status: form.enabled ? 'ACTIVE' : 'BLOCKED',
    }
    mutation.mutate(payload)
  }

  const title = location ? 'Editar ubicación' : 'Nueva ubicación'
  const isPending = mutation.isPending

  return (
    <Modal open={open} onClose={() => !isPending && onClose()} title={title}>
      <form className="form-grid" onSubmit={handleSubmit} noValidate>
        <label>
          <span>Nombre *</span>
          <input
            className={`input${errors.name ? ' input-error' : ''}`}
            value={form.name}
            onChange={event => handleChange('name', event.target.value)}
            placeholder="Bodega principal"
            disabled={isPending}
            required
            data-autofocus
          />
          {errors.name ? <p className="error">{errors.name}</p> : null}
        </label>
        <label>
          <span>Código *</span>
          <input
            className={`input${errors.code ? ' input-error' : ''}`}
            value={form.code}
            onChange={event => handleChange('code', event.target.value.toUpperCase())}
            placeholder="BOD-001"
            disabled={isPending}
            maxLength={32}
            required
          />
          {errors.code ? <p className="error">{errors.code}</p> : null}
        </label>
        <label>
          <span>Tipo *</span>
          <select
            className="input"
            value={form.type}
            onChange={event => handleChange('type', event.target.value as LocationType)}
            disabled={isPending}
            required
          >
            {TYPE_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Descripción</span>
          <textarea
            className="input"
            rows={3}
            value={form.description}
            onChange={event => handleChange('description', event.target.value)}
            placeholder="Detalles de la ubicación"
            disabled={isPending}
          />
        </label>
        <label>
          <span>Estado</span>
          <div className="inline-actions">
            <label className="inline-flex">
              <input
                type="checkbox"
                checked={form.enabled}
                onChange={event => handleChange('enabled', event.target.checked)}
                disabled={isPending}
              />
              <span className="muted small" style={{ marginLeft: '0.5rem' }}>
                Activa
              </span>
            </label>
          </div>
        </label>
        {errors.form ? (
          <p className="error" style={{ gridColumn: '1 / -1' }}>
            {errors.form}
          </p>
        ) : null}
        <div className="modal-actions" style={{ gridColumn: '1 / -1' }}>
          <button className="btn ghost" type="button" onClick={onClose} disabled={isPending}>
            Cancelar
          </button>
          <button className="btn" type="submit" disabled={isPending}>
            {isPending ? 'Guardando…' : location ? 'Guardar' : 'Crear'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
