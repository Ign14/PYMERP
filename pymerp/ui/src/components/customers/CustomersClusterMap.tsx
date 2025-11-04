import { useMemo, useState } from "react";
import { Customer, CustomerStats } from "../../services/client";

type Props = {
  customers: Customer[];
  customerStats: Record<string, CustomerStats>;
  onCustomerSelect: (customer: Customer) => void;
  selectedCustomerId: string | null;
  getHealthStatus: (lastSaleDate?: string) => {
    status: string;
    label: string;
    color: string;
    icon: string;
  };
};

export default function CustomersClusterMap({
  customers,
  customerStats,
  onCustomerSelect,
  selectedCustomerId,
  getHealthStatus,
}: Props) {
  const [filterSegment, setFilterSegment] = useState<string | null>(null);
  const [filterHealth, setFilterHealth] = useState<string | null>(null);

  // Filtrar clientes que tienen coordenadas GPS
  const customersWithGPS = useMemo(() => {
    return customers.filter((customer) => {
      const hasGPS = customer.lat !== null && customer.lat !== undefined && 
                     customer.lng !== null && customer.lng !== undefined &&
                     `${customer.lat}`.trim() !== "" && `${customer.lng}`.trim() !== "";
      
      if (!hasGPS) return false;

      // Aplicar filtro de segmento
      if (filterSegment && customer.segment !== filterSegment) return false;

      // Aplicar filtro de salud
      if (filterHealth) {
        const stats = customerStats[customer.id];
        const health = getHealthStatus(stats?.lastSaleDate || undefined);
        if (health.status !== filterHealth) return false;
      }

      return true;
    });
  }, [customers, filterSegment, filterHealth, customerStats, getHealthStatus]);

  // Calcular centro del mapa basado en todos los clientes
  const mapCenter = useMemo(() => {
    if (customersWithGPS.length === 0) return { lat: -33.4489, lng: -70.6693 }; // Santiago por defecto

    const avgLat = customersWithGPS.reduce((sum, c) => sum + parseFloat(`${c.lat}`), 0) / customersWithGPS.length;
    const avgLng = customersWithGPS.reduce((sum, c) => sum + parseFloat(`${c.lng}`), 0) / customersWithGPS.length;

    return { lat: avgLat, lng: avgLng };
  }, [customersWithGPS]);

  // Obtener segmentos únicos
  const uniqueSegments = useMemo(() => {
    const segments = new Set(customers.map(c => c.segment).filter(Boolean));
    return Array.from(segments);
  }, [customers]);

  // URL del mapa con múltiples marcadores
  const mapUrl = useMemo(() => {
    if (customersWithGPS.length === 0) {
      return `https://www.google.com/maps?q=${mapCenter.lat},${mapCenter.lng}&output=embed&z=12`;
    }

    // Para múltiples marcadores, usamos la API de direcciones de Google Maps
    const markers = customersWithGPS
      .slice(0, 20) // Limitar a 20 para no exceder límites de URL
      .map(c => `${c.lat},${c.lng}`)
      .join('|');

    return `https://www.google.com/maps/embed/v1/view?key=YOUR_API_KEY&center=${mapCenter.lat},${mapCenter.lng}&zoom=12`;
  }, [customersWithGPS, mapCenter]);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-5">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-xl font-semibold text-neutral-100">
          🗺️ Mapa de Clientes ({customersWithGPS.length})
        </h3>
        <div className="flex gap-2">
          <select
            className="input bg-neutral-800 border-neutral-700 text-neutral-100 text-sm"
            value={filterSegment || ""}
            onChange={(e) => setFilterSegment(e.target.value || null)}
          >
            <option value="">Todos los segmentos</option>
            {uniqueSegments.map((seg) => (
              <option key={seg} value={seg}>
                {seg}
              </option>
            ))}
          </select>
          <select
            className="input bg-neutral-800 border-neutral-700 text-neutral-100 text-sm"
            value={filterHealth || ""}
            onChange={(e) => setFilterHealth(e.target.value || null)}
          >
            <option value="">Todos los estados</option>
            <option value="healthy">🟢 Saludables</option>
            <option value="at-risk">🟡 En riesgo</option>
            <option value="inactive">🔴 Inactivos</option>
          </select>
        </div>
      </div>

      {customersWithGPS.length === 0 ? (
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
          <p className="text-neutral-400">
            No hay clientes con coordenadas GPS disponibles
          </p>
        </div>
      ) : (
        <>
          <div className="bg-neutral-800 border border-neutral-700 rounded-lg overflow-hidden mb-4" style={{ height: "500px" }}>
            <iframe
              title="Mapa de clientes"
              src={mapUrl}
              width="100%"
              height="100%"
              loading="lazy"
              allowFullScreen
              style={{ border: 0 }}
            />
          </div>

          {/* Lista de clientes con GPS */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 max-h-60 overflow-y-auto">
            {customersWithGPS.map((customer) => {
              const stats = customerStats[customer.id];
              const health = getHealthStatus(stats?.lastSaleDate || undefined);
              const isSelected = selectedCustomerId === customer.id;

              return (
                <button
                  key={customer.id}
                  className={`bg-neutral-800 hover:bg-neutral-700 border rounded-lg p-3 text-left transition-all ${
                    isSelected ? "border-blue-500 ring-2 ring-blue-500" : "border-neutral-700"
                  }`}
                  onClick={() => onCustomerSelect(customer)}
                >
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-lg">{health.icon}</span>
                    <strong className="text-neutral-100 text-sm">{customer.name}</strong>
                  </div>
                  {customer.segment && (
                    <p className="text-xs text-neutral-400 mb-1">{customer.segment}</p>
                  )}
                  <p className="text-xs font-mono text-neutral-500">
                    📍 {parseFloat(`${customer.lat}`).toFixed(4)}, {parseFloat(`${customer.lng}`).toFixed(4)}
                  </p>
                  {stats && (
                    <p className="text-xs text-neutral-400 mt-1">
                      {stats.totalSales || 0} ventas • ${(stats.totalRevenue || 0).toLocaleString("es-CL")}
                    </p>
                  )}
                </button>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
