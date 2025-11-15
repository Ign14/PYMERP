import { FormEvent, useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCustomer, updateCustomer, Customer, CustomerPayload } from '../../services/client'
import Modal from './Modal'
import { parseProblemDetail } from '../../utils/problemDetail'
import { EMAIL_REGEX, normalizeEmail } from '../../utils/validation'
import { isValidRut, normalizeRut } from '../../utils/rut'

type Props = {
  open: boolean
  onClose: () => void
  editingCustomer?: Customer | null
}

type FormState = Omit<CustomerPayload, 'lat' | 'lng'> & {
  latText: string
  lngText: string
}

type FieldErrors = Partial<
  Record<
    | 'name'
    | 'rut'
    | 'email'
    | 'phone'
    | 'address'
    | 'segment'
    | 'contactPerson'
    | 'notes'
    | 'latText'
    | 'lngText',
    string
  >
>

const createEmptyForm = (): FormState => ({
  name: '',
  rut: '',
  phone: '',
  email: '',
  address: '',
  segment: '',
  contactPerson: '',
  notes: '',
  active: true,
  latText: '',
  lngText: '',
})

const buildFormState = (customer?: Customer | null): FormState =>
  customer
    ? {
        name: customer.name,
        rut: customer.rut || '',
        phone: customer.phone || '',
        email: customer.email || '',
        address: customer.address || '',
        segment: customer.segment || '',
        contactPerson: customer.contactPerson || '',
        notes: customer.notes || '',
        active: customer.active !== false,
        latText: customer.lat != null ? String(customer.lat) : '',
        lngText: customer.lng != null ? String(customer.lng) : '',
      }
    : createEmptyForm()

