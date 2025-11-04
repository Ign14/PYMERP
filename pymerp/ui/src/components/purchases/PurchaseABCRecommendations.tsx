import { useQuery } from '@tanstack/react-query'
import { getPurchaseABCAnalysis } from '../../services/client'

export default function PurchaseABCRecommendations() {
  const { data: classifications } = useQuery({
    queryKey: ['purchaseABCAnalysis'],
    queryFn: () => getPurchaseABCAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  })

  if (!classifications || classifications.length === 0) {
    return null
  }

  const classA = classifications.filter(c => c.classification === 'A')
  const classB = classifications.filter(c => c.classification === 'B')
  const classC = classifications.filter(c => c.classification === 'C')

  const totalSpent = classifications.reduce((sum, c) => sum + c.totalSpent, 0)
  const classASpent = classA.reduce((sum, c) => sum + c.totalSpent, 0)
  const classAPercentage = totalSpent > 0 ? (classASpent / totalSpent) * 100 : 0

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">
        💡 Insights y Estrategias por Clasificación
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Clase A */}
        <div className="bg-gradient-to-br from-red-900/20 to-red-800/10 border border-red-800 rounded-lg p-5">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-2xl">🔴</span>
            <div>
              <h4 className="text-red-400 font-semibold">Clase A - Proveedores Críticos</h4>
              <p className="text-xs text-red-300">
                {classA.length} proveedores · {classAPercentage.toFixed(1)}% del gasto
              </p>
            </div>
          </div>

          <div className="space-y-3 text-sm">
            <div className="bg-neutral-900/50 rounded-lg p-3">
              <p className="text-red-200 font-medium mb-2">🎯 Estrategia:</p>
              <ul className="text-neutral-300 space-y-1 text-xs">
                <li>• Negociar contratos a largo plazo</li>
                <li>• Reuniones trimestrales de revisión</li>
                <li>• Planes de contingencia definidos</li>
                <li>• Monitoreo continuo de desempeño</li>
              </ul>
            </div>

            <div className="bg-neutral-900/50 rounded-lg p-3">
              <p className="text-red-200 font-medium mb-2">⚠️ Riesgos:</p>
              <ul className="text-neutral-300 space-y-1 text-xs">
                <li>• Alta dependencia de pocos proveedores</li>
                <li>• Impacto severo si hay problemas</li>
                <li>• Poder de negociación limitado</li>
              </ul>
            </div>

            <div className="bg-amber-900/20 border border-amber-700 rounded p-2">
              <p className="text-amber-200 text-xs">
                ⚡ <strong>Acción urgente:</strong> Desarrollar proveedores alternativos para
                reducir riesgo
              </p>
            </div>
          </div>
        </div>

        {/* Clase B */}
        <div className="bg-gradient-to-br from-amber-900/20 to-amber-800/10 border border-amber-800 rounded-lg p-5">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-2xl">🟡</span>
            <div>
              <h4 className="text-amber-400 font-semibold">Clase B - Proveedores Importantes</h4>
              <p className="text-xs text-amber-300">
                {classB.length} proveedores · Equilibrio ideal
              </p>
            </div>
          </div>

          <div className="space-y-3 text-sm">
            <div className="bg-neutral-900/50 rounded-lg p-3">
              <p className="text-amber-200 font-medium mb-2">🎯 Estrategia:</p>
              <ul className="text-neutral-300 space-y-1 text-xs">
                <li>• Revisión semestral de contratos</li>
                <li>• Evaluar promoción a Clase A</li>
                <li>• Buscar alternativas competitivas</li>
                <li>• Optimizar costos y condiciones</li>
              </ul>
            </div>

            <div className="bg-neutral-900/50 rounded-lg p-3">
              <p className="text-amber-200 font-medium mb-2">📊 Oportunidades:</p>
              <ul className="text-neutral-300 space-y-1 text-xs">
                <li>• Consolidar compras para mejores precios</li>
                <li>• Negociar descuentos por volumen</li>
                <li>• Explorar servicios adicionales</li>
              </ul>
            </div>

            <div className="bg-blue-900/20 border border-blue-700 rounded p-2">
              <p className="text-blue-200 text-xs">
                💼 <strong>Recomendación:</strong> Mantener relación activa y evaluar potencial de
                crecimiento
              </p>
            </div>
          </div>
        </div>

        {/* Clase C */}
        <div className="bg-gradient-to-br from-green-900/20 to-green-800/10 border border-green-800 rounded-lg p-5">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-2xl">🟢</span>
            <div>
              <h4 className="text-green-400 font-semibold">Clase C - Proveedores Ocasionales</h4>
              <p className="text-xs text-green-300">
                {classC.length} proveedores · Bajo impacto individual
              </p>
            </div>
          </div>

          <div className="space-y-3 text-sm">
            <div className="bg-neutral-900/50 rounded-lg p-3">
              <p className="text-green-200 font-medium mb-2">🎯 Estrategia:</p>
              <ul className="text-neutral-300 space-y-1 text-xs">
                <li>• Consolidar compras similares</li>
                <li>• Evaluar eliminación de proveedores</li>
                <li>• Automatizar procesos de compra</li>
                <li>• Reducir carga administrativa</li>
              </ul>
            </div>

            <div className="bg-neutral-900/50 rounded-lg p-3">
              <p className="text-green-200 font-medium mb-2">⚡ Optimización:</p>
              <ul className="text-neutral-300 space-y-1 text-xs">
                <li>• Agrupar con proveedores Clase B</li>
                <li>• Compras esporádicas bajo demanda</li>
                <li>• Minimizar costos de gestión</li>
              </ul>
            </div>

            <div className="bg-purple-900/20 border border-purple-700 rounded p-2">
              <p className="text-purple-200 text-xs">
                🔄 <strong>Acción:</strong> Revisar anualmente y considerar consolidación con otros
                proveedores
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Resumen global */}
      <div className="mt-6 bg-gradient-to-r from-blue-900/20 to-purple-900/20 border border-blue-800 rounded-lg p-5">
        <h4 className="text-white font-semibold mb-3 flex items-center gap-2">
          <span>📈</span> Resumen del Análisis ABC
        </h4>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div>
            <p className="text-neutral-400 mb-1">Distribución de Proveedores</p>
            <p className="text-white">
              <span className="text-red-400 font-bold">{classA.length}</span> A ·{' '}
              <span className="text-amber-400 font-bold">{classB.length}</span> B ·{' '}
              <span className="text-green-400 font-bold">{classC.length}</span> C
            </p>
          </div>
          <div>
            <p className="text-neutral-400 mb-1">Principio de Pareto</p>
            <p className="text-white">
              {classA.length > 0 ? (
                <>
                  ~{((classA.length / classifications.length) * 100).toFixed(0)}% de proveedores
                  genera ~{classAPercentage.toFixed(0)}% del gasto
                </>
              ) : (
                'Distribución equilibrada'
              )}
            </p>
          </div>
          <div>
            <p className="text-neutral-400 mb-1">Recomendación Principal</p>
            <p className="text-blue-300">
              {classAPercentage > 85
                ? '⚠️ Reducir dependencia de Clase A'
                : classC.length > classA.length * 3
                  ? '🔄 Consolidar proveedores Clase C'
                  : '✅ Distribución saludable'}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
