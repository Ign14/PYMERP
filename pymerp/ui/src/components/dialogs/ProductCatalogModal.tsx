import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import Modal from './Modal'
import { listProducts, Product } from '../../services/client'
import placeholderImage from '../../../assets/product-placeholder.svg'

type Props = {
  open: boolean
  onClose: () => void
  onSelectProduct: (product: Product) => void
}

const DEFAULT_PAGE_SIZE = 12

export default function ProductCatalogModal({ open, onClose, onSelectProduct }: Props) {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<'active' | 'inactive' | 'all'>('active')

  const productsQuery = useQuery({
    queryKey: ['products', { q: search, page, status: statusFilter }],
    queryFn: () =>
      listProducts({
        q: search || undefined,
        page,
        size: DEFAULT_PAGE_SIZE,
        status: statusFilter,
      }),
    placeholderData: keepPreviousData,
    enabled: open,
  })

  const products: Product[] = productsQuery.data?.content ?? []
  const totalPages = productsQuery.data?.totalPages ?? 1

  const handleProductClick = (product: Product) => {
    onSelectProduct(product)
  }

  return (
    <Modal open={open} onClose={onClose} title="Catálogo de productos" className="modal-wide">
      <div style={{ width: '100%', maxWidth: '1000px' }}>
        {/* Filtros mejorados */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr auto',
            gap: '1rem',
            marginBottom: '1.5rem',
            padding: '1rem',
            background: 'var(--bg-secondary)',
            borderRadius: '12px',
            border: '1px solid var(--border-color)',
          }}
        >
          <input
            className="input"
            type="text"
            placeholder="🔍 Buscar por nombre, SKU o código..."
            value={search}
            onChange={e => {
              setSearch(e.target.value)
              setPage(0)
            }}
            data-autofocus
            style={{ fontSize: '0.95rem' }}
          />

          <select
            className="input"
            value={statusFilter}
            onChange={e => {
              setStatusFilter(e.target.value as 'active' | 'inactive' | 'all')
              setPage(0)
            }}
            style={{ minWidth: '140px' }}
          >
            <option value="active">✓ Activos</option>
            <option value="inactive">✗ Inactivos</option>
            <option value="all">📋 Todos</option>
          </select>
        </div>

        {/* Grid de productos */}
        {productsQuery.isLoading && (
          <p className="muted" style={{ textAlign: 'center', padding: '2rem' }}>
            Cargando productos...
          </p>
        )}

        {productsQuery.isError && (
          <p className="error" style={{ textAlign: 'center', padding: '2rem' }}>
            {(productsQuery.error as Error)?.message ?? 'No se pudieron cargar los productos'}
          </p>
        )}

        {!productsQuery.isLoading && products.length === 0 && (
          <p className="muted" style={{ textAlign: 'center', padding: '2rem' }}>
            No se encontraron productos
          </p>
        )}

        {!productsQuery.isLoading && products.length > 0 && (
          <>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
                gap: '1.25rem',
                marginBottom: '1.5rem',
              }}
            >
              {products.map(product => {
                const imageUrl =
                  typeof product.imageUrl === 'string' && product.imageUrl.trim().length > 0
                    ? product.imageUrl
                    : placeholderImage

                const price = product.currentPrice && Number(product.currentPrice) > 0
                  ? `$${Number(product.currentPrice).toLocaleString()}`
                  : 'Sin precio'

                return (
                  <button
                    key={product.id}
                    type="button"
                    onClick={() => handleProductClick(product)}
                    className="product-catalog-card"
                  >
                    <div className="product-catalog-card-image-wrapper">
                      <img
                        src={imageUrl}
                        alt={product.name}
                        className="product-catalog-card-image"
                      />
                      {!product.active && (
                        <span className="product-catalog-card-badge-inactive">Inactivo</span>
                      )}
                    </div>

                    <div className="product-catalog-card-content">
                      <h4 className="product-catalog-card-title" title={product.name}>
                        {product.name}
                      </h4>
                      <p className="product-catalog-card-sku">{product.sku}</p>

                      <div className="product-catalog-card-footer">
                        <span className="product-catalog-card-label">Precio</span>
                        <span className="product-catalog-card-price">{price}</span>
                      </div>

                      {product.category && (
                        <span className="product-catalog-card-category" title={product.category}>
                          {product.category}
                        </span>
                      )}
                    </div>
                  </button>
                )
              })}
            </div>

            {/* Paginación */}
            {totalPages > 1 && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  gap: '0.5rem',
                  paddingTop: '1rem',
                  borderTop: '1px solid var(--border-color)',
                }}
              >
                <button
                  className="btn ghost small"
                  onClick={() => setPage(prev => Math.max(0, prev - 1))}
                  disabled={page === 0}
                >
                  ‹ Anterior
                </button>
                <span className="muted small">
                  Página {page + 1} de {totalPages}
                </span>
                <button
                  className="btn ghost small"
                  onClick={() => setPage(prev => Math.min(totalPages - 1, prev + 1))}
                  disabled={page >= totalPages - 1}
                >
                  Siguiente ›
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </Modal>
  )
}
