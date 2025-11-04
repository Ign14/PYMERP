import { FormEvent, useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Supplier, SupplierPayload, createSupplier, updateSupplier } from '../../services/client'
import { parseProblemDetail } from '../../utils/problemDetail'
import Modal from './Modal'

type Props = {
  open: boolean
  supplier?: Supplier | null
  onClose: () => void
  onSaved?: (supplier: Supplier) => void
}

type FormState = {
  name: string
  rut: string
  address: string
  commune: string
  businessActivity: string
  phone: string
  email: string
}

type FormErrors = Partial<Record<keyof FormState, string>>

const EMPTY_FORM: FormState = {
  name: '',
  rut: '',
  address: '',
  commune: '',
  businessActivity: '',
  phone: '',
  email: '',
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function SupplierFormDialog({ open, supplier, onClose, onSaved }: Props) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<FormErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (payload: SupplierPayload) => {
      if (supplier) {
        return updateSupplier(supplier.id, payload)
      }
      return createSupplier(payload)
    },
    onSuccess: saved => {
      setFieldErrors({})
      setGlobalError(null)
      onSaved?.(saved)
      onClose()
    },
    onError: (error: unknown) => {
      const parsed = parseProblemDetail(error)
      const nextErrors: FormErrors = {}
      for (const [field, message] of Object.entries(parsed.fieldErrors)) {
        if (field in EMPTY_FORM && typeof message === 'string' && message.trim().length > 0) {
          nextErrors[field as keyof FormState] = message
        }
      }
      setFieldErrors(nextErrors)
      setGlobalError(parsed.message ?? 'No se pudo guardar el proveedor')
    },
  })

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM)
      setFieldErrors({})
      setGlobalError(null)
      mutation.reset()
      return
    }

    if (supplier) {
      setForm({
        name: supplier.name ?? '',
        rut: supplier.rut ?? '',
        address: supplier.address ?? '',
        commune: supplier.commune ?? '',
        businessActivity: supplier.businessActivity ?? '',
        phone: supplier.phone ?? '',
        email: supplier.email ?? '',
      })
    } else {
      setForm(EMPTY_FORM)
    }

    setFieldErrors({})
    setGlobalError(null)
    mutation.reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, supplier])

  const clearFieldError = (field: keyof FormState) => {
    setFieldErrors(prev => {
      if (!prev[field]) {
        return prev
      }
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setGlobalError(null)

    const name = form.name.trim()
    const rut = form.rut.trim()
    const email = form.email.trim()

    const errors: FormErrors = {}
    if (!name) {
      errors.name = 'El nombre es obligatorio'
    }
    if (!rut) {
      errors.rut = 'El RUT es obligatorio'
    }
    if (email && !EMAIL_REGEX.test(email)) {
      errors.email = 'Ingresa un email válido'
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    setFieldErrors({})

    const toOptional = (value: string) => {
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : null
    }

    const payload: SupplierPayload = {
      name,
      rut,
      address: toOptional(form.address),
      commune: toOptional(form.commune),
      businessActivity: toOptional(form.businessActivity),
      phone: toOptional(form.phone),
      email: toOptional(form.email),
    }

    mutation.mutate(payload)
  }

  const title = supplier ? 'Editar proveedor' : 'Nuevo proveedor'
  const isPending = mutation.isPending

  return (
    <Modal open={open} title={title} onClose={() => !isPending && onClose()}>
      <form className="form-grid" onSubmit={onSubmit} noValidate>
        <label>
          <span>Nombre *</span>
          <input
            className={`input${fieldErrors.name ? ' input-error' : ''}`}
            value={form.name}
            onChange={event => {
              setForm(prev => ({ ...prev, name: event.target.value }))
              clearFieldError('name')
              setGlobalError(null)
            }}
            placeholder="Proveedor demo"
            disabled={isPending}
            maxLength={100}
            required
            data-autofocus
            aria-invalid={fieldErrors.name ? 'true' : undefined}
            aria-describedby={fieldErrors.name ? 'supplier-form-name-error' : undefined}
          />
          {fieldErrors.name ? (
            <p id="supplier-form-name-error" className="error" aria-live="polite">
              {fieldErrors.name}
            </p>
          ) : null}
        </label>
        <label>
          <span>RUT *</span>
          <input
            className={`input${fieldErrors.rut ? ' input-error' : ''}`}
            value={form.rut}
            onChange={event => {
              setForm(prev => ({ ...prev, rut: event.target.value }))
              clearFieldError('rut')
              setGlobalError(null)
            }}
            placeholder="76.123.456-k"
            disabled={isPending}
            maxLength={20}
            required
            aria-invalid={fieldErrors.rut ? 'true' : undefined}
            aria-describedby={fieldErrors.rut ? 'supplier-form-rut-error' : undefined}
          />
          {fieldErrors.rut ? (
            <p id="supplier-form-rut-error" className="error" aria-live="polite">
              {fieldErrors.rut}
            </p>
          ) : null}
        </label>
        <label>
          <span>Dirección</span>
          <input
            className="input"
            value={form.address}
            onChange={event => {
              setForm(prev => ({ ...prev, address: event.target.value }))
              clearFieldError('address')
              setGlobalError(null)
            }}
            placeholder="Av. Principal 1234"
            disabled={isPending}
            maxLength={200}
          />
          {fieldErrors.address ? (
            <p className="error" aria-live="polite">
              {fieldErrors.address}
            </p>
          ) : null}
        </label>
        <label>
          <span>Comuna</span>
          <input
            className="input"
            value={form.commune}
            onChange={event => {
              setForm(prev => ({ ...prev, commune: event.target.value }))
              clearFieldError('commune')
              setGlobalError(null)
            }}
            placeholder="Providencia"
            disabled={isPending}
            maxLength={120}
          />
          {fieldErrors.commune ? (
            <p className="error" aria-live="polite">
              {fieldErrors.commune}
            </p>
          ) : null}
        </label>
        <label>
          <span>Giro / actividad</span>
          <input
            className="input"
            value={form.businessActivity}
            onChange={event => {
              setForm(prev => ({ ...prev, businessActivity: event.target.value }))
              clearFieldError('businessActivity')
              setGlobalError(null)
            }}
            placeholder="Distribución"
            disabled={isPending}
            maxLength={120}
          />
          {fieldErrors.businessActivity ? (
            <p className="error" aria-live="polite">
              {fieldErrors.businessActivity}
            </p>
          ) : null}
        </label>
        <label>
          <span>Teléfono</span>
          <input
            className="input"
            value={form.phone}
            onChange={event => {
              setForm(prev => ({ ...prev, phone: event.target.value }))
              clearFieldError('phone')
              setGlobalError(null)
            }}
            placeholder="+56 9 1234 5678"
            disabled={isPending}
            maxLength={30}
          />
          {fieldErrors.phone ? (
            <p className="error" aria-live="polite">
              {fieldErrors.phone}
            </p>
          ) : null}
        </label>
        <label>
          <span>Email</span>
          <input
            className={`input${fieldErrors.email ? ' input-error' : ''}`}
            value={form.email}
            onChange={event => {
              setForm(prev => ({ ...prev, email: event.target.value }))
              clearFieldError('email')
              setGlobalError(null)
            }}
            placeholder="contacto@empresa.cl"
            type="email"
            disabled={isPending}
            maxLength={120}
            aria-invalid={fieldErrors.email ? 'true' : undefined}
            aria-describedby={fieldErrors.email ? 'supplier-form-email-error' : undefined}
          />
          {fieldErrors.email ? (
            <p id="supplier-form-email-error" className="error" aria-live="polite">
              {fieldErrors.email}
            </p>
          ) : null}
        </label>
        <div className="buttons">
          <button className="btn" type="submit" disabled={isPending}>
            {isPending ? 'Guardando...' : 'Guardar'}
          </button>
          <button className="btn ghost" type="button" onClick={onClose} disabled={isPending}>
            Cancelar
          </button>
        </div>
        {globalError ? (
          <p className="error" aria-live="polite">
            {globalError}
          </p>
        ) : null}
      </form>
    </Modal>
  )
}
