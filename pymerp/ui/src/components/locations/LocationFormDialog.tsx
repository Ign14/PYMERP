import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  Location,
  LocationPayload,
  LocationStatus,
  LocationType,
  createLocation,
  updateLocation,
} from '../../services/client'
import Modal from '../dialogs/Modal'
import { isValidRut, normalizeRut } from '../../utils/rut'

type Props = {
  open: boolean
  location?: Location | null
  onClose: () => void
  onSaved?: (location: Location) => void
  existingCodes: string[]
}

type FormState = {
  code: string
  name: string
  type: LocationType
  businessName: string
  rut: string
  description: string
  status: LocationStatus
}

type FormErrors = Partial<Record<keyof FormState | 'form', string>>

const EMPTY_FORM: FormState = {
  code: '',
  name: '',
  type: 'BODEGA',
  businessName: '',
  rut: '',
  description: '',
  status: 'ACTIVE',
}

const TYPE_OPTIONS: { value: LocationType; label: string }[] = [
  { value: 'BODEGA', label: 'Bodega' },
  { value: 'CONTAINER', label: 'Container' },
  { value: 'LOCAL', label: 'Local' },
  { value: 'CAMION', label: 'Camión' },
  { value: 'CAMIONETA', label: 'Camioneta' },
]

const STATUS_OPTIONS: { value: LocationStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Activa' },
  { value: 'BLOCKED', label: 'Bloqueada' },
]

export default function LocationFormDialog({
  open,
  location,
  onClose,
  onSaved,
  existingCodes,
}: Props) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [errors, setErrors] = useState<FormErrors>({})

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM)
      setErrors({})
      mutation.reset()
      return
    }
    if (location) {
      setForm({
        code: location.code ?? '',
        name: location.name ?? '',
        type: location.type,
        businessName: location.businessName ?? '',
        rut: location.rut ?? '',
        description: location.description ?? '',
        status: location.status ?? 'ACTIVE',
      })
    } else {
      setForm(EMPTY_FORM)
    }
    setErrors({})
    mutation.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, location])

  const mutation = useMutation({
    mutationFn: (payload: LocationPayload) => {
      if (location) {
        return updateLocation(location.id, payload)
      }
      return createLocation(payload)
    },
    onSuccess: saved => {
      onSaved?.(saved)
      onClose()
    },
    onError: (error: unknown) => {
      setErrors(prev => ({
        ...prev,
        form: error instanceof Error ? error.message : 'No se pudo guardar la ubicación',
      }))
    },
  })

  const normalizedExistingCodes = useMemo(
    () => existingCodes.map(code => code.trim().toLowerCase()).filter(Boolean),
    [existingCodes]
  )

  const setField = <K extends keyof FormState>(field: K, value: FormState[K]) => {
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
    const nextErrors: FormErrors = {}
    const code = form.code.trim()
    const name = form.name.trim()
    const rut = form.rut.trim()

    if (!code) {
      nextErrors.code = 'El código es obligatorio'
    } else if (normalizedExistingCodes.includes(code.toLowerCase())) {
      nextErrors.code = 'El código ya existe'
    }

    if (!name) {
      nextErrors.name = 'El nombre es obligatorio'
    }

    if (rut && !isValidRut(rut)) {
      nextErrors.rut = 'Ingresa un RUT válido'
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors)
      return
    }

    const payload: LocationPayload = {
      code,
      name,
      type: form.type,
      businessName: form.businessName.trim() || undefined,
      rut: rut ? normalizeRut(rut) : undefined,
      description: form.description.trim() || undefined,
      status: form.status,
    }

    mutation.mutate(payload)
  }

  const title = location ? 'Editar ubicación' : 'Nueva ubicación'
  const isPending = mutation.isPending

  return (
    <Modal open={open} onClose={() => !isPending && onClose()} title={title}>
      <form className="form-grid" onSubmit={handleSubmit} noValidate>
        {location ? (
          <label>
            <span>ID</span>
            <input className="input" value={location.id} disabled />
          </label>
        ) : null}
        <label>
          <span>Código *</span>
          <input
            className={`input${errors.code ? ' input-error' : ''}`}
            value={form.code}
            onChange={event => setField('code', event.target.value.toUpperCase())}
            placeholder="BOD-001"
            disabled={isPending}
            required
            data-autofocus
          />
          {errors.code ? <p className="error">{errors.code}</p> : null}
        </label>
        <label>
          <span>Nombre *</span>
          <input
            className={`input${errors.name ? ' input-error' : ''}`}
            value={form.name}
            onChange={event => setField('name', event.target.value)}
            placeholder="Bodega principal"
            disabled={isPending}
            required
          />
          {errors.name ? <p className="error">{errors.name}</p> : null}
        </label>
        <label>
          <span>Tipo *</span>
          <select
            className="input"
            value={form.type}
            onChange={event => setField('type', event.target.value as LocationType)}
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
          <span>Razón social</span>
          <input
            className="input"
            value={form.businessName}
            onChange={event => setField('businessName', event.target.value)}
            placeholder="Comercial Demo SpA"
            disabled={isPending}
          />
        </label>
        <label>
          <span>RUT</span>
          <input
            className={`input${errors.rut ? ' input-error' : ''}`}
            value={form.rut}
            onChange={event => setField('rut', event.target.value)}
            placeholder="76.123.456-0"
            disabled={isPending}
            maxLength={20}
          />
          {errors.rut ? <p className="error">{errors.rut}</p> : null}
        </label>
        <label>
          <span>Estado *</span>
          <select
            className="input"
            value={form.status}
            onChange={event => setField('status', event.target.value as LocationStatus)}
            disabled={isPending}
            required
          >
            {STATUS_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          <span>Descripción</span>
          <textarea
            className="input"
            rows={3}
            value={form.description}
            onChange={event => setField('description', event.target.value)}
            placeholder="Notas adicionales, horario, restricciones..."
            disabled={isPending}
          />
        </label>
        {errors.form ? (
          <p className="error" style={{ gridColumn: '1 / -1' }}>
            {errors.form}
          </p>
        ) : null}
        {mutation.isError && !errors.form ? (
          <p className="error" style={{ gridColumn: '1 / -1' }}>
            {(mutation.error as Error)?.message ?? 'No se pudo guardar la ubicación'}
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
