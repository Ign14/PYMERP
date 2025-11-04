import { FormEvent, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createCustomer, updateCustomer, Customer, CustomerPayload } from '../../services/client'
import Modal from './Modal'

type Props = {
  open: boolean
  onClose: () => void
  editingCustomer?: Customer | null
}

type FormState = Omit<CustomerPayload, 'lat' | 'lng'> & {
  latText: string
  lngText: string
}

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

export default function CustomerCreateDialog({ open, onClose, editingCustomer }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<FormState>(() =>
    editingCustomer
      ? {
          name: editingCustomer.name,
          rut: editingCustomer.rut || '',
          phone: editingCustomer.phone || '',
          email: editingCustomer.email || '',
          address: editingCustomer.address || '',
          segment: editingCustomer.segment || '',
          contactPerson: editingCustomer.contactPerson || '',
          notes: editingCustomer.notes || '',
          active: editingCustomer.active !== false,
          latText: editingCustomer.lat?.toString() || '',
          lngText: editingCustomer.lng?.toString() || '',
        }
      : createEmptyForm()
  )

  const createMutation = useMutation({
    mutationFn: (payload: CustomerPayload) => createCustomer(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'], exact: false })
      handleClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CustomerPayload }) =>
      updateCustomer(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'], exact: false })
      handleClose()
    },
  })

  const handleClose = () => {
    setForm(createEmptyForm())
    onClose()
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!form.name || !form.name.trim()) {
      alert('El nombre es obligatorio')
      return
    }

    const parseCoordinate = (value: string): number | null => {
      if (!value || !value.trim()) {
        return null
      }
      const parsed = Number(value.trim())
      return Number.isFinite(parsed) ? parsed : null
    }

    const payload: CustomerPayload = {
      name: form.name.trim(),
      rut: form.rut?.trim() || undefined,
      phone: form.phone?.trim() || undefined,
      email: form.email?.trim() || undefined,
      address: form.address?.trim() || undefined,
      segment: form.segment?.trim() || undefined,
      contactPerson: form.contactPerson?.trim() || undefined,
      notes: form.notes?.trim() || undefined,
      active: form.active,
      lat: parseCoordinate(form.latText),
      lng: parseCoordinate(form.lngText),
    }

    if (editingCustomer) {
      updateMutation.mutate({ id: editingCustomer.id, payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title={editingCustomer ? 'Editar Cliente' : 'Nuevo Cliente'}
    >
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          <span>Nombre *</span>
          <input
            className="input"
            autoFocus
            value={form.name}
            onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
            placeholder="Cliente demo"
          />
        </label>
        <label>
          <span>RUT</span>
          <input
            className="input"
            value={form.rut ?? ''}
            onChange={e => setForm(prev => ({ ...prev, rut: e.target.value }))}
            placeholder="12.345.678-9"
          />
        </label>
        <label>
          <span>Email</span>
          <input
            className="input"
            type="email"
            value={form.email ?? ''}
            onChange={e => setForm(prev => ({ ...prev, email: e.target.value }))}
            placeholder="cliente@correo.com"
          />
        </label>
        <label>
          <span>Teléfono</span>
          <input
            className="input"
            value={form.phone ?? ''}
            onChange={e => setForm(prev => ({ ...prev, phone: e.target.value }))}
            placeholder="+56 9 0000 0000"
          />
        </label>
        <label>
          <span>Persona de Contacto</span>
          <input
            className="input"
            value={form.contactPerson ?? ''}
            onChange={e => setForm(prev => ({ ...prev, contactPerson: e.target.value }))}
            placeholder="Juan Pérez"
          />
        </label>
        <label>
          <span>Segmento</span>
          <input
            className="input"
            value={form.segment ?? ''}
            onChange={e => setForm(prev => ({ ...prev, segment: e.target.value }))}
            placeholder="Retail"
          />
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          <span>Dirección</span>
          <input
            className="input"
            value={form.address ?? ''}
            onChange={e => setForm(prev => ({ ...prev, address: e.target.value }))}
            placeholder="Av. Demo 123"
          />
        </label>
        <label>
          <span>Latitud</span>
          <input
            className="input"
            type="number"
            step="any"
            value={form.latText}
            onChange={e => setForm(prev => ({ ...prev, latText: e.target.value }))}
            placeholder="-33.4489"
          />
        </label>
        <label>
          <span>Longitud</span>
          <input
            className="input"
            type="number"
            step="any"
            value={form.lngText}
            onChange={e => setForm(prev => ({ ...prev, lngText: e.target.value }))}
            placeholder="-70.6693"
          />
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          <span>Notas</span>
          <textarea
            className="input"
            rows={3}
            value={form.notes ?? ''}
            onChange={e => setForm(prev => ({ ...prev, notes: e.target.value }))}
            placeholder="Observaciones adicionales..."
          />
        </label>
        <label className="checkbox-label" style={{ gridColumn: '1 / -1' }}>
          <input
            type="checkbox"
            checked={form.active !== false}
            onChange={e => setForm(prev => ({ ...prev, active: e.target.checked }))}
          />
          <span>Cliente activo</span>
        </label>
        <div className="buttons" style={{ gridColumn: '1 / -1' }}>
          <button
            className="btn"
            type="submit"
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {editingCustomer
              ? updateMutation.isPending
                ? 'Actualizando...'
                : 'Actualizar'
              : createMutation.isPending
                ? 'Guardando...'
                : 'Crear'}
          </button>
          <button className="btn ghost" type="button" onClick={handleClose}>
            Cancelar
          </button>
        </div>
        {createMutation.isError && (
          <p className="error" style={{ gridColumn: '1 / -1' }}>
            {(createMutation.error as Error)?.message}
          </p>
        )}
        {updateMutation.isError && (
          <p className="error" style={{ gridColumn: '1 / -1' }}>
            {(updateMutation.error as Error)?.message}
          </p>
        )}
      </form>
    </Modal>
  )
}
