import { forwardRef, useCallback, useEffect, useImperativeHandle, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deleteSupplier, listSuppliers, Supplier } from '../services/client'
import SupplierContactsDialog from './SupplierContactsDialog'

type Props = {
  onOpenCreateDialog?: () => void
  onOpenEditDialog?: (supplier: Supplier) => void
}

export type SuppliersCardHandle = {
  openCreate: () => void
}

const SuppliersCard = forwardRef<SuppliersCardHandle, Props>(
  ({ onOpenCreateDialog, onOpenEditDialog }, ref) => {
    const queryClient = useQueryClient()
    const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null)
    const [query, setQuery] = useState('')
    const [activeFilter, setActiveFilter] = useState<boolean | undefined>(true)
    const [contactsDialogOpen, setContactsDialogOpen] = useState(false)

    const suppliersQuery = useQuery<Supplier[], Error>({
      queryKey: ['suppliers', query, activeFilter],
      queryFn: () => listSuppliers(query || undefined, activeFilter),
      refetchOnWindowFocus: false,
    })

    const openCreate = useCallback(() => {
      onOpenCreateDialog?.()
    }, [onOpenCreateDialog])

    useImperativeHandle(
      ref,
      () => ({
        openCreate,
      }),
      [openCreate]
    )

    useEffect(() => {
      if (!selectedSupplier) return
      const updated = suppliersQuery.data?.find(item => item.id === selectedSupplier.id)
      if (!updated) {
        setSelectedSupplier(null)
      } else if (updated !== selectedSupplier) {
        setSelectedSupplier(updated)
      }
    }, [suppliersQuery.data, selectedSupplier])

    const deleteMutation = useMutation({
      mutationFn: (id: string) => deleteSupplier(id),
      onSuccess: (_, id) => {
        queryClient.invalidateQueries({ queryKey: ['suppliers'] })
        if (selectedSupplier?.id === id) {
          setSelectedSupplier(null)
        }
      },
    })

    const handleEdit = () => {
      if (!selectedSupplier) return
      onOpenEditDialog?.(selectedSupplier)
    }

    const handleDelete = () => {
      if (!selectedSupplier || deleteMutation.isPending) return
      if (window.confirm(`¿Eliminar proveedor ${selectedSupplier.name}?`)) {
        deleteMutation.mutate(selectedSupplier.id)
      }
    }

    const { data, isLoading, isError, error, refetch, isFetching } = suppliersQuery

    const formatValue = (value?: string | null) => {
      if (!value) {
        return 'Sin información'
      }
      const trimmed = value.trim()
      return trimmed.length > 0 ? trimmed : 'Sin información'
    }

    const detailItems = selectedSupplier
      ? [
          { label: 'Nombre', value: selectedSupplier.name },
          { label: 'RUT', value: selectedSupplier.rut ?? null },
          { label: 'Dirección', value: selectedSupplier.address ?? null },
          { label: 'Comuna', value: selectedSupplier.commune ?? null },
          { label: 'Giro / actividad', value: selectedSupplier.businessActivity ?? null },
          { label: 'Teléfono', value: selectedSupplier.phone ?? null },
          { label: 'Email', value: selectedSupplier.email ?? null, type: 'email' as const },
        ]
      : []

    return (
      <div className="card-content">
        <div className="card-header">
          <h2>Proveedores</h2>
          <button className="btn btn-secondary" onClick={() => refetch()} disabled={isFetching}>
            {isFetching ? 'Cargando...' : '🔄 Refrescar'}
          </button>
        </div>

        <div className="search-box">
          <input
            type="text"
            placeholder="Buscar por nombre, RUT, email o teléfono..."
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
        </div>

        <div className="filter-buttons">
          <button
            className={`btn ${activeFilter === undefined ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setActiveFilter(undefined)}
          >
            Todos
          </button>
          <button
            className={`btn ${activeFilter === true ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setActiveFilter(true)}
          >
            Activos
          </button>
          <button
            className={`btn ${activeFilter === false ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => setActiveFilter(false)}
          >
            Inactivos
          </button>
        </div>

        <div className="inline-actions">
          <button
            className="btn ghost"
            type="button"
            onClick={handleEdit}
            disabled={!selectedSupplier}
          >
            ✏️ Editar
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => setContactsDialogOpen(true)}
            disabled={!selectedSupplier}
          >
            👥 Contactos
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={handleDelete}
            disabled={!selectedSupplier || deleteMutation.isPending}
          >
            {deleteMutation.isPending ? 'Eliminando...' : '🗑️ Eliminar'}
          </button>
        </div>
        {deleteMutation.isError && (
          <p className="error">
            {(deleteMutation.error as Error)?.message ?? 'No se pudo eliminar el proveedor'}
          </p>
        )}

        {isLoading && <p>Cargando proveedores...</p>}
        {isError && (
          <p className="error">{error?.message ?? 'No se pudieron cargar los proveedores'}</p>
        )}

        {!isLoading && !isError && (
          <>
            <ul className="list">
              {(data ?? []).map(supplier => (
                <li
                  key={supplier.id}
                  className={selectedSupplier?.id === supplier.id ? 'selected' : undefined}
                  onClick={() => setSelectedSupplier(supplier)}
                >
                  <strong>{supplier.name}</strong>
                  {supplier.rut ? <span className="mono small"> RUT {supplier.rut}</span> : null}
                </li>
              ))}
              {(data ?? []).length === 0 && (
                <li className="muted">No hay proveedores registrados</li>
              )}
            </ul>
            {selectedSupplier ? (
              <div className="supplier-details" aria-live="polite">
                {detailItems.map(item => (
                  <div key={item.label} className="supplier-detail-item">
                    <span className="supplier-detail-label">{item.label}</span>
                    <span className="supplier-detail-value">
                      {item.type === 'email' && item.value && item.value.trim().length > 0 ? (
                        <a href={`mailto:${item.value.trim()}`}>{item.value.trim()}</a>
                      ) : (
                        formatValue(item.value)
                      )}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="muted">Selecciona un proveedor para ver sus datos.</p>
            )}
          </>
        )}

        {selectedSupplier && (
          <SupplierContactsDialog
            supplierId={selectedSupplier.id}
            supplierName={selectedSupplier.name}
            isOpen={contactsDialogOpen}
            onClose={() => setContactsDialogOpen(false)}
          />
        )}
      </div>
    )
  }
)

SuppliersCard.displayName = 'SuppliersCard'

export default SuppliersCard
