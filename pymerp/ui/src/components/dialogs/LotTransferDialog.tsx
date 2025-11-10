import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import Modal from './Modal'
import {
  listLocations,
  transferInventoryLot,
  Location,
  LotTransferRequest,
  ProductStockLot,
} from '../../services/client'
import axios from 'axios'

type Props = {
  open: boolean
  lot: ProductStockLot | null
  productName: string
  onClose: () => void
  onTransferred?: () => void
}

type FormState = {
  targetLocationId: string
  qty: string
  note: string
}

const INITIAL_FORM: FormState = {
  targetLocationId: '',
  qty: '',
  note: '',
}

export default function LotTransferDialog({ open, lot, productName, onClose, onTransferred }: Props) {
  const [form, setForm] = useState<FormState>(INITIAL_FORM)
  const [formError, setFormError] = useState<string | null>(null)

  const availableQty = lot?.quantity ?? 0
  const currentLocationLabel = useMemo(() => {
    if (!lot) return '—'
    if (lot.locationCode && lot.locationName) {
      return `${lot.locationCode} · ${lot.locationName}`
    }
    if (lot.locationCode) return lot.locationCode
    if (lot.locationName) return lot.locationName
    return 'Sin ubicación asignada'
  }, [lot])

  useEffect(() => {
    if (!open) {
      setForm(INITIAL_FORM)
      setFormError(null)
      return
    }
    setForm(prev => ({ ...INITIAL_FORM, qty: availableQty > 0 ? String(Math.min(availableQty, 1)) : '' }))
    setFormError(null)
  }, [open, availableQty])

  const locationsQuery = useQuery<Location[], Error>({
    queryKey: ['locations'],
    queryFn: () => listLocations(),
    enabled: open,
    staleTime: 5 * 60 * 1000,
  })

  const targetOptions = useMemo(() => {
    if (!locationsQuery.data || !lot?.locationId) {
      return locationsQuery.data ?? []
    }
    return locationsQuery.data.filter(location => location.id !== lot.locationId)
  }, [locationsQuery.data, lot?.locationId])

  const mutation = useMutation({
    mutationFn: async () => {
      if (!lot) {
        throw new Error('No hay lote seleccionado')
      }
      const qtyValue = Number(form.qty)
      if (!Number.isFinite(qtyValue) || qtyValue <= 0) {
        throw new Error('La cantidad debe ser mayor a 0')
      }
      if (qtyValue > availableQty) {
        throw new Error(`La cantidad no puede superar ${availableQty}`)
      }
      if (!form.targetLocationId) {
        throw new Error('Selecciona una ubicación destino')
      }
      const payload: LotTransferRequest = {
        targetLocationId: form.targetLocationId,
        qty: qtyValue,
        note: form.note.trim() || undefined,
      }
      await transferInventoryLot(lot.lotId, payload)
    },
    onSuccess: () => {
      setForm(INITIAL_FORM)
      onTransferred?.()
      onClose()
    },
    onError: error => {
      const message = resolveErrorMessage(error)
      setFormError(message)
    },
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (mutation.isPending) return
    setFormError(null)
    mutation.mutate()
  }

  const disableTransfer =
    !lot ||
    !form.targetLocationId ||
    !form.qty ||
    Number(form.qty) <= 0 ||
    Number(form.qty) > availableQty ||
    targetOptions.length === 0 ||
    mutation.isPending

  return (
    <Modal open={open} onClose={onClose} title="Transferir stock" className="modal-card" >
      <form onSubmit={handleSubmit} style={{ width: '100%', maxWidth: '480px' }}>
        <section className="transfer-summary">
          <p className="muted small">Producto</p>
          <p style={{ marginBottom: '0.75rem', fontWeight: 600 }}>{productName}</p>
          <div className="summary-row">
            <div>
              <p className="muted small">Ubicación actual</p>
              <p>{currentLocationLabel}</p>
            </div>
            <div>
              <p className="muted small">Stock disponible</p>
              <p className="mono">{availableQty}</p>
            </div>
          </div>
        </section>

        {targetOptions.length === 0 && !locationsQuery.isLoading && (
          <p className="panel-error" style={{ marginBottom: '1rem' }}>
            No hay ubicaciones destino disponibles para transferir.
          </p>
        )}

        <div className="form-grid" style={{ display: 'grid', gap: '1rem', marginTop: '1rem' }}>
          <label className="form-field">
            <span className="label">Ubicación destino</span>
            <select
              className="input"
              value={form.targetLocationId}
              onChange={event => setForm(prev => ({ ...prev, targetLocationId: event.target.value }))}
              disabled={locationsQuery.isLoading || mutation.isPending || targetOptions.length === 0}
              data-autofocus
            >
              <option value="">Selecciona una ubicación…</option>
              {targetOptions.map(location => (
                <option key={location.id} value={location.id}>
                  {location.code} · {location.name}
                </option>
              ))}
            </select>
            {locationsQuery.isLoading && <span className="muted small">Cargando ubicaciones…</span>}
          </label>

          <label className="form-field">
            <span className="label">Cantidad a transferir</span>
            <input
              className="input"
              type="number"
              min="0"
              step="0.01"
              value={form.qty}
              onChange={event => setForm(prev => ({ ...prev, qty: event.target.value }))}
              disabled={mutation.isPending}
            />
            <span className="muted very-small">Máximo permitido: {availableQty}</span>
          </label>

          <label className="form-field">
            <span className="label">Comentario (opcional)</span>
            <textarea
              className="input"
              rows={3}
              value={form.note}
              onChange={event => setForm(prev => ({ ...prev, note: event.target.value }))}
              placeholder="Ej. mover a bodega principal"
              disabled={mutation.isPending}
            />
          </label>
        </div>

        {formError && (
          <p className="panel-error" style={{ marginTop: '1rem' }}>
            {formError}
          </p>
        )}

        <footer
          style={{
            marginTop: '1.5rem',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem',
          }}
        >
          <button type="button" className="btn ghost" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </button>
          <button type="submit" className="btn primary" disabled={disableTransfer}>
            {mutation.isPending ? 'Transfiriendo…' : 'Transferir'}
          </button>
        </footer>
      </form>
    </Modal>
  )
}

function resolveErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const detail = error.response?.data
    if (detail) {
      if (typeof detail === 'string') {
        return detail
      }
      if (typeof detail === 'object') {
        const byKey =
          (detail as { message?: string; detail?: string; error?: string }).message ??
          (detail as { detail?: string }).detail ??
          (detail as { error?: string }).error
        if (byKey && typeof byKey === 'string') {
          return byKey
        }
      }
    }
    return error.response?.statusText ?? 'No se pudo completar la transferencia'
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return 'No se pudo completar la transferencia'
}
