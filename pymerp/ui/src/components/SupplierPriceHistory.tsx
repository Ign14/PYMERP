import { useQuery } from "@tanstack/react-query";
import { listSuppliers, listProducts, getSupplierPriceHistory, Supplier, Product, SupplierPriceHistory as PriceHistoryType } from "../services/client";
import { useState, useMemo } from "react";

export default function SupplierPriceHistory() {
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>("");
  const [selectedProductId, setSelectedProductId] = useState<string>("");

  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ["suppliers"],
    queryFn: () => listSuppliers(),
    refetchOnWindowFocus: false,
  });

  const productsQuery = useQuery<Product[], Error>({
    queryKey: ["products"],
    queryFn: () => listProducts({ page: 0, size: 1000 }).then(p => p.content),
    refetchOnWindowFocus: false,
  });

  const priceHistoryQuery = useQuery<PriceHistoryType, Error>({
    queryKey: ["supplier-price-history", selectedSupplierId, selectedProductId],
    queryFn: () => getSupplierPriceHistory(selectedSupplierId, selectedProductId),
    enabled: !!selectedSupplierId && !!selectedProductId,
    refetchOnWindowFocus: false,
  });

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("es-CL", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const activeSuppliers = useMemo(() => {
    return (suppliersQuery.data ?? []).filter(s => s.active !== false);
  }, [suppliersQuery.data]);

  const getTrendIcon = (trend: string) => {
    if (trend === "UP") return "📈";
    if (trend === "DOWN") return "📉";
    return "➡️";
  };

  const getTrendColor = (trend: string) => {
    if (trend === "UP") return "text-red-400";
    if (trend === "DOWN") return "text-green-400";
    return "text-neutral-400";
  };

  const getTrendLabel = (trend: string, percentage: number) => {
    if (trend === "STABLE") return "Estable";
    const sign = percentage > 0 ? "+" : "";
    return `${sign}${percentage.toFixed(1)}%`;
  };

  const history = priceHistoryQuery.data;

  return (
    <div className="card-content">
      <h2 className="text-lg font-semibold text-neutral-100 mb-4">📊 Historial de Precios</h2>

      {/* Selectores */}
      <div className="grid grid-cols-2 gap-3 mb-4">
        <div>
          <label className="block text-xs text-neutral-400 mb-1">Proveedor</label>
          <select
            value={selectedSupplierId}
            onChange={(e) => setSelectedSupplierId(e.target.value)}
            className="w-full px-3 py-2 text-sm rounded bg-neutral-900 text-neutral-200 border border-neutral-700 focus:outline-none focus:border-blue-600"
          >
            <option value="">Seleccionar proveedor...</option>
            {activeSuppliers.map((supplier) => (
              <option key={supplier.id} value={supplier.id}>
                {supplier.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs text-neutral-400 mb-1">Producto</label>
          <select
            value={selectedProductId}
            onChange={(e) => setSelectedProductId(e.target.value)}
            className="w-full px-3 py-2 text-sm rounded bg-neutral-900 text-neutral-200 border border-neutral-700 focus:outline-none focus:border-blue-600"
            disabled={!selectedSupplierId}
          >
            <option value="">Seleccionar producto...</option>
            {(productsQuery.data ?? []).map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Estado de carga/error */}
      {priceHistoryQuery.isLoading && selectedSupplierId && selectedProductId && (
        <div className="text-center py-8">
          <p className="text-neutral-400">Cargando historial de precios...</p>
        </div>
      )}

      {priceHistoryQuery.isError && (
        <div className="text-center py-8">
          <p className="text-red-400">{priceHistoryQuery.error?.message ?? "Error al cargar historial"}</p>
        </div>
      )}

      {/* Sin selección */}
      {!selectedSupplierId || !selectedProductId ? (
        <div className="text-center py-12 bg-neutral-900/50 rounded border border-neutral-800">
          <p className="text-neutral-400">Selecciona un proveedor y producto para ver el historial de precios</p>
          <p className="text-xs text-neutral-500 mt-2">Analiza la evolución de precios en el último año</p>
        </div>
      ) : null}

      {/* Datos cargados */}
      {history && !priceHistoryQuery.isLoading && (
        <>
          {/* Estadísticas */}
          <div className="grid grid-cols-4 gap-3 mb-4">
            <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
              <div className="text-xs text-neutral-500 mb-1">Precio Actual</div>
              <div className="text-lg font-bold text-blue-400">
                {formatCurrency(history.currentPrice)}
              </div>
            </div>
            <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
              <div className="text-xs text-neutral-500 mb-1">Precio Promedio</div>
              <div className="text-lg font-bold text-neutral-200">
                {formatCurrency(history.averagePrice)}
              </div>
            </div>
            <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
              <div className="text-xs text-neutral-500 mb-1">Mínimo</div>
              <div className="text-lg font-bold text-green-400">
                {formatCurrency(history.minPrice)}
              </div>
            </div>
            <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
              <div className="text-xs text-neutral-500 mb-1">Máximo</div>
              <div className="text-lg font-bold text-red-400">
                {formatCurrency(history.maxPrice)}
              </div>
            </div>
          </div>

          {/* Tendencia */}
          <div className={`p-3 rounded border mb-4 ${
            history.trend === "UP" 
              ? "bg-red-950/30 border-red-800/50" 
              : history.trend === "DOWN"
                ? "bg-green-950/30 border-green-800/50"
                : "bg-neutral-900/50 border-neutral-800"
          }`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-2xl">{getTrendIcon(history.trend)}</span>
                <div>
                  <div className="text-sm font-semibold text-neutral-200">
                    Tendencia: {history.trend === "UP" ? "Al alza" : history.trend === "DOWN" ? "A la baja" : "Estable"}
                  </div>
                  <div className="text-xs text-neutral-500">
                    Últimos 3 meses vs 3 meses anteriores
                  </div>
                </div>
              </div>
              <div className={`text-xl font-bold ${getTrendColor(history.trend)}`}>
                {getTrendLabel(history.trend, history.trendPercentage)}
              </div>
            </div>
            {history.trend === "UP" && history.trendPercentage > 10 && (
              <div className="mt-2 pt-2 border-t border-red-800/30">
                <p className="text-xs text-red-300">
                  ⚠️ Precio ha aumentado más del 10% en los últimos 3 meses. Considere negociar o buscar alternativas.
                </p>
              </div>
            )}
          </div>

          {/* Historial de compras */}
          {history.priceHistory.length > 0 ? (
            <div>
              <h3 className="text-sm font-semibold text-neutral-300 mb-2">Historial de Compras (Último Año)</h3>
              <div className="max-h-64 overflow-y-auto border border-neutral-800 rounded">
                <table className="w-full text-xs">
                  <thead className="sticky top-0 bg-neutral-900 border-b border-neutral-800">
                    <tr>
                      <th className="text-left py-2 px-3 text-neutral-400 font-medium">Fecha</th>
                      <th className="text-right py-2 px-3 text-neutral-400 font-medium">Precio Unit.</th>
                      <th className="text-right py-2 px-3 text-neutral-400 font-medium">Cantidad</th>
                      <th className="text-right py-2 px-3 text-neutral-400 font-medium">Total</th>
                      <th className="text-right py-2 px-3 text-neutral-400 font-medium">vs Promedio</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.priceHistory.map((point, idx) => {
                      const total = point.unitPrice * point.quantity;
                      const vsAvg = history.averagePrice > 0 
                        ? ((point.unitPrice - history.averagePrice) / history.averagePrice) * 100 
                        : 0;
                      const isAboveAvg = vsAvg > 0;

                      return (
                        <tr 
                          key={idx}
                          className="border-b border-neutral-800/50 hover:bg-neutral-900/30"
                        >
                          <td className="py-2 px-3 text-neutral-300">{formatDate(point.date)}</td>
                          <td className="py-2 px-3 text-right font-medium text-neutral-200">
                            {formatCurrency(point.unitPrice)}
                          </td>
                          <td className="py-2 px-3 text-right text-neutral-400">
                            {point.quantity.toLocaleString("es-CL")}
                          </td>
                          <td className="py-2 px-3 text-right font-medium text-neutral-200">
                            {formatCurrency(total)}
                          </td>
                          <td className={`py-2 px-3 text-right text-xs ${
                            isAboveAvg ? "text-red-400" : "text-green-400"
                          }`}>
                            {vsAvg > 0 ? "+" : ""}{vsAvg.toFixed(1)}%
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <div className="text-xs text-neutral-500 mt-2">
                Total de compras en el período: {history.priceHistory.length}
              </div>
            </div>
          ) : (
            <div className="text-center py-8 bg-neutral-900/50 rounded border border-neutral-800">
              <p className="text-neutral-400">No hay historial de compras para este producto con este proveedor</p>
              <p className="text-xs text-neutral-500 mt-2">Los datos se mostrarán después de realizar compras</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
