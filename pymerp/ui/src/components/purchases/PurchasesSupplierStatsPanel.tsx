import { useQuery } from '@tanstack/react-query'
import { listPurchases, listSuppliers } from '../../services/client'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts'
import { useMemo } from 'react'

type Props = {
  startDate?: string
  endDate?: string
}

type SupplierStats = {
  supplierId: string
  supplierName: string
  totalOrders: number
  totalAmount: number
  avgOrderAmount: number
  lastOrderDate: string | null
  daysSinceLastOrder: number
}

export function PurchasesSupplierStatsPanel({ startDate, endDate }: Props) {
  const suppliersQuery = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => listSuppliers(),
  })

  const purchasesQuery = useQuery({
    queryKey: ['purchases-all', startDate, endDate],
    queryFn: async () => {
      const params: any = {
        page: 0,
        size: 10000,
        from: startDate ? new Date(startDate).toISOString() : undefined,
        to: endDate ? new Date(endDate + 'T23:59:59').toISOString() : undefined,
      }
      return listPurchases(params)
    },
  })

  const supplierStats = useMemo<SupplierStats[]>(() => {
    if (!purchasesQuery.data || !suppliersQuery.data) return []

    const purchases = purchasesQuery.data.content
    const suppliers = suppliersQuery.data

    const statsMap = new Map<string, SupplierStats>()

    purchases.forEach(purchase => {
      if (!purchase.supplierId) return

      const supplier = suppliers.find(s => s.id === purchase.supplierId)
      const supplierName = supplier?.name ?? purchase.supplierName ?? 'Desconocido'

      if (!statsMap.has(purchase.supplierId)) {
        statsMap.set(purchase.supplierId, {
          supplierId: purchase.supplierId,
          supplierName,
          totalOrders: 0,
          totalAmount: 0,
          avgOrderAmount: 0,
          lastOrderDate: null,
          daysSinceLastOrder: 0,
        })
      }

      const stats = statsMap.get(purchase.supplierId)!
      stats.totalOrders++
      stats.totalAmount += parseFloat(purchase.total?.toString() ?? '0')

      const orderDate = purchase.issuedAt ? new Date(purchase.issuedAt).toISOString() : null
      if (orderDate && (!stats.lastOrderDate || orderDate > stats.lastOrderDate)) {
        stats.lastOrderDate = orderDate
      }
    })

    const now = new Date()
    const statsArray = Array.from(statsMap.values()).map(stat => ({
      ...stat,
      avgOrderAmount: stat.totalAmount / stat.totalOrders,
      daysSinceLastOrder: stat.lastOrderDate
        ? Math.floor(
            (now.getTime() - new Date(stat.lastOrderDate).getTime()) / (1000 * 60 * 60 * 24)
          )
        : 999,
    }))

    return statsArray.sort((a, b) => b.totalAmount - a.totalAmount).slice(0, 10)
  }, [purchasesQuery.data, suppliersQuery.data])

  if (purchasesQuery.isLoading || suppliersQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100">Top 10 Proveedores</h3>
        <p className="muted text-neutral-400">Cargando estadísticas...</p>
      </div>
    )
  }

  if (!supplierStats.length) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100">Top 10 Proveedores</h3>
        <p className="muted text-neutral-400">
          No hay datos de proveedores en el período seleccionado
        </p>
      </div>
    )
  }

  const chartData = supplierStats.map(stat => ({
    name:
      stat.supplierName.length > 20
        ? stat.supplierName.substring(0, 17) + '...'
        : stat.supplierName,
    fullName: stat.supplierName,
    total: stat.totalAmount,
    orders: stat.totalOrders,
    avg: stat.avgOrderAmount,
  }))

  const COLORS = [
    '#0088FE',
    '#00C49F',
    '#FFBB28',
    '#FF8042',
    '#8884d8',
    '#82ca9d',
    '#ffc658',
    '#ff7c7c',
    '#8dd1e1',
    '#d084d0',
  ]

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <h3 className="text-neutral-100">Top 10 Proveedores</h3>
      <p className="muted text-neutral-400" style={{ marginBottom: '1rem' }}>
        Volumen total de compras por proveedor (período: {startDate} a {endDate})
      </p>

      <div style={{ marginBottom: '2rem' }}>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
            <XAxis dataKey="name" angle={-45} textAnchor="end" height={100} stroke="#a3a3a3" />
            <YAxis stroke="#a3a3a3" />
            <Tooltip
              formatter={(value: number, name: string, props: any) => {
                if (name === 'total') return [`$${value.toLocaleString()}`, 'Total']
                if (name === 'orders') return [value, 'Órdenes']
                if (name === 'avg') return [`$${value.toLocaleString()}`, 'Promedio']
                return value
              }}
              labelFormatter={label => chartData.find(d => d.name === label)?.fullName ?? label}
              contentStyle={{ backgroundColor: '#262626', border: '1px solid #404040' }}
              labelStyle={{ color: '#f5f5f5' }}
            />
            <Bar dataKey="total" name="Total">
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>Proveedor</th>
              <th>Órdenes</th>
              <th>Total</th>
              <th>Promedio</th>
              <th>Última compra</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {supplierStats.map(stat => (
              <tr key={stat.supplierId}>
                <td>{stat.supplierName}</td>
                <td>{stat.totalOrders}</td>
                <td>${stat.totalAmount.toLocaleString()}</td>
                <td>${stat.avgOrderAmount.toLocaleString()}</td>
                <td>
                  {stat.lastOrderDate ? new Date(stat.lastOrderDate).toLocaleDateString() : 'N/A'}
                </td>
                <td>
                  {stat.daysSinceLastOrder < 30 ? (
                    <span className="status active">Activo</span>
                  ) : stat.daysSinceLastOrder < 90 ? (
                    <span className="status warning">Regular</span>
                  ) : (
                    <span className="status inactive">Inactivo</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ marginTop: '1rem' }}>
        <h4 className="text-neutral-100">Métricas Generales</h4>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="muted text-neutral-400">Total Proveedores</p>
            <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }} className="text-neutral-100">
              {supplierStats.length}
            </p>
          </div>
          <div>
            <p className="muted text-neutral-400">Total Órdenes</p>
            <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }} className="text-neutral-100">
              {supplierStats.reduce((sum, s) => sum + s.totalOrders, 0)}
            </p>
          </div>
          <div>
            <p className="muted text-neutral-400">Volumen Total</p>
            <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }} className="text-neutral-100">
              ${supplierStats.reduce((sum, s) => sum + s.totalAmount, 0).toLocaleString()}
            </p>
          </div>
          <div>
            <p className="muted text-neutral-400">Proveedores Activos</p>
            <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }} className="text-neutral-100">
              {supplierStats.filter(s => s.daysSinceLastOrder < 30).length}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
