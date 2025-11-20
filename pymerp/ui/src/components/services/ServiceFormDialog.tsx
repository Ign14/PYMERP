import { FormEvent, useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  ServiceDTO,
  ServicePayload,
  ServiceStatus,
  createService,
  updateService,
} from '../../services/client'
import Modal from '../dialogs/Modal'

type Props = {
  open: boolean
  service?: ServiceDTO | null
  onClose: () => void
  onSaved?: (service: ServiceDTO) => void
  existingCodes: string[]
}

type FormState = {
  code: string
  name: string
  category: string
  unitPrice: string
  status: ServiceStatus
  description: string
}

type FormErrors = Partial<Record<keyof FormState | 'form', string>>

const EMPTY_FORM: FormState = {
  code: '',
  name: '',
  category: '',
  unitPrice: '',
  status: 'ACTIVE',
  description: '',
}

const STATUS_OPTIONS: { value: ServiceStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'Inactivo' },
]

export default function ServiceFormDialog({
  open,
  service,
  onClose,
  onSaved,
  existingCodes,
}: Props) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [errors, setErrors] = useState<FormErrors>({})

  const mutation = useMutation({
    mutationFn: (payload: ServicePayload) => {
      if (service) {
        return updateService(service.id, payload)
      }
      return createService(payload)
    },
    onSuccess: saved => {
      onSaved?.(saved)
      onClose()
    },
    onError: (error: unknown) => {
      setErrors(prev => ({
        ...prev,
        form: error instanceof Error ? error.message : 'No se pudo guardar el servicio',
      }))
    },
  })

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM)
      setErrors({})
      mutation.reset()
      return
    }
    if (service) {
      setForm({
        code: service.code ?? '',
        name: service.name ?? '',
        category: service.category ?? '',
        unitPrice: service.unitPrice?.toString() ?? '',
        status: service.status ?? 'ACTIVE',
        description: service.description ?? '',
      })
    } else {
      setForm(EMPTY_FORM)
    }
    setErrors({})
    mutation.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, service])

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
    const category = form.category.trim()
    const priceValue = Number(form.unitPrice)

    if (!code) {
      nextErrors.code = 'El código es obligatorio'
    } else if (!service && existingCodes.includes(code.toLowerCase())) {
      nextErrors.code = 'El código ya existe'
    } else if (service && code.toLowerCase() !== service.code.toLowerCase() && existingCodes.includes(code.toLowerCase())) {
      nextErrors.code = 'El código ya existe'
    }

    if (!name) {
      nextErrors.name = 'El nombre es obligatorio'
    }

    if (!Number.isFinite(priceValue) || priceValue <= 0) {
      nextErrors.unitPrice = 'El precio debe ser mayor a 0'
    }

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors)
      return
    }

    const payload: ServicePayload = {
      code,
      name,
      category: category || undefined,
      unitPrice: Number(priceValue.toFixed(2)),
      status: form.status,
      description: form.description.trim() || undefined,
    }

    mutation.mutate(payload)
  }

  const title = service ? 'Editar servicio' : 'Nuevo servicio'
  const isPending = mutation.isPending

  return (
    <Modal open={open} onClose={() => !isPending && onClose()} title={title}>
      <form className="form-grid" onSubmit={handleSubmit} noValidate>
        {service ? (
          <label>
            <span>ID</span>
            <input className="input" value={service.id} disabled />
          </label>
        ) : null}
        <label>
          <span>Código *</span>
          <input
            className={`input${errors.code ? ' input-error' : ''}`}
            value={form.code}
            onChange={event => setField('code', event.target.value.toUpperCase())}
            placeholder="SRV-001"
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
            placeholder="Servicio de transporte"
            disabled={isPending}
            required
          />
          {errors.name ? <p className="error">{errors.name}</p> : null}
        </label>
        <label>
          <span>Categoría</span>
          <input
            className="input"
            value={form.category}
            onChange={event => setField('category', event.target.value)}
            placeholder="Consultoría, Transporte, etc."
            disabled={isPending}
          />
        </label>
        <label>
          <span>Precio unitario *</span>
          <input
            className={`input${errors.unitPrice ? ' input-error' : ''}`}
            type="number"
            min="0"
            step="0.01"
            value={form.unitPrice}
            onChange={event => setField('unitPrice', event.target.value)}
            placeholder="50000"
            disabled={isPending}
            required
          />
          {errors.unitPrice ? <p className="error">{errors.unitPrice}</p> : null}
        </label>
        <label>
          <span>Estado *</span>
          <select
            className="input"
            value={form.status}
            onChange={event => setField('status', event.target.value as ServiceStatus)}
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
            placeholder="Detalles del servicio o condiciones comerciales"
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
            {(mutation.error as Error)?.message ?? 'No se pudo guardar el servicio'}
          </p>
        ) : null}
        <div className="modal-actions" style={{ gridColumn: '1 / -1' }}>
          <button className="btn ghost" type="button" onClick={onClose} disabled={isPending}>
            Cancelar
          </button>
          <button className="btn" type="submit" disabled={isPending}>
            {isPending ? 'Guardando…' : service ? 'Guardar' : 'Crear'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
