import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchCompany,
  type Company,
  type UpdateCompanyPayload,
  updateCompany,
} from '../../services/client'
import {
  emptyCompanyFormValues,
  type CompanyFormErrors,
  type CompanyFormValues,
  validateCompanyForm,
  valuesFromCompany,
} from '../CreateCompanyForm'
import { parseProblemDetail } from '../../utils/problemDetail'

type AccountOverviewCardProps = {
  companyId: string
}

export default function AccountOverviewCard({ companyId }: AccountOverviewCardProps) {
  const queryClient = useQueryClient()
  const [isEditing, setIsEditing] = useState(false)
  const [values, setValues] = useState<CompanyFormValues>(emptyCompanyFormValues())
  const [fieldErrors, setFieldErrors] = useState<CompanyFormErrors>({})
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const companyQuery = useQuery<Company, Error>({
    queryKey: ['company', companyId],
    queryFn: () => fetchCompany(companyId),
    enabled: !!companyId,
    refetchOnWindowFocus: false,
  })

  useEffect(() => {
    if (companyQuery.data && isEditing) {
      setValues(valuesFromCompany(companyQuery.data))
    }
  }, [companyQuery.data, isEditing])

  const mutation = useMutation({
    mutationFn: (payload: UpdateCompanyPayload) => updateCompany(companyId, payload),
    onSuccess: updated => {
      queryClient.invalidateQueries({ queryKey: ['company', companyId], exact: true })
      queryClient.invalidateQueries({ queryKey: ['companies'], exact: false })
      setNotice('Datos guardados correctamente')
      setErrorMessage(null)
      setFieldErrors({})
      setIsEditing(false)
      setValues(valuesFromCompany(updated))
    },
    onError: (error: unknown) => {
      const parsed = parseProblemDetail(error)
      setFieldErrors(prev => ({ ...prev, ...parsed.fieldErrors }))
      setErrorMessage(parsed.message ?? 'No se pudieron guardar los cambios')
      setNotice(null)
    },
  })

  const isLoading = companyQuery.isLoading
  const loadError = companyQuery.isError ? companyQuery.error : null
  const company = companyQuery.data

  useEffect(() => {
    if (company && !isEditing) {
      setValues(valuesFromCompany(company))
    }
  }, [company, isEditing])

  const companySummary = useMemo(() => {
    if (!company) {
      return null
    }
    const location = [company.address, company.commune].filter(Boolean).join(', ')
    return {
      businessName: company.businessName ?? '',
      rut: company.rut ?? '',
      activity: company.businessActivity ?? '',
      location,
      phone: company.phone ?? '',
      email: company.email ?? '',
      receiptFooterMessage: company.receiptFooterMessage ?? '',
      updatedAt: company.updatedAt,
    }
  }, [company])

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
    setErrorMessage(null)
    setNotice(null)

    const { payload, errors } = validateCompanyForm(values)
    if (!payload || Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    mutation.mutate(payload as UpdateCompanyPayload)
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Cuenta de la empresa</h2>
        <div className="card-actions">
          <button
            className="btn-link"
            onClick={() => companyQuery.refetch()}
            disabled={companyQuery.isFetching}
          >
            {companyQuery.isFetching ? 'Actualizando...' : 'Actualizar'}
          </button>
          <button
            className="btn-link"
            onClick={() => {
              if (company && !isEditing) {
                setValues(valuesFromCompany(company))
              }
              setFieldErrors({})
              setErrorMessage(null)
              setNotice(null)
              setIsEditing(prev => !prev)
            }}
            disabled={isLoading || mutation.isPending}
          >
            {isEditing ? 'Cancelar' : 'Editar'}
          </button>
        </div>
      </div>
      <div className="card-body">
        {isLoading && <p>Cargando configuracion de la empresa...</p>}
        {loadError && (
          <p className="error" aria-live="assertive">
            {loadError.message ?? 'No se pudo cargar la informacion de la empresa'}
          </p>
        )}
        {!isLoading && !loadError && companySummary && !isEditing && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <dl
              style={{
                display: 'grid',
                gap: '12px',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              }}
            >
              <div>
                <dt>Razon social</dt>
                <dd>{companySummary.businessName || 'Sin registrar'}</dd>
              </div>
              <div>
                <dt>RUT</dt>
                <dd>{companySummary.rut || 'Sin registrar'}</dd>
              </div>
              <div>
                <dt>Giro</dt>
                <dd>{companySummary.activity || 'Sin registrar'}</dd>
              </div>
              <div>
                <dt>Ubicacion</dt>
                <dd>{companySummary.location || 'Sin registrar'}</dd>
              </div>
              <div>
                <dt>Telefono</dt>
                <dd>{companySummary.phone || 'Sin registrar'}</dd>
              </div>
              <div>
                <dt>Correo</dt>
                <dd>{companySummary.email || 'Sin registrar'}</dd>
              </div>
            </dl>
            {companySummary.receiptFooterMessage ? (
              <p className="muted small">
                Mensaje en boletas: {companySummary.receiptFooterMessage}
              </p>
            ) : null}
            {companySummary.updatedAt ? (
              <p className="muted small">
                Ultima actualizacion: {new Date(companySummary.updatedAt).toLocaleString()}
              </p>
            ) : null}
            {notice ? (
              <p className="success" aria-live="polite">
                {notice}
              </p>
            ) : null}
          </div>
        )}

        {!isLoading && !loadError && company && isEditing && (
          <form className="form-grid" onSubmit={handleSubmit} noValidate>
            <label>
              <span>Razon social *</span>
              <input
                className="input"
                value={values.businessName}
                onChange={event => {
                  setValues(prev => ({ ...prev, businessName: event.target.value }))
                  clearFieldError('businessName')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
                aria-invalid={fieldErrors.businessName ? 'true' : undefined}
                aria-describedby={
                  fieldErrors.businessName ? 'account-businessName-error' : undefined
                }
              />
              {fieldErrors.businessName ? (
                <p id="account-businessName-error" className="error" aria-live="polite">
                  {fieldErrors.businessName}
                </p>
              ) : null}
            </label>

            <label>
              <span>RUT *</span>
              <input
                className="input"
                value={values.rut}
                onChange={event => {
                  setValues(prev => ({ ...prev, rut: event.target.value }))
                  clearFieldError('rut')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
                aria-invalid={fieldErrors.rut ? 'true' : undefined}
                aria-describedby={fieldErrors.rut ? 'account-rut-error' : undefined}
              />
              {fieldErrors.rut ? (
                <p id="account-rut-error" className="error" aria-live="polite">
                  {fieldErrors.rut}
                </p>
              ) : null}
            </label>

            <label>
              <span>Giro</span>
              <input
                className="input"
                value={values.businessActivity}
                onChange={event => {
                  setValues(prev => ({ ...prev, businessActivity: event.target.value }))
                  clearFieldError('businessActivity')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
              />
            </label>

            <label>
              <span>Direccion</span>
              <input
                className="input"
                value={values.address}
                onChange={event => {
                  setValues(prev => ({ ...prev, address: event.target.value }))
                  clearFieldError('address')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
              />
            </label>

            <label>
              <span>Comuna</span>
              <input
                className="input"
                value={values.commune}
                onChange={event => {
                  setValues(prev => ({ ...prev, commune: event.target.value }))
                  clearFieldError('commune')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
              />
            </label>

            <label>
              <span>Telefono</span>
              <input
                className="input"
                value={values.phone}
                onChange={event => {
                  setValues(prev => ({ ...prev, phone: event.target.value }))
                  clearFieldError('phone')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
              />
            </label>

            <label>
              <span>Correo de contacto</span>
              <input
                className="input"
                value={values.email}
                onChange={event => {
                  setValues(prev => ({ ...prev, email: event.target.value }))
                  clearFieldError('email')
                  setNotice(null)
                }}
                disabled={mutation.isPending}
                aria-invalid={fieldErrors.email ? 'true' : undefined}
                aria-describedby={fieldErrors.email ? 'account-email-error' : undefined}
              />
              {fieldErrors.email ? (
                <p id="account-email-error" className="error" aria-live="polite">
                  {fieldErrors.email}
                </p>
              ) : null}
            </label>

            <label className="full">
              <span>Mensaje en comprobantes</span>
              <textarea
                className="input"
                rows={3}
                value={values.receiptFooterMessage}
                onChange={event => {
                  setValues(prev => ({ ...prev, receiptFooterMessage: event.target.value }))
                  clearFieldError('receiptFooterMessage')
                  setNotice(null)
                }}
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
                onClick={() => {
                  if (company) {
                    setValues(valuesFromCompany(company))
                  } else {
                    setValues(emptyCompanyFormValues())
                  }
                  setFieldErrors({})
                  setErrorMessage(null)
                  setNotice(null)
                  setIsEditing(false)
                }}
                disabled={mutation.isPending}
              >
                Cancelar
              </button>
            </div>

            {errorMessage ? (
              <p className="error" aria-live="assertive">
                {errorMessage}
              </p>
            ) : null}
            {notice ? (
              <p className="success" aria-live="polite">
                {notice}
              </p>
            ) : null}
          </form>
        )}
      </div>
    </div>
  )
}
