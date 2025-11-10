import { FormEvent, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  createCompanyWithDetails,
  updateCompanyWithDetails,
  CompanyResponse,
  CompanyCreateRequest,
  ParentLocationRequest,
} from '../../services/client'
import Modal from './Modal'

type Props = {
  open: boolean
  onClose: () => void
  editingCompany?: CompanyResponse | null
}

type FormState = {
  businessName: string
  fantasyName: string
  rut: string
  logoUrl: string
  businessActivity: string
  address: string
  commune: string
  phone: string
  email: string
  receiptFooterMessage: string
  slogan: string
  parentLocations: ParentLocationRequest[]
}

const createEmptyForm = (): FormState => ({
  businessName: '',
  fantasyName: '',
  rut: '',
  logoUrl: '',
  businessActivity: '',
  address: '',
  commune: '',
  phone: '',
  email: '',
  receiptFooterMessage: '',
  slogan: '',
  parentLocations: [],
})

export default function CompanyManagementDialog({ open, onClose, editingCompany }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<FormState>(() =>
    editingCompany
      ? {
          businessName: editingCompany.businessName,
          fantasyName: editingCompany.fantasyName || '',
          rut: editingCompany.rut,
          logoUrl: editingCompany.logoUrl || '',
          businessActivity: editingCompany.businessActivity || '',
          address: editingCompany.address || '',
          commune: editingCompany.commune || '',
          phone: editingCompany.phone || '',
          email: editingCompany.email || '',
          receiptFooterMessage: editingCompany.receiptFooterMessage || '',
          slogan: editingCompany.slogan || '',
          parentLocations: editingCompany.parentLocations || [],
        }
      : createEmptyForm()
  )

  const createMutation = useMutation({
    mutationFn: (payload: CompanyCreateRequest) => createCompanyWithDetails(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'], exact: false })
      handleClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CompanyCreateRequest }) =>
      updateCompanyWithDetails(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'], exact: false })
      handleClose()
    },
  })

  const handleClose = () => {
    setForm(createEmptyForm())
    onClose()
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!form.businessName.trim()) {
      alert('La razón social es obligatoria')
      return
    }
    if (!form.rut.trim()) {
      alert('El RUT es obligatorio')
      return
    }

    const payload: CompanyCreateRequest = {
      businessName: form.businessName.trim(),
      fantasyName: form.fantasyName.trim() || undefined,
      rut: form.rut.trim(),
      logoUrl: form.logoUrl.trim() || undefined,
      businessActivity: form.businessActivity.trim() || undefined,
      address: form.address.trim() || undefined,
      commune: form.commune.trim() || undefined,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
      receiptFooterMessage: form.receiptFooterMessage.trim() || undefined,
      slogan: form.slogan.trim() || undefined,
      parentLocations: form.parentLocations.filter(
        loc => loc.name.trim() && loc.code.trim()
      ),
    }

    if (editingCompany) {
      updateMutation.mutate({ id: editingCompany.id, payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  const addParentLocation = () => {
    setForm(prev => ({
      ...prev,
      parentLocations: [...prev.parentLocations, { name: '', code: '' }],
    }))
  }

  const removeParentLocation = (index: number) => {
    setForm(prev => ({
      ...prev,
      parentLocations: prev.parentLocations.filter((_, i) => i !== index),
    }))
  }

  const updateParentLocation = (index: number, field: 'name' | 'code', value: string) => {
    setForm(prev => {
      const updated = [...prev.parentLocations]
      updated[index] = { ...updated[index], [field]: value }
      return { ...prev, parentLocations: updated }
    })
  }

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title={editingCompany ? 'Editar Empresa' : 'Nueva Empresa'}
    >
      <form onSubmit={onSubmit}>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium mb-1">
              Razón Social <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={form.businessName}
              onChange={e => setForm({ ...form, businessName: e.target.value })}
              className="w-full px-3 py-2 border rounded"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Nombre de Fantasía</label>
            <input
              type="text"
              value={form.fantasyName}
              onChange={e => setForm({ ...form, fantasyName: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">
              RUT <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={form.rut}
              onChange={e => setForm({ ...form, rut: e.target.value })}
              className="w-full px-3 py-2 border rounded"
              placeholder="12345678-9"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">URL del Logo</label>
            <input
              type="text"
              value={form.logoUrl}
              onChange={e => setForm({ ...form, logoUrl: e.target.value })}
              className="w-full px-3 py-2 border rounded"
              placeholder="https://ejemplo.com/logo.png"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Giro Comercial</label>
            <input
              type="text"
              value={form.businessActivity}
              onChange={e => setForm({ ...form, businessActivity: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Dirección</label>
            <input
              type="text"
              value={form.address}
              onChange={e => setForm({ ...form, address: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Comuna</label>
            <input
              type="text"
              value={form.commune}
              onChange={e => setForm({ ...form, commune: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Teléfono</label>
            <input
              type="text"
              value={form.phone}
              onChange={e => setForm({ ...form, phone: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Email</label>
            <input
              type="email"
              value={form.email}
              onChange={e => setForm({ ...form, email: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Slogan</label>
            <input
              type="text"
              value={form.slogan}
              onChange={e => setForm({ ...form, slogan: e.target.value })}
              className="w-full px-3 py-2 border rounded"
            />
          </div>
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium mb-1">Mensaje para recibos</label>
          <textarea
            value={form.receiptFooterMessage}
            onChange={e => setForm({ ...form, receiptFooterMessage: e.target.value })}
            className="w-full px-3 py-2 border rounded"
            rows={3}
          />
        </div>

        <div className="mb-4">
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium">Ubicaciones Padre</label>
            <button
              type="button"
              onClick={addParentLocation}
              className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm"
            >
              + Agregar
            </button>
          </div>
          {form.parentLocations.length === 0 ? (
            <p className="text-sm text-gray-500">
              No hay ubicaciones padre. Agrégalas para organizar el inventario.
            </p>
          ) : (
            <div className="space-y-2">
              {form.parentLocations.map((loc, idx) => (
                <div key={idx} className="flex gap-2 items-center">
                  <input
                    type="text"
                    value={loc.name}
                    onChange={e => updateParentLocation(idx, 'name', e.target.value)}
                    placeholder="Nombre (ej: Bodega Principal)"
                    className="flex-1 px-3 py-2 border rounded"
                  />
                  <input
                    type="text"
                    value={loc.code}
                    onChange={e => updateParentLocation(idx, 'code', e.target.value)}
                    placeholder="Código (ej: BP)"
                    className="w-32 px-3 py-2 border rounded"
                  />
                  <button
                    type="button"
                    onClick={() => removeParentLocation(idx)}
                    className="px-3 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-4 border-t">
          <button
            type="button"
            onClick={handleClose}
            className="px-4 py-2 border rounded hover:bg-gray-100"
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {editingCompany ? 'Actualizar' : 'Crear'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
