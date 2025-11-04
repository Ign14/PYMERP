import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createSupplierContact,
  listSupplierContacts,
  SupplierContact,
  SupplierContactPayload,
} from '../services/client'

type Props = {
  supplierId: string
  supplierName: string
  isOpen: boolean
  onClose: () => void
}

export default function SupplierContactsDialog({
  supplierId,
  supplierName,
  isOpen,
  onClose,
}: Props) {
  const queryClient = useQueryClient()
  const [isAdding, setIsAdding] = useState(false)
  const [formData, setFormData] = useState<SupplierContactPayload>({
    name: '',
    title: '',
    phone: '',
    email: '',
  })

  const contactsQuery = useQuery<SupplierContact[], Error>({
    queryKey: ['supplierContacts', supplierId],
    queryFn: () => listSupplierContacts(supplierId),
    enabled: isOpen,
  })

  const createMutation = useMutation({
    mutationFn: (payload: SupplierContactPayload) => createSupplierContact(supplierId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['supplierContacts', supplierId] })
      setFormData({ name: '', title: '', phone: '', email: '' })
      setIsAdding(false)
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!formData.name.trim()) return
    createMutation.mutate(formData)
  }

  const handleCancel = () => {
    setFormData({ name: '', title: '', phone: '', email: '' })
    setIsAdding(false)
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Contactos de {supplierName}</h2>
          <button className="btn-close" onClick={onClose} aria-label="Cerrar">
            ✕
          </button>
        </div>

        <div className="modal-body">
          {contactsQuery.isLoading && <p>Cargando contactos...</p>}
          {contactsQuery.isError && (
            <p className="error">
              {contactsQuery.error?.message ?? 'No se pudieron cargar los contactos'}
            </p>
          )}

          {!contactsQuery.isLoading && !contactsQuery.isError && (
            <>
              {(contactsQuery.data ?? []).length === 0 && !isAdding && (
                <p className="muted">No hay contactos registrados</p>
              )}

              {(contactsQuery.data ?? []).length > 0 && (
                <div className="contacts-list">
                  {contactsQuery.data!.map(contact => (
                    <div key={contact.id} className="contact-card">
                      <div className="contact-name">
                        <strong>{contact.name}</strong>
                        {contact.title && <span className="contact-title">{contact.title}</span>}
                      </div>
                      <div className="contact-details">
                        {contact.phone && (
                          <div>
                            <span className="label">📞 Teléfono:</span>
                            <span>{contact.phone}</span>
                          </div>
                        )}
                        {contact.email && (
                          <div>
                            <span className="label">✉️ Email:</span>
                            <a href={`mailto:${contact.email}`}>{contact.email}</a>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {isAdding ? (
                <form onSubmit={handleSubmit} className="contact-form">
                  <h3>Nuevo Contacto</h3>
                  <div className="form-group">
                    <label htmlFor="contact-name">Nombre *</label>
                    <input
                      id="contact-name"
                      type="text"
                      value={formData.name}
                      onChange={e => setFormData({ ...formData, name: e.target.value })}
                      required
                      autoFocus
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="contact-title">Cargo</label>
                    <input
                      id="contact-title"
                      type="text"
                      value={formData.title || ''}
                      onChange={e => setFormData({ ...formData, title: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="contact-phone">Teléfono</label>
                    <input
                      id="contact-phone"
                      type="tel"
                      value={formData.phone || ''}
                      onChange={e => setFormData({ ...formData, phone: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="contact-email">Email</label>
                    <input
                      id="contact-email"
                      type="email"
                      value={formData.email || ''}
                      onChange={e => setFormData({ ...formData, email: e.target.value })}
                    />
                  </div>
                  {createMutation.isError && (
                    <p className="error">
                      {(createMutation.error as Error)?.message ?? 'No se pudo crear el contacto'}
                    </p>
                  )}
                  <div className="form-actions">
                    <button type="button" className="btn btn-ghost" onClick={handleCancel}>
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={createMutation.isPending}
                    >
                      {createMutation.isPending ? 'Guardando...' : 'Guardar'}
                    </button>
                  </div>
                </form>
              ) : (
                <button className="btn btn-primary" onClick={() => setIsAdding(true)}>
                  + Agregar contacto
                </button>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
