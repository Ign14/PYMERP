import { useEffect, useMemo, useRef, useState } from 'react'
import Modal from '../dialogs/Modal'
import { useSaleDocuments } from '../../hooks/useSaleDocuments'
import type { SaleDocument } from '../../services/client'

const DATE_FORMATTER = new Intl.DateTimeFormat('es-CL', { dateStyle: 'medium' })
const CURRENCY_FORMATTER = new Intl.NumberFormat('es-CL', {
  style: 'currency',
  currency: 'CLP',
  maximumFractionDigits: 0,
})

function formatDate(value: string) {
  if (!value) {
    return '—'
  }
  const timestamp = Date.parse(value)
  if (Number.isNaN(timestamp)) {
    return value
  }
  return DATE_FORMATTER.format(new Date(timestamp))
}

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—'
  }
  return CURRENCY_FORMATTER.format(value)
}

function getDocumentNumber(document: SaleDocument): string {
  const parts = [document.documentNumber]
  if (!parts[0] || !parts[0]?.trim()) {
    parts[0] = document.docNumber
  }
  if (!parts[0] || !parts[0]?.trim()) {
    if (document.series && document.folio !== undefined && document.folio !== null) {
      parts[0] = `${document.series}-${document.folio}`
    }
  }
  return parts[0]?.trim() || document.id
}

type SaleDocumentsModalProps = {
  isOpen: boolean
  onClose: () => void
  pageSize?: number
}

const DEFAULT_PAGE_SIZE = 10

export default function SaleDocumentsModal({
  isOpen,
  onClose,
  pageSize = DEFAULT_PAGE_SIZE,
}: SaleDocumentsModalProps) {
  const [page, setPage] = useState(0)
  const closeButtonRef = useRef<HTMLButtonElement | null>(null)

  useEffect(() => {
    if (isOpen) {
      setPage(0)
    }
  }, [isOpen])

  const documentsQuery = useSaleDocuments({
    page,
    size: pageSize,
    enabled: isOpen,
  })

  const documents = documentsQuery.data?.content ?? []
  const totalPages = documentsQuery.data?.totalPages ?? 1

  const isEmpty = !documentsQuery.isLoading && !documentsQuery.isError && documents.length === 0

  const statusMessage = useMemo(() => {
    if (documentsQuery.isFetching && !documentsQuery.isLoading) {
      return 'Actualizando documentos...'
    }
    return null
  }, [documentsQuery.isFetching, documentsQuery.isLoading])

  const handleRetry = () => {
    void documentsQuery.refetch()
  }

  const handlePrevious = () => {
    setPage(prev => Math.max(0, prev - 1))
  }

  const handleNext = () => {
    setPage(prev => prev + 1)
  }

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      title="Documentos de venta"
      initialFocusRef={closeButtonRef}
      className="modal--wide"
    >
      <div className="sale-documents-modal" aria-live="polite">
        {documentsQuery.isLoading && <p role="status">Cargando documentos...</p>}
        {documentsQuery.isError && (
          <div className="sale-documents-modal__error" role="alert">
            <p>
              {documentsQuery.error?.message ?? 'No se pudieron obtener los documentos de venta.'}
            </p>
            <button className="btn" type="button" onClick={handleRetry}>
              Reintentar
            </button>
          </div>
        )}
        {statusMessage && (
          <p className="muted" role="status">
            {statusMessage}
          </p>
        )}
        {isEmpty && <p className="muted">No hay documentos de ventas registrados.</p>}

        {!documentsQuery.isLoading && !documentsQuery.isError && documents.length > 0 && (
          <div className="sale-documents-modal__table">
            <div className="table-wrapper">
              <table className="table sale-documents-modal__grid">
                <thead>
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Número de documento</th>
                    <th scope="col">Cliente</th>
                    <th scope="col">Total</th>
                    <th scope="col" className="sale-documents-modal__status-column">
                      Estado
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {documents.map(document => (
                    <tr key={document.id}>
                      <td>{formatDate(document.date)}</td>
                      <td>
                        <span
                          className="sale-documents-modal__cell"
                          title={getDocumentNumber(document)}
                        >
                          {getDocumentNumber(document)}
                        </span>
                      </td>
                      <td>
                        <span className="sale-documents-modal__cell" title={document.customerName}>
                          {document.customerName || '—'}
                        </span>
                      </td>
                      <td className="mono">{formatCurrency(document.total)}</td>
                      <td className="sale-documents-modal__status-column">
                        {document.status ? (
                          <span
                            className={`status ${document.status.toLowerCase()}`}
                            aria-label={document.status}
                          >
                            {document.status}
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {!documentsQuery.isError && documents.length > 0 && (
          <div className="sale-documents-modal__pagination">
            <button
              className="btn"
              type="button"
              onClick={handlePrevious}
              disabled={page === 0 || documentsQuery.isFetching}
            >
              Anterior
            </button>
            <span className="muted">
              Página {page + 1} de {totalPages}
            </span>
            <button
              className="btn"
              type="button"
              onClick={handleNext}
              disabled={page + 1 >= totalPages || documentsQuery.isFetching}
            >
              Siguiente
            </button>
          </div>
        )}
      </div>

      <footer className="sale-documents-modal__footer">
        <button ref={closeButtonRef} className="btn ghost" type="button" onClick={onClose}>
          Cerrar
        </button>
      </footer>
    </Modal>
  )
}
