import { FormEvent, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCompany, type Company, type CreateCompanyPayload } from '../services/client'
import { isValidRut, normalizeRut } from '../utils/rut'
import { parseProblemDetail } from '../utils/problemDetail'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type Props = {
  onCreated?: () => void
}

export type CompanyFormValues = {
  businessName: string
  rut: string
  businessActivity: string
  address: string
  commune: string
  phone: string
  email: string
  receiptFooterMessage: string
}

export type CompanyFormErrors = Partial<Record<keyof CompanyFormValues, string>>

export function emptyCompanyFormValues(): CompanyFormValues {
  return {
    businessName: '',
    rut: '',
    businessActivity: '',
    address: '',
    commune: '',
    phone: '',
    email: '',
    receiptFooterMessage: '',
  }
}

export function valuesFromCompany(company: Company): CompanyFormValues {
  return {
    businessName: company.businessName ?? '',
    rut: company.rut ?? '',
    businessActivity: company.businessActivity ?? '',
    address: company.address ?? '',
    commune: company.commune ?? '',
    phone: company.phone ?? '',
    email: company.email ?? '',
    receiptFooterMessage: company.receiptFooterMessage ?? '',
  }
}

export function validateCompanyForm(values: CompanyFormValues): {
  payload?: CreateCompanyPayload
  errors: CompanyFormErrors
} {
  const errors: CompanyFormErrors = {}

  const businessName = values.businessName.trim()
  if (!businessName) {
    errors.businessName = 'La razón social es obligatoria'
  }

  const rutInput = values.rut.trim()
  let normalizedRut: string | undefined
  if (!rutInput) {
    errors.rut = 'El RUT es obligatorio'
  } else if (!isValidRut(rutInput)) {
    errors.rut = 'Ingresa un RUT válido'
  } else {
    normalizedRut = normalizeRut(rutInput)
  }

  const emailInput = values.email.trim()
  if (emailInput && !EMAIL_REGEX.test(emailInput)) {
    errors.email = 'Ingresa un email válido'
  }

  if (Object.keys(errors).length > 0) {
    return { errors }
  }

  const toOptional = (value: string) => {
    const trimmed = value.trim()
    return trimmed.length > 0 ? trimmed : undefined
  }

  const payload: CreateCompanyPayload = {
    businessName,
    rut: normalizedRut ?? values.rut,
    businessActivity: toOptional(values.businessActivity),
    address: toOptional(values.address),
    commune: toOptional(values.commune),
    phone: toOptional(values.phone),
    email: emailInput ? emailInput.toLowerCase() : undefined,
    receiptFooterMessage: toOptional(values.receiptFooterMessage),
  }

  return { payload, errors }
}

