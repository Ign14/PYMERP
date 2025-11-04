import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getABCAnalysis, ProductABCClassification } from '../services/client'

export default function ABCProductsTable() {
  const [selectedClass, setSelectedClass] = useState<string>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 10

  const {
    data: analysis,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['abcAnalysis'],
    queryFn: () => getABCAnalysis(),
    refetchInterval: 300000,
  })

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-6 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-12 bg-neutral-800 rounded"></div>
          ))}
        </div>
      </div>
    )
  }

  if (error || !analysis) {
    return (
      <div className="bg-red-900/20 border border-red-800 rounded-lg p-6 text-center">
        <p className="text-red-400">Error al cargar productos</p>
      </div>
    )
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value)
  }

  const formatDate = (dateString: string | null) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString('es-CL')
  }

  // Filtrar productos
  const filteredProducts = analysis.filter((product: ProductABCClassification) => {
    const matchesClass = selectedClass === 'all' || product.classification === selectedClass
    const matchesSearch =
      product.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.category.toLowerCase().includes(searchTerm.toLowerCase())
    return matchesClass && matchesSearch
  })

  // Paginación
  const totalPages = Math.ceil(filteredProducts.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const currentProducts = filteredProducts.slice(startIndex, endIndex)

  const getClassBadgeStyles = (classification: string) => {
    switch (classification) {
      case 'A':
        return 'bg-green-900/40 border-green-800 text-green-400'
      case 'B':
        return 'bg-yellow-900/40 border-yellow-800 text-yellow-400'
      case 'C':
        return 'bg-orange-900/40 border-orange-800 text-orange-400'
      default:
        return 'bg-neutral-800 border-neutral-700 text-neutral-400'
    }
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-neutral-100 mb-4">
          Productos por Clasificación ABC
        </h3>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-3 mb-4">
          {/* Search */}
          <div className="flex-1">
            <input
              type="text"
              placeholder="Buscar producto o categoría..."
              value={searchTerm}
              onChange={e => {
                setSearchTerm(e.target.value)
                setCurrentPage(1)
              }}
              className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-neutral-100 placeholder-neutral-500 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
            />
          </div>

          {/* Class Filter */}
          <div className="flex gap-2">
            <button
              onClick={() => {
                setSelectedClass('all')
                setCurrentPage(1)
              }}
              className={`px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
                selectedClass === 'all'
                  ? 'bg-blue-900/40 border-blue-800 text-blue-400'
                  : 'bg-neutral-800 border-neutral-700 text-neutral-400 hover:bg-neutral-700'
              }`}
            >
              Todos ({analysis.length})
            </button>
            <button
              onClick={() => {
                setSelectedClass('A')
                setCurrentPage(1)
              }}
              className={`px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
                selectedClass === 'A'
                  ? 'bg-green-900/40 border-green-800 text-green-400'
                  : 'bg-neutral-800 border-neutral-700 text-neutral-400 hover:bg-neutral-700'
              }`}
            >
              Clase A
            </button>
            <button
              onClick={() => {
                setSelectedClass('B')
                setCurrentPage(1)
              }}
              className={`px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
                selectedClass === 'B'
                  ? 'bg-yellow-900/40 border-yellow-800 text-yellow-400'
                  : 'bg-neutral-800 border-neutral-700 text-neutral-400 hover:bg-neutral-700'
              }`}
            >
              Clase B
            </button>
            <button
              onClick={() => {
                setSelectedClass('C')
                setCurrentPage(1)
              }}
              className={`px-3 py-2 rounded-lg text-xs font-medium border transition-colors ${
                selectedClass === 'C'
                  ? 'bg-orange-900/40 border-orange-800 text-orange-400'
                  : 'bg-neutral-800 border-neutral-700 text-neutral-400 hover:bg-neutral-700'
              }`}
            >
              Clase C
            </button>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="border-b border-neutral-800">
            <tr className="text-left text-neutral-400">
              <th className="pb-3 font-medium">Clase</th>
              <th className="pb-3 font-medium">Producto</th>
              <th className="pb-3 font-medium">Categoría</th>
              <th className="pb-3 font-medium text-right">Valor Total</th>
              <th className="pb-3 font-medium text-right">% del Total</th>
              <th className="pb-3 font-medium text-right">Cantidad</th>
              <th className="pb-3 font-medium text-right">Frecuencia</th>
              <th className="pb-3 font-medium text-right">Última Mov.</th>
            </tr>
          </thead>
          <tbody>
            {currentProducts.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-8 text-center text-neutral-500">
                  No se encontraron productos
                </td>
              </tr>
            ) : (
              currentProducts.map(product => (
                <tr
                  key={product.productId}
                  className="border-b border-neutral-800/50 hover:bg-neutral-800/30"
                >
                  <td className="py-3">
                    <span
                      className={`inline-flex px-2.5 py-1 rounded-md text-xs font-semibold border ${getClassBadgeStyles(product.classification)}`}
                    >
                      {product.classification}
                    </span>
                  </td>
                  <td className="py-3 text-neutral-100 font-medium">{product.productName}</td>
                  <td className="py-3 text-neutral-400">{product.category}</td>
                  <td className="py-3 text-right text-neutral-100 font-medium">
                    {formatCurrency(product.totalValue)}
                  </td>
                  <td className="py-3 text-right text-neutral-400">
                    {product.percentageOfTotalValue.toFixed(2)}%
                  </td>
                  <td className="py-3 text-right text-neutral-400">
                    {product.totalQuantity.toLocaleString('es-CL')}
                  </td>
                  <td className="py-3 text-right text-neutral-400">{product.salesFrequency}</td>
                  <td className="py-3 text-right text-neutral-400 text-xs">
                    {formatDate(product.lastMovementDate)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <div className="text-xs text-neutral-500">
            Mostrando {startIndex + 1}-{Math.min(endIndex, filteredProducts.length)} de{' '}
            {filteredProducts.length} productos
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
              disabled={currentPage === 1}
              className="px-3 py-1 bg-neutral-800 border border-neutral-700 rounded text-xs text-neutral-400 hover:bg-neutral-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <span className="px-3 py-1 text-xs text-neutral-400">
              Página {currentPage} de {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
              disabled={currentPage === totalPages}
              className="px-3 py-1 bg-neutral-800 border border-neutral-700 rounded text-xs text-neutral-400 hover:bg-neutral-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
