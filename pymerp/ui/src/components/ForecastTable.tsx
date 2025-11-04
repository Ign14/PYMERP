import { useQuery } from "@tanstack/react-query";
import { getForecastAnalysis } from "../services/client";
import { useState } from "react";

export default function ForecastTable() {
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [trendFilter, setTrendFilter] = useState<string>("all");
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const { data: forecasts, isLoading, error } = useQuery({
    queryKey: ["forecastAnalysis"],
    queryFn: () => getForecastAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="h-96 bg-neutral-800 rounded"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <p className="text-red-400">Error al cargar pronósticos</p>
      </div>
    );
  }

  if (!forecasts || forecasts.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <p className="text-neutral-400">No hay pronósticos disponibles</p>
      </div>
    );
  }

  // Filtrar datos
  const filteredData = forecasts.filter(f => {
    const matchesStatus = statusFilter === "all" || f.stockStatus === statusFilter;
    const matchesTrend = trendFilter === "all" || f.trend === trendFilter;
    const matchesSearch = 
      f.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      f.category.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesStatus && matchesTrend && matchesSearch;
  });

  // Paginación
  const totalPages = Math.ceil(filteredData.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const paginatedData = filteredData.slice(startIndex, startIndex + itemsPerPage);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "understocked":
        return <span className="px-2 py-1 text-xs font-semibold rounded-full bg-red-900/40 border border-red-800 text-red-400">Stock Bajo</span>;
      case "overstocked":
        return <span className="px-2 py-1 text-xs font-semibold rounded-full bg-orange-900/40 border border-orange-800 text-orange-400">Sobre-stock</span>;
      case "optimal":
        return <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-900/40 border border-green-800 text-green-400">Óptimo</span>;
      default:
        return <span className="px-2 py-1 text-xs font-semibold rounded-full bg-neutral-800 border border-neutral-700 text-neutral-400">{status}</span>;
    }
  };

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case "increasing":
        return <span className="text-green-400">↗️ Creciente</span>;
      case "decreasing":
        return <span className="text-red-400">↘️ Decreciente</span>;
      case "stable":
        return <span className="text-blue-400">→ Estable</span>;
      default:
        return <span className="text-neutral-400">{trend}</span>;
    }
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 space-y-4">
      <h3 className="text-xl font-semibold text-white">🎯 Tabla de Pronósticos</h3>

      {/* Filtros */}
      <div className="flex flex-col md:flex-row gap-4">
        {/* Filtro por Estado */}
        <div className="flex gap-2">
          <button
            onClick={() => setStatusFilter("all")}
            className={`px-3 py-1.5 text-sm rounded-lg border transition ${
              statusFilter === "all"
                ? "bg-blue-600 border-blue-500 text-white"
                : "bg-neutral-800 border-neutral-700 text-neutral-300 hover:bg-neutral-700"
            }`}
          >
            Todos
          </button>
          <button
            onClick={() => setStatusFilter("understocked")}
            className={`px-3 py-1.5 text-sm rounded-lg border transition ${
              statusFilter === "understocked"
                ? "bg-red-600 border-red-500 text-white"
                : "bg-neutral-800 border-neutral-700 text-neutral-300 hover:bg-neutral-700"
            }`}
          >
            Stock Bajo
          </button>
          <button
            onClick={() => setStatusFilter("optimal")}
            className={`px-3 py-1.5 text-sm rounded-lg border transition ${
              statusFilter === "optimal"
                ? "bg-green-600 border-green-500 text-white"
                : "bg-neutral-800 border-neutral-700 text-neutral-300 hover:bg-neutral-700"
            }`}
          >
            Óptimo
          </button>
          <button
            onClick={() => setStatusFilter("overstocked")}
            className={`px-3 py-1.5 text-sm rounded-lg border transition ${
              statusFilter === "overstocked"
                ? "bg-orange-600 border-orange-500 text-white"
                : "bg-neutral-800 border-neutral-700 text-neutral-300 hover:bg-neutral-700"
            }`}
          >
            Sobre-stock
          </button>
        </div>

        {/* Buscador */}
        <input
          type="text"
          placeholder="Buscar producto o categoría..."
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setCurrentPage(1);
          }}
          className="flex-1 px-4 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-white placeholder-neutral-500 focus:outline-none focus:border-blue-500"
        />
      </div>

      {/* Tabla */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-neutral-800">
              <th className="text-left p-3 text-sm font-semibold text-neutral-400">Estado</th>
              <th className="text-left p-3 text-sm font-semibold text-neutral-400">Producto</th>
              <th className="text-left p-3 text-sm font-semibold text-neutral-400">Categoría</th>
              <th className="text-right p-3 text-sm font-semibold text-neutral-400">Stock Actual</th>
              <th className="text-right p-3 text-sm font-semibold text-neutral-400">Demanda (30d)</th>
              <th className="text-right p-3 text-sm font-semibold text-neutral-400">Días Stock</th>
              <th className="text-center p-3 text-sm font-semibold text-neutral-400">Tendencia</th>
              <th className="text-right p-3 text-sm font-semibold text-neutral-400">Orden Recomendada</th>
              <th className="text-right p-3 text-sm font-semibold text-neutral-400">Confianza</th>
            </tr>
          </thead>
          <tbody>
            {paginatedData.map((forecast, index) => (
              <tr key={index} className="border-b border-neutral-800 hover:bg-neutral-800/30 transition">
                <td className="p-3">{getStatusBadge(forecast.stockStatus)}</td>
                <td className="p-3 text-white font-medium">{forecast.productName}</td>
                <td className="p-3 text-neutral-300">{forecast.category}</td>
                <td className="p-3 text-right text-white">{forecast.currentStock.toFixed(0)}</td>
                <td className="p-3 text-right text-blue-400 font-semibold">{forecast.predictedDemand.toFixed(0)}</td>
                <td className="p-3 text-right">
                  <span className={
                    forecast.daysOfStock < 15 
                      ? "text-red-400 font-semibold" 
                      : forecast.daysOfStock > 60 
                      ? "text-orange-400" 
                      : "text-green-400"
                  }>
                    {forecast.daysOfStock} días
                  </span>
                </td>
                <td className="p-3 text-center text-sm">{getTrendIcon(forecast.trend)}</td>
                <td className="p-3 text-right">
                  {forecast.recommendedOrderQty > 0 ? (
                    <span className="text-yellow-400 font-semibold">{forecast.recommendedOrderQty.toFixed(0)}</span>
                  ) : (
                    <span className="text-neutral-500">-</span>
                  )}
                </td>
                <td className="p-3 text-right text-neutral-400">{forecast.confidence.toFixed(0)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Paginación */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-4">
          <div className="text-sm text-neutral-400">
            Mostrando {startIndex + 1}-{Math.min(startIndex + itemsPerPage, filteredData.length)} de {filteredData.length}
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-3 py-1.5 text-sm bg-neutral-800 border border-neutral-700 rounded-lg text-neutral-300 hover:bg-neutral-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <span className="px-3 py-1.5 text-sm text-neutral-300">
              Página {currentPage} de {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="px-3 py-1.5 text-sm bg-neutral-800 border border-neutral-700 rounded-lg text-neutral-300 hover:bg-neutral-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}

      {filteredData.length === 0 && (
        <div className="text-center py-8 text-neutral-400">
          No se encontraron pronósticos con los filtros aplicados
        </div>
      )}
    </div>
  );
}
