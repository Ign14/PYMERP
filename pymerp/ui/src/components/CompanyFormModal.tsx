import { FormEvent, useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'

import Modal from './dialogs/Modal'
import { updateCompany, type Company, type UpdateCompanyPayload } from '../services/client'
import {
  emptyCompanyFormValues,
  type CompanyFormErrors,
  type CompanyFormValues,
  validateCompanyForm,
  valuesFromCompany,
} from './CreateCompanyForm'
import { parseProblemDetail } from '../utils/problemDetail'

interface CompanyFormModalProps {
  company: Company | null
  open: boolean
  onClose: () => void
}

export default function CompanyFormModal({ company, open, onClose }: CompanyFormModalProps) {
  const [values, setValues] = useState<CompanyFormValues>(emptyCompanyFormValues())
  const [fieldErrors, setFieldErrors] = useState<CompanyFormErrors>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (company && open) {
      setValues(valuesFromCompany(company))
      setFieldErrors({})
      setGlobalError(null)
      setNotice(null)
    } else if (!open) {
      setValues(emptyCompanyFormValues())
      setFieldErrors({})
      setGlobalError(null)
      setNotice(null)
    }
  }, [company, open])

  const mutation = useMutation({
    mutationFn: (payload: UpdateCompanyPayload) => {
      if (!company) {
        throw new Error('No hay compañía seleccionada')
      }
      return updateCompany(company.id, payload)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'], exact: false })
      setFieldErrors({})
      setGlobalError(null)
      setNotice('Cambios guardados correctamente')
    },
    onError: (error: unknown) => {
      const parsed = parseProblemDetail(error)
      setFieldErrors(prev => ({ ...prev, ...parsed.fieldErrors }))
      setGlobalError(parsed.message ?? 'No se pudo actualizar la compañía')
      setNotice(null)
    },
  })

  if (!open || !company) {
    return null
  }

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
    setNotice(null)

    const { payload, errors } = validateCompanyForm(values)
    if (!payload || Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    mutation.mutate(payload)
  }

  return (
    <Modal open={open} title="Editar compañía" onClose={onClose}>
      <form onSubmit={handleSubmit} className="form-grid" noValidate>
        <label>
          <span>Razón social *</span>
          <input
            value={values.businessName}
            onChange={event => {
              setValues(prev => ({ ...prev, businessName: event.target.value }))
              clearFieldError('businessName')
              setNotice(null)
            }}
            className="input"
            placeholder="Ej. Comercial Demo SpA"
            aria-invalid={fieldErrors.businessName ? 'true' : undefined}
            aria-describedby={
              fieldErrors.businessName ? 'edit-company-businessName-error' : undefined
            }
            disabled={mutation.isPending}
          />
          {fieldErrors.businessName ? (
            <p id="edit-company-businessName-error" className="error" aria-live="polite">
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
              setNotice(null)
            }}
            className="input"
            placeholder="76.123.456-0"
            aria-invalid={fieldErrors.rut ? 'true' : undefined}
            aria-describedby={fieldErrors.rut ? 'edit-company-rut-error' : undefined}
            disabled={mutation.isPending}
          />
          {fieldErrors.rut ? (
            <p id="edit-company-rut-error" className="error" aria-live="polite">
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
              setNotice(null)
            }}
            className="input"
            placeholder="Ej. Servicios de tecnología"
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
              setNotice(null)
            }}
            className="input"
            placeholder="Av. Principal 1234"
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
              setNotice(null)
            }}
            className="input"
            placeholder="Providencia"
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
              setNotice(null)
            }}
            className="input"
            placeholder="+56 9 1234 5678"
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
              setNotice(null)
            }}
            className="input"
            placeholder="contacto@empresa.cl"
            aria-invalid={fieldErrors.email ? 'true' : undefined}
            aria-describedby={fieldErrors.email ? 'edit-company-email-error' : undefined}
            disabled={mutation.isPending}
          />
          {fieldErrors.email ? (
            <p id="edit-company-email-error" className="error" aria-live="polite">
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
              setNotice(null)
            }}
            className="input"
            rows={3}
            placeholder="Ej. ¡Gracias por preferirnos!"
            disabled={mutation.isPending}
          />
        </label>

        <div className="buttons">
          <button className="btn" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Guardando...' : 'Guardar cambios'}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
          >
            Cerrar
          </button>
        </div>

        {globalError ? (
          <p className="error" aria-live="assertive">
            {globalError}
          </p>
        ) : null}

        {notice ? (
          <p className="success" aria-live="polite">
            {notice}
          </p>
        ) : null}
      </form>
    </Modal>
  )
}