const toOptional = (value?: string | null) => {
  if (value == null) return undefined
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

const mapBackendFieldToFormField = (field: string): keyof FieldErrors | null => {
  switch (field) {
    case 'name':
      return 'name'
    case 'rut':
    case 'document':
      return 'rut'
    case 'email':
      return 'email'
    case 'phone':
      return 'phone'
    case 'address':
      return 'address'
    case 'segment':
      return 'segment'
    case 'contactPerson':
      return 'contactPerson'
    case 'notes':
      return 'notes'
    case 'lat':
      return 'latText'
    case 'lng':
      return 'lngText'
    default:
      return null
  }
}

export default function CustomerCreateDialog({ open, onClose, editingCustomer }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<FormState>(() => buildFormState(editingCustomer))
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)

  const clearFieldError = (field: keyof FieldErrors) => {
    setFieldErrors(prev => {
      if (!prev[field]) {
        return prev
      }
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  useEffect(() => {
    if (!open) {
      return
    }
    setForm(buildFormState(editingCustomer))
    setFieldErrors({})
    setGlobalError(null)
  }, [open, editingCustomer])

  const validateForm = (currentForm: FormState): { payload?: CustomerPayload; errors: FieldErrors } => {
    const errors: FieldErrors = {}
    const nameInput = currentForm.name.trim()
    if (nameInput.length < 3) {
      errors.name = 'El nombre debe tener al menos 3 caracteres'
    }

    const emailInput = currentForm.email.trim()
    if (!emailInput) {
      errors.email = 'El email es obligatorio'
    } else if (!EMAIL_REGEX.test(emailInput.toLowerCase())) {
      errors.email = 'Ingresa un email válido'
    }
    const normalizedEmail = emailInput ? normalizeEmail(emailInput) : ''

    const rutInput = currentForm.rut.trim()
    let normalizedRut: string | undefined
    if (rutInput.length > 0) {
      if (!isValidRut(rutInput)) {
        errors.rut = 'Ingresa un RUT válido'
      } else {
        normalizedRut = normalizeRut(rutInput)
      }
    }

    const parseCoordinate = (value: string, field: 'latText' | 'lngText'): number | null => {
      if (!value || !value.trim()) {
        return null
      }
      const parsed = Number(value.trim())
      if (!Number.isFinite(parsed)) {
        errors[field] = 'Ingresa una coordenada válida'
        return null
      }
      return parsed
    }

    const lat = parseCoordinate(currentForm.latText, 'latText')
    const lng = parseCoordinate(currentForm.lngText, 'lngText')

    if (Object.keys(errors).length > 0) {
      return { errors }
    }

    const phoneValue = currentForm.phone ? currentForm.phone.replace(/\s+/g, ' ').trim() : ''

    const payload: CustomerPayload = {
      name: nameInput,
      rut: normalizedRut ?? toOptional(currentForm.rut),
      document: normalizedRut ?? toOptional(currentForm.rut),
      phone: phoneValue || undefined,
      email: normalizedEmail,
      address: toOptional(currentForm.address),
      segment: toOptional(currentForm.segment),
      contactPerson: toOptional(currentForm.contactPerson),
      notes: toOptional(currentForm.notes),
      active: currentForm.active,
      lat,
      lng,
    }

    return { payload, errors }
  }

  const handleMutationError = (error: unknown) => {
    const parsed = parseProblemDetail(error)
    const nextErrors: FieldErrors = {}
    for (const [field, message] of Object.entries(parsed.fieldErrors)) {
      const mapped = mapBackendFieldToFormField(field)
      if (message && mapped) {
        nextErrors[mapped] = message
      }
    }
    setFieldErrors(nextErrors)
    setGlobalError(parsed.message ?? 'No se pudo guardar el cliente')
  }

  const createMutation = useMutation({
    mutationFn: (payload: CustomerPayload) => createCustomer(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'], exact: false })
      setGlobalError(null)
      setFieldErrors({})
      handleClose()
    },
    onError: handleMutationError,
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CustomerPayload }) =>
      updateCustomer(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'], exact: false })
      setGlobalError(null)
      setFieldErrors({})
      handleClose()
    },
    onError: handleMutationError,
  })

  const handleClose = () => {
    if (createMutation.isPending || updateMutation.isPending) {
      return
    }
    setForm(createEmptyForm())
    setFieldErrors({})
    setGlobalError(null)
    onClose()
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    setFieldErrors({})
    setGlobalError(null)
    const { payload, errors } = validateForm(form)
    if (!payload || Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    if (editingCustomer) {
      updateMutation.mutate({ id: editingCustomer.id, payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title={editingCustomer ? 'Editar Cliente' : 'Nuevo Cliente'}
    >
      <form className="form-grid" onSubmit={onSubmit} noValidate>
        <label>
          <span>Nombre *</span>
          <input
            className="input"
            autoFocus
            value={form.name}
            onChange={e => {
              setForm(prev => ({ ...prev, name: e.target.value }))
              clearFieldError('name')
              setGlobalError(null)
            }}
            placeholder="Cliente demo"
            aria-invalid={fieldErrors.name ? 'true' : undefined}
            aria-describedby={fieldErrors.name ? 'customer-name-error' : undefined}
            required
            disabled={isPending}
          />
          {fieldErrors.name ? (
            <span id="customer-name-error" className="error">
              {fieldErrors.name}
            </span>
          ) : null}
        </label>
        <label>
          <span>RUT</span>
          <input
            className="input"
            value={form.rut ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, rut: e.target.value }))
              clearFieldError('rut')
              setGlobalError(null)
            }}
            placeholder="12.345.678-9"
            aria-invalid={fieldErrors.rut ? 'true' : undefined}
            aria-describedby={fieldErrors.rut ? 'customer-rut-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.rut ? (
            <span id="customer-rut-error" className="error">
              {fieldErrors.rut}
            </span>
          ) : null}
        </label>
        <label>
          <span>Email *</span>
          <input
            className="input"
            type="email"
            value={form.email ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, email: e.target.value }))
              clearFieldError('email')
              setGlobalError(null)
            }}
            placeholder="cliente@correo.com"
            aria-invalid={fieldErrors.email ? 'true' : undefined}
            aria-describedby={fieldErrors.email ? 'customer-email-error' : undefined}
            required
            disabled={isPending}
          />
          {fieldErrors.email ? (
            <span id="customer-email-error" className="error">
              {fieldErrors.email}
            </span>
          ) : null}
        </label>
        <label>
          <span>Teléfono</span>
          <input
            className="input"
            value={form.phone ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, phone: e.target.value }))
              clearFieldError('phone')
              setGlobalError(null)
            }}
            placeholder="+56 9 0000 0000"
            aria-invalid={fieldErrors.phone ? 'true' : undefined}
            aria-describedby={fieldErrors.phone ? 'customer-phone-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.phone ? (
            <span id="customer-phone-error" className="error">
              {fieldErrors.phone}
            </span>
          ) : null}
        </label>
        <label>
          <span>Persona de Contacto</span>
          <input
            className="input"
            value={form.contactPerson ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, contactPerson: e.target.value }))
              clearFieldError('contactPerson')
              setGlobalError(null)
            }}
            placeholder="Juan Pérez"
            aria-invalid={fieldErrors.contactPerson ? 'true' : undefined}
            aria-describedby={fieldErrors.contactPerson ? 'customer-contact-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.contactPerson ? (
            <span id="customer-contact-error" className="error">
              {fieldErrors.contactPerson}
            </span>
          ) : null}
        </label>
        <label>
          <span>Segmento</span>
          <input
            className="input"
            value={form.segment ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, segment: e.target.value }))
              clearFieldError('segment')
              setGlobalError(null)
            }}
            placeholder="Retail"
            aria-invalid={fieldErrors.segment ? 'true' : undefined}
            aria-describedby={fieldErrors.segment ? 'customer-segment-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.segment ? (
            <span id="customer-segment-error" className="error">
              {fieldErrors.segment}
            </span>
          ) : null}
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          <span>Dirección</span>
          <input
            className="input"
            value={form.address ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, address: e.target.value }))
              clearFieldError('address')
              setGlobalError(null)
            }}
            placeholder="Av. Demo 123"
            aria-invalid={fieldErrors.address ? 'true' : undefined}
            aria-describedby={fieldErrors.address ? 'customer-address-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.address ? (
            <span id="customer-address-error" className="error">
              {fieldErrors.address}
            </span>
          ) : null}
        </label>
        <label>
          <span>Latitud</span>
          <input
            className="input"
            type="number"
            step="any"
            value={form.latText}
            onChange={e => {
              setForm(prev => ({ ...prev, latText: e.target.value }))
              clearFieldError('latText')
              setGlobalError(null)
            }}
            placeholder="-33.4489"
            aria-invalid={fieldErrors.latText ? 'true' : undefined}
            aria-describedby={fieldErrors.latText ? 'customer-lat-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.latText ? (
            <span id="customer-lat-error" className="error">
              {fieldErrors.latText}
            </span>
          ) : null}
        </label>
        <label>
          <span>Longitud</span>
          <input
            className="input"
            type="number"
            step="any"
            value={form.lngText}
            onChange={e => {
              setForm(prev => ({ ...prev, lngText: e.target.value }))
              clearFieldError('lngText')
              setGlobalError(null)
            }}
            placeholder="-70.6693"
            aria-invalid={fieldErrors.lngText ? 'true' : undefined}
            aria-describedby={fieldErrors.lngText ? 'customer-lng-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.lngText ? (
            <span id="customer-lng-error" className="error">
              {fieldErrors.lngText}
            </span>
          ) : null}
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          <span>Notas</span>
          <textarea
            className="input"
            rows={3}
            value={form.notes ?? ''}
            onChange={e => {
              setForm(prev => ({ ...prev, notes: e.target.value }))
              clearFieldError('notes')
              setGlobalError(null)
            }}
            placeholder="Observaciones adicionales..."
            aria-invalid={fieldErrors.notes ? 'true' : undefined}
            aria-describedby={fieldErrors.notes ? 'customer-notes-error' : undefined}
            disabled={isPending}
          />
          {fieldErrors.notes ? (
            <span id="customer-notes-error" className="error">
              {fieldErrors.notes}
            </span>
          ) : null}
        </label>
        <label className="checkbox-label" style={{ gridColumn: '1 / -1' }}>
          <input
            type="checkbox"
            checked={form.active !== false}
            onChange={e => {
              setForm(prev => ({ ...prev, active: e.target.checked }))
              setGlobalError(null)
            }}
            disabled={isPending}
          />
          <span>Cliente activo</span>
        </label>
        {globalError ? (
          <p className="error" style={{ gridColumn: '1 / -1' }}>
            {globalError}
          </p>
        ) : null}
        <div className="buttons" style={{ gridColumn: '1 / -1' }}>
          <button
            className="btn"
            type="submit"
            disabled={isPending}
          >
            {editingCustomer
              ? updateMutation.isPending
                ? 'Actualizando...'
                : 'Actualizar'
              : createMutation.isPending
                ? 'Guardando...'
                : 'Crear'}
          </button>
          <button className="btn ghost" type="button" onClick={handleClose} disabled={isPending}>
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  )
}