export default function CreateCompanyForm({ onCreated }: Props) {
  const [values, setValues] = useState<CompanyFormValues>(emptyCompanyFormValues())
  const [fieldErrors, setFieldErrors] = useState<CompanyFormErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: createCompany,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'], exact: false })
      setValues(emptyCompanyFormValues())
      setFieldErrors({})
      setGlobalError(null)
      setSuccessMessage('Compañía creada correctamente')
      onCreated?.()
    },
    onError: (error: unknown) => {
      const parsed = parseProblemDetail(error)
      setFieldErrors(prev => ({ ...prev, ...parsed.fieldErrors }))
      setGlobalError(parsed.message ?? 'No se pudo crear la compañía')
      setSuccessMessage(null)
    },
  })

  const clearFieldError = (field: keyof CompanyFormValues) => {
    setFieldErrors(prev => {
      if (!prev[field]) {
        return prev
      }
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setGlobalError(null)
    setSuccessMessage(null)

    const { payload, errors } = validateCompanyForm(values)
    if (!payload || Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    mutation.mutate(payload)
  }

  const handleReset = () => {
    setValues(emptyCompanyFormValues())
    setFieldErrors({})
    setGlobalError(null)
    setSuccessMessage(null)
  }

  return (
    <div className="card">
      <h2>Crear compañía</h2>
      <form onSubmit={handleSubmit} className="form-grid" noValidate>
        <label>
          <span>Razón social *</span>
          <input
            value={values.businessName}
            onChange={event => {
              setValues(prev => ({ ...prev, businessName: event.target.value }))
              clearFieldError('businessName')
              setSuccessMessage(null)
            }}
            placeholder="Ej. Comercial Demo SpA"
            className="input"
            aria-invalid={fieldErrors.businessName ? 'true' : undefined}
            aria-describedby={fieldErrors.businessName ? 'company-businessName-error' : undefined}
            disabled={mutation.isPending}
          />
          {fieldErrors.businessName ? (
            <p id="company-businessName-error" className="error" aria-live="polite">
              {fieldErrors.businessName}
            </p>
          ) : null}
        </label>

        <label>
          <span>RUT *</span>
          <input
            value={values.rut}
            onChange={event => {
              setValues(prev => ({ ...prev, rut: event.target.value }))
              clearFieldError('rut')
              setSuccessMessage(null)
            }}
            placeholder="76.123.456-0"
            className="input"
            aria-invalid={fieldErrors.rut ? 'true' : undefined}
            aria-describedby={fieldErrors.rut ? 'company-rut-error' : undefined}
            disabled={mutation.isPending}
          />
          {fieldErrors.rut ? (
            <p id="company-rut-error" className="error" aria-live="polite">
              {fieldErrors.rut}
            </p>
          ) : null}
        </label>

        <label>
          <span>Giro / actividad</span>
          <input
            value={values.businessActivity}
            onChange={event => {
              setValues(prev => ({ ...prev, businessActivity: event.target.value }))
              clearFieldError('businessActivity')
              setSuccessMessage(null)
            }}
            placeholder="Ej. Servicios de tecnología"
            className="input"
            disabled={mutation.isPending}
          />
        </label>

        <label>
          <span>Dirección</span>
          <input
            value={values.address}
            onChange={event => {
              setValues(prev => ({ ...prev, address: event.target.value }))
              clearFieldError('address')
              setSuccessMessage(null)
            }}
            placeholder="Av. Principal 1234"
            className="input"
            disabled={mutation.isPending}
          />
        </label>

        <label>
          <span>Comuna</span>
          <input
            value={values.commune}
            onChange={event => {
              setValues(prev => ({ ...prev, commune: event.target.value }))
              clearFieldError('commune')
              setSuccessMessage(null)
            }}
            placeholder="Providencia"
            className="input"
            disabled={mutation.isPending}
          />
        </label>

        <label>
          <span>Teléfono</span>
          <input
            value={values.phone}
            onChange={event => {
              setValues(prev => ({ ...prev, phone: event.target.value }))
              clearFieldError('phone')
              setSuccessMessage(null)
            }}
            placeholder="+56 9 1234 5678"
            className="input"
            disabled={mutation.isPending}
          />
        </label>

        <label>
          <span>Email</span>
          <input
            value={values.email}
            onChange={event => {
              setValues(prev => ({ ...prev, email: event.target.value }))
              clearFieldError('email')
              setSuccessMessage(null)
            }}
            placeholder="contacto@empresa.cl"
            className="input"
            aria-invalid={fieldErrors.email ? 'true' : undefined}
            aria-describedby={fieldErrors.email ? 'company-email-error' : undefined}
            disabled={mutation.isPending}
          />
          {fieldErrors.email ? (
            <p id="company-email-error" className="error" aria-live="polite">
              {fieldErrors.email}
            </p>
          ) : null}
        </label>

        <label className="full">
          <span>Mensaje personalizado para boletas</span>
          <textarea
            value={values.receiptFooterMessage}
            onChange={event => {
              setValues(prev => ({ ...prev, receiptFooterMessage: event.target.value }))
              clearFieldError('receiptFooterMessage')
              setSuccessMessage(null)
            }}
            placeholder="Ej. ¡Gracias por preferirnos!"
            className="input"
            rows={3}
            disabled={mutation.isPending}
          />
        </label>

        <div className="buttons">
          <button className="btn" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? 'Guardando...' : 'Crear'}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={handleReset}
            disabled={mutation.isPending}
          >
            Limpiar
          </button>
        </div>

        {globalError ? (
          <p className="error" aria-live="assertive">
            {globalError}
          </p>
        ) : null}

        {successMessage ? (
          <p className="success" aria-live="polite">
            {successMessage}
          </p>
        ) : null}
      </form>
    </div>
  )
}
