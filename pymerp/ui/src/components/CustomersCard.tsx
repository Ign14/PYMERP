import {
  KeyboardEvent,
  MouseEvent,
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
} from "react";
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import CustomersTableView from "./customers/CustomersTableView";
import {
  deleteCustomer,
  listCustomers,
  getCustomerStats,
  getCustomerSaleHistory,
  Customer,
  CustomerStats,
  CustomerSaleHistoryItem,
  Page,
} from "../services/client";
import { computeNextPageParam, mergeCustomerPages } from "./customersUtils";

type Props = {
  segmentFilter?: string | null;
  segmentLabel?: string | null;
  onClearSegment?: () => void;
  onOpenCreateDialog?: () => void;
  onOpenEditDialog?: (customer: Customer) => void;
};

export type CustomersCardHandle = {
  focusCreate: () => void;
  clearForm: () => void;
  getFilters: () => { query: string; segment: string | null; active: boolean | null };
};

const PAGE_SIZE = 20;
const SORT_ORDER = "createdAt,desc";

const CustomersCard = forwardRef<CustomersCardHandle, Props>((props, ref) => {
  const { segmentFilter, segmentLabel, onClearSegment, onOpenCreateDialog, onOpenEditDialog } = props;
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState<boolean | null>(null);
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null);
  const [showDetailsPanel, setShowDetailsPanel] = useState(false);
  const [viewMode, setViewMode] = useState<"list" | "table">("list");
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [advancedFilters, setAdvancedFilters] = useState({
    minRevenue: "",
    maxRevenue: "",
    lastPurchaseDays: "",
    hasAddress: false,
    hasGPS: false,
  });
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const pagesLoggedRef = useRef(0);

  useImperativeHandle(
    ref,
    () => ({
      focusCreate: () => {
        onOpenCreateDialog?.();
      },
      clearForm: () => {
        // Ya no es necesario
      },
      getFilters: () => ({
        query,
        segment: segmentFilter ?? null,
        active: activeFilter,
      }),
    }),
    [onOpenCreateDialog, query, segmentFilter, activeFilter]
  );

  useEffect(() => {
    pagesLoggedRef.current = 0;
  }, [query, segmentFilter, activeFilter]);

  const fetchPage = useCallback(
    async (pageParam: number) => {
      const trimmed = query.trim();
      const start = performance.now();
      const response = await listCustomers({
        q: trimmed.length ? trimmed : undefined,
        segment: segmentFilter ?? undefined,
        active: activeFilter ?? undefined,
        page: pageParam,
        size: PAGE_SIZE,
        sort: SORT_ORDER,
      });
      const elapsed = Math.round(performance.now() - start);
      const count = response.content?.length ?? 0;
      console.info(`[Customers] page ${pageParam} fetched (${count} items) in ${elapsed}ms`);
      return response;
    },
    [query, segmentFilter, activeFilter]
  );

  const customersQuery = useInfiniteQuery({
    queryKey: ["customers", query, segmentFilter ?? null, activeFilter ?? null],
    queryFn: ({ pageParam = 0 }) => fetchPage(pageParam),
    getNextPageParam: (lastPage) => computeNextPageParam(lastPage),
    initialPageParam: 0,
  });

  const { fetchNextPage, isFetchingNextPage, refetch: refetchCustomers } = customersQuery;
  const flattenedCustomers = useMemo(() => {
    return mergeCustomerPages(customersQuery.data?.pages ?? []);
  }, [customersQuery.data]);

  // Obtener stats para todos los clientes visibles (solo los primeros 20 para performance)
  const customerIds = useMemo(() => {
    return flattenedCustomers.slice(0, 20).map(c => c.id);
  }, [flattenedCustomers]);

  const allStatsQuery = useQuery({
    queryKey: ["customers", "batch-stats", customerIds],
    queryFn: async () => {
      // Obtener stats para cada cliente
      const statsPromises = customerIds.map(id => 
        getCustomerStats(id).catch(() => null)
      );
      const stats = await Promise.all(statsPromises);
      return customerIds.reduce((acc, id, index) => {
        const stat = stats[index];
        if (stat) {
          acc[id] = stat;
        }
        return acc;
      }, {} as Record<string, CustomerStats>);
    },
    enabled: customerIds.length > 0,
    staleTime: 60_000,
  });

  useEffect(() => {
    if (flattenedCustomers.length === 0) {
      setSelectedCustomerId(null);
      return;
    }
    setSelectedCustomerId((prev) => {
      if (prev && flattenedCustomers.some((customer) => customer.id === prev)) {
        return prev;
      }
      return flattenedCustomers[0]?.id ?? null;
    });
  }, [flattenedCustomers]);

  // Aplicar filtros avanzados
  const filteredCustomers = useMemo(() => {
    if (!advancedFilters.minRevenue && !advancedFilters.maxRevenue && !advancedFilters.lastPurchaseDays && !advancedFilters.hasAddress && !advancedFilters.hasGPS) {
      return flattenedCustomers;
    }

    return flattenedCustomers.filter((customer) => {
      const stats = allStatsQuery.data?.[customer.id];

      // Filtro de ingresos mínimos
      if (advancedFilters.minRevenue) {
        const minRev = parseFloat(advancedFilters.minRevenue);
        if (!stats || (stats.totalRevenue || 0) < minRev) return false;
      }

      // Filtro de ingresos máximos
      if (advancedFilters.maxRevenue) {
        const maxRev = parseFloat(advancedFilters.maxRevenue);
        if (!stats || (stats.totalRevenue || 0) > maxRev) return false;
      }

      // Filtro de días desde última compra
      if (advancedFilters.lastPurchaseDays) {
        const maxDays = parseInt(advancedFilters.lastPurchaseDays);
        if (!stats?.lastSaleDate) return false;
        const daysSinceLastSale = Math.floor((new Date().getTime() - new Date(stats.lastSaleDate).getTime()) / (1000 * 60 * 60 * 24));
        if (daysSinceLastSale > maxDays) return false;
      }

      // Filtro de dirección
      if (advancedFilters.hasAddress && !customer.address) return false;

      // Filtro de GPS
      if (advancedFilters.hasGPS && (!customer.lat || !customer.lng)) return false;

      return true;
    });
  }, [flattenedCustomers, allStatsQuery.data, advancedFilters]);

  const selectedCustomer = useMemo(() => {
    if (!selectedCustomerId) return null;
    return filteredCustomers.find((customer) => customer.id === selectedCustomerId) ?? null;
  }, [filteredCustomers, selectedCustomerId]);

  const mapQuery = useMemo(() => {
    if (!selectedCustomer) return null;
    if (
      selectedCustomer.lat !== undefined &&
      selectedCustomer.lat !== null &&
      `${selectedCustomer.lat}`.trim() !== "" &&
      selectedCustomer.lng !== undefined &&
      selectedCustomer.lng !== null &&
      `${selectedCustomer.lng}`.trim() !== ""
    ) {
      return `${selectedCustomer.lat},${selectedCustomer.lng}`;
    }
    if (selectedCustomer.address && selectedCustomer.address.trim()) {
      return selectedCustomer.address.trim();
    }
    return null;
  }, [selectedCustomer]);

  const mapEmbedUrl = useMemo(() => {
    return mapQuery ? `https://www.google.com/maps?q=${encodeURIComponent(mapQuery)}&output=embed` : null;
  }, [mapQuery]);

  const mapExternalUrl = useMemo(() => {
    return mapQuery ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(mapQuery)}` : null;
  }, [mapQuery]);

  const hasNextPage = customersQuery.hasNextPage ?? false;
  const isInitialLoading = customersQuery.isLoading && !isFetchingNextPage;

  useEffect(() => {
    const totalPagesFetched = customersQuery.data?.pages.length ?? 0;
    if (totalPagesFetched > pagesLoggedRef.current) {
      pagesLoggedRef.current = totalPagesFetched;
      console.info(`[Customers] pages fetched so far: ${totalPagesFetched}`);
    }
  }, [customersQuery.data]);

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target) return;
    const observer = new IntersectionObserver((entries) => {
      const entry = entries[0];
      if (entry.isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    });
    observer.observe(target);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCustomer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers"], exact: false });
    },
  });

  // Customer stats and sales history queries
  const statsQuery = useQuery<CustomerStats, Error>({
    queryKey: ["customers", selectedCustomerId, "stats"],
    queryFn: () => getCustomerStats(selectedCustomerId!),
    enabled: !!selectedCustomerId && showDetailsPanel,
    staleTime: 30_000,
  });

  const salesHistoryQuery = useQuery<Page<CustomerSaleHistoryItem>, Error>({
    queryKey: ["customers", selectedCustomerId, "sales"],
    queryFn: () => getCustomerSaleHistory(selectedCustomerId!, 0, 5),
    enabled: !!selectedCustomerId && showDetailsPanel,
    staleTime: 30_000,
  });

  // Función para calcular el estado de salud del cliente
    const getCustomerHealthStatus = (lastSaleDate?: string) => {
    if (!lastSaleDate) {
      return { status: "inactive", label: "Sin ventas", color: "bg-neutral-700 text-neutral-400 border-neutral-600", icon: "⚪" };
    }
    const daysSinceLastSale = Math.floor((new Date().getTime() - new Date(lastSaleDate).getTime()) / (1000 * 60 * 60 * 24));
    if (daysSinceLastSale <= 30) {
      return { status: "healthy", label: "Saludable", color: "bg-green-950 text-green-400 border-green-800", icon: "🟢" };
    }
    if (daysSinceLastSale <= 90) {
      return { status: "at-risk", label: "En riesgo", color: "bg-yellow-950 text-yellow-400 border-yellow-800", icon: "🟡" };
    } else {
      return { status: "inactive", label: "Inactivo", color: "bg-red-950 text-red-400 border-red-800", icon: "🔴" };
    }
  };

  // Calcular scoring RFM (Recency, Frequency, Monetary)
  const calculateRFM = (stats?: CustomerStats) => {
    if (!stats) return { 
      recency: 0, 
      frequency: 0, 
      monetary: 0, 
      recencyScore: 0,
      frequencyScore: 0,
      monetaryScore: 0,
      score: "000", 
      segment: "Sin datos" 
    };

    // Recency: días desde última compra (menor es mejor)
    const recency = stats.lastSaleDate
      ? Math.floor((new Date().getTime() - new Date(stats.lastSaleDate).getTime()) / (1000 * 60 * 60 * 24))
      : 999;
    const recencyScore = recency <= 30 ? 5 : recency <= 60 ? 4 : recency <= 90 ? 3 : recency <= 180 ? 2 : 1;

    // Frequency: número de compras (mayor es mejor)
    const frequency = stats.totalSales || 0;
    const frequencyScore = frequency >= 20 ? 5 : frequency >= 10 ? 4 : frequency >= 5 ? 3 : frequency >= 2 ? 2 : 1;

    // Monetary: ingresos totales (mayor es mejor)
    const monetary = stats.totalRevenue || 0;
    const monetaryScore = monetary >= 5000000 ? 5 : monetary >= 2000000 ? 4 : monetary >= 1000000 ? 3 : monetary >= 500000 ? 2 : 1;

    const score = `${recencyScore}${frequencyScore}${monetaryScore}`;

    // Segmentación automática basada en RFM
    let segment = "Sin clasificar";
    if (recencyScore >= 4 && frequencyScore >= 4 && monetaryScore >= 4) {
      segment = "🏆 Campeón";
    } else if (recencyScore >= 4 && monetaryScore >= 4) {
      segment = "💎 Leal";
    } else if (recencyScore >= 4) {
      segment = "⭐ Potencial";
    } else if (frequencyScore >= 4 || monetaryScore >= 4) {
      segment = "🔄 Necesita atención";
    } else if (recencyScore <= 2) {
      segment = "⚠️ En riesgo";
    } else if (recencyScore === 1) {
      segment = "💤 Dormido";
    }

    return { recency, frequency, monetary, recencyScore, frequencyScore, monetaryScore, score, segment };
  };

  const errorMessage = customersQuery.isError
    ? (customersQuery.error as Error | { message?: string })?.message ?? "No se pudieron cargar los clientes"
    : undefined;
  const showEmptyState = !isInitialLoading && !customersQuery.isError && filteredCustomers.length === 0;

  return (
    <div>
      <div className="card-header border-b border-neutral-800 pb-4 mb-4">
        <h2 className="text-neutral-100">Clientes</h2>
        <div className="flex gap-2 items-center flex-1">
          <input
            className="input bg-neutral-800 border-neutral-700 text-neutral-100 flex-1"
            placeholder="Buscar por nombre, email, teléfono o RUT"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="flex gap-1 bg-neutral-800 border border-neutral-700 rounded-lg p-1">
            <button
              className={`px-3 py-1 rounded text-sm ${viewMode === "list" ? "bg-neutral-700 text-neutral-100" : "text-neutral-400 hover:text-neutral-100"}`}
              onClick={() => setViewMode("list")}
              title="Vista de lista"
            >
              📋 Lista
            </button>
            <button
              className={`px-3 py-1 rounded text-sm ${viewMode === "table" ? "bg-neutral-700 text-neutral-100" : "text-neutral-400 hover:text-neutral-100"}`}
              onClick={() => setViewMode("table")}
              title="Vista de tabla"
            >
              📊 Tabla
            </button>
          </div>
        </div>
        <button
          className="btn ghost text-sm text-neutral-400 hover:text-neutral-100"
          onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
        >
          {showAdvancedFilters ? "🔼" : "🔽"} Filtros avanzados
        </button>
      </div>

      {showAdvancedFilters && (
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 mb-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-300 mb-2">
                Ingresos mínimos ($)
              </label>
              <input
                type="number"
                className="input bg-neutral-900 border-neutral-700 text-neutral-100 w-full"
                placeholder="Ej: 100000"
                value={advancedFilters.minRevenue}
                onChange={(e) => setAdvancedFilters(prev => ({ ...prev, minRevenue: e.target.value }))}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-300 mb-2">
                Ingresos máximos ($)
              </label>
              <input
                type="number"
                className="input bg-neutral-900 border-neutral-700 text-neutral-100 w-full"
                placeholder="Ej: 5000000"
                value={advancedFilters.maxRevenue}
                onChange={(e) => setAdvancedFilters(prev => ({ ...prev, maxRevenue: e.target.value }))}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-300 mb-2">
                Última compra (máx días)
              </label>
              <input
                type="number"
                className="input bg-neutral-900 border-neutral-700 text-neutral-100 w-full"
                placeholder="Ej: 90"
                value={advancedFilters.lastPurchaseDays}
                onChange={(e) => setAdvancedFilters(prev => ({ ...prev, lastPurchaseDays: e.target.value }))}
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="hasAddress"
                checked={advancedFilters.hasAddress}
                onChange={(e) => setAdvancedFilters(prev => ({ ...prev, hasAddress: e.target.checked }))}
                className="w-4 h-4 text-blue-600 bg-neutral-900 border-neutral-700 rounded focus:ring-blue-500"
              />
              <label htmlFor="hasAddress" className="text-sm text-neutral-300">
                Solo con dirección
              </label>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="hasGPS"
                checked={advancedFilters.hasGPS}
                onChange={(e) => setAdvancedFilters(prev => ({ ...prev, hasGPS: e.target.checked }))}
                className="w-4 h-4 text-blue-600 bg-neutral-900 border-neutral-700 rounded focus:ring-blue-500"
              />
              <label htmlFor="hasGPS" className="text-sm text-neutral-300">
                Solo con coordenadas GPS
              </label>
            </div>
            <div className="flex items-center gap-2">
              <button
                className="btn ghost text-sm"
                onClick={() => setAdvancedFilters({
                  minRevenue: "",
                  maxRevenue: "",
                  lastPurchaseDays: "",
                  hasAddress: false,
                  hasGPS: false,
                })}
              >
                Limpiar filtros
              </button>
            </div>
          </div>
        </div>
      )}

      {segmentLabel && (
        <div className="active-filter bg-neutral-800 border border-neutral-700 rounded-lg">
          <span className="text-neutral-100">Filtrado por segmento: {segmentLabel}</span>
          {onClearSegment && (
            <button className="btn ghost" type="button" onClick={onClearSegment}>
              Limpiar
            </button>
          )}
        </div>
      )}

      {isInitialLoading && <p className="text-neutral-400">Loading...</p>}
      {!isInitialLoading && customersQuery.isError && flattenedCustomers.length === 0 && (
        <div className="error bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">{errorMessage}</p>
          <button className="btn ghost" type="button" onClick={() => refetchCustomers()}>
            Reintentar
          </button>
        </div>
      )}

      {viewMode === "table" ? (
        <CustomersTableView
          customers={filteredCustomers}
          customerStats={allStatsQuery.data || {}}
          onCustomerSelect={(customer) => setSelectedCustomerId(customer.id)}
          onCustomerEdit={(customer) => onOpenEditDialog?.(customer)}
          onCustomerDelete={(customerId) => deleteMutation.mutate(customerId)}
          selectedCustomerId={selectedCustomerId}
          getHealthStatus={getCustomerHealthStatus}
          calculateRFM={calculateRFM}
        />
      ) : (
        !customersQuery.isError && filteredCustomers.length > 0 && (
          <ul className="list" aria-live="polite">
            {filteredCustomers.map((customer: Customer) => {
              const isSelected = selectedCustomerId === customer.id;
              const customerStats = allStatsQuery.data?.[customer.id];
              const healthStatus = getCustomerHealthStatus(customerStats?.lastSaleDate || undefined);
              
              return (
                <li
                  key={customer.id}
                  className={`list-row bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 rounded-lg${isSelected ? " selected ring-2 ring-blue-500" : ""}`}
                  onClick={() => setSelectedCustomerId(customer.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(event: KeyboardEvent<HTMLLIElement>) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      setSelectedCustomerId(customer.id);
                    }
                  }}
                >
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <strong className="text-neutral-100">{customer.name}</strong>
                      {customerStats && (
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium border ${healthStatus.color}`}>
                          <span>{healthStatus.icon}</span>
                          <span>{healthStatus.label}</span>
                        </span>
                      )}
                    </div>
                    {customer.rut ? <div className="mono small text-neutral-400">RUT: {customer.rut}</div> : null}
                    {customer.segment ? <div className="mono small text-neutral-300">{customer.segment}</div> : null}
                    {customer.email ? <div className="mono small text-neutral-400">{customer.email}</div> : null}
                    {customer.phone ? <div className="mono small text-neutral-400">{customer.phone}</div> : null}
                    {customer.address ? <div className="mono small text-neutral-400">{customer.address}</div> : null}
                    {customer.active === false ? (
                      <div className="badge bg-neutral-700 text-neutral-300 border border-neutral-600">Inactivo</div>
                    ) : null}
                  </div>
                  <div style={{ display: "flex", gap: "0.5rem" }}>
                    <button
                      className="btn ghost"
                      onClick={(event: MouseEvent<HTMLButtonElement>) => {
                        event.stopPropagation();
                        onOpenEditDialog?.(customer);
                      }}
                    >
                      Editar
                    </button>
                    <button
                      className="btn ghost"
                      onClick={(event: MouseEvent<HTMLButtonElement>) => {
                        event.stopPropagation();
                        deleteMutation.mutate(customer.id);
                      }}
                      disabled={deleteMutation.isPending}
                    >
                      {customer.active !== false ? "Desactivar" : "Activar"}
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )
      )}

      {showEmptyState && (
        <div className="muted text-neutral-400 text-center py-8">
          <p>Sin clientes</p>
          <p className="small">Crea tu primer cliente usando el formulario debajo.</p>
        </div>
      )}

      {customersQuery.isError && filteredCustomers.length > 0 && (
        <div className="error inline bg-red-950 border border-red-800 rounded-lg p-3">
          <span className="text-red-400">{errorMessage}</span>
          <button className="btn ghost" type="button" onClick={() => refetchCustomers()}>
            Reintentar
          </button>
        </div>
      )}

      <div ref={loadMoreRef} aria-hidden="true" />

      {isFetchingNextPage && <p className="muted text-neutral-400">Cargando mas...</p>}
      {!hasNextPage && filteredCustomers.length > 0 && <p className="muted text-neutral-400">No hay mas resultados</p>}

      {selectedCustomer && (
        <>
          <div className="map-panel bg-neutral-800 border border-neutral-700 rounded-lg mt-4">
            <div className="map-header border-b border-neutral-700">
              <div>
                <strong className="text-neutral-100">{selectedCustomer.name}</strong>
                {selectedCustomer.address ? (
                  <p className="muted small text-neutral-400">{selectedCustomer.address}</p>
                ) : (
                  <p className="muted small text-neutral-400">Sin dirección registrada</p>
                )}
              </div>
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <button
                  type="button"
                  className="btn ghost"
                  onClick={() => setShowDetailsPanel(!showDetailsPanel)}
                >
                  {showDetailsPanel ? "Ocultar detalles" : "Ver detalles"}
                </button>
                {mapExternalUrl && (
                  <a className="btn ghost" href={mapExternalUrl} target="_blank" rel="noreferrer">
                    Ver en Google Maps
                  </a>
                )}
              </div>
            </div>
            {mapEmbedUrl ? (
              <div className="map-preview">
                <iframe title={`Ubicación de ${selectedCustomer.name}`} src={mapEmbedUrl} loading="lazy" allowFullScreen />
              </div>
            ) : (
              <p className="muted small text-neutral-400">Registra una dirección o coordenadas para visualizar el mapa.</p>
            )}
          </div>

          {showDetailsPanel && (
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4" style={{ marginTop: "1rem"}}>
              <h3 style={{ marginBottom: "1rem", fontSize: "0.95rem" }} className="text-neutral-100">
                Historial y Estadísticas
              </h3>

              {statsQuery.isLoading && <p className="muted text-neutral-400">Cargando estadísticas...</p>}
              {statsQuery.isError && <p className="error text-red-400">Error al cargar estadísticas</p>}
              
              {statsQuery.data && (
                <>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: "1rem", marginBottom: "1rem" }}>
                    <div>
                      <p className="muted small text-neutral-400">Total Ventas</p>
                      <p style={{ fontSize: "1.5rem", fontWeight: "600", margin: "0.25rem 0" }} className="text-neutral-100">
                        {statsQuery.data.totalSales || 0}
                      </p>
                    </div>
                    <div>
                      <p className="muted small text-neutral-400">Ingresos Totales</p>
                      <p style={{ fontSize: "1.5rem", fontWeight: "600", margin: "0.25rem 0" }} className="text-neutral-100">
                        ${(statsQuery.data.totalRevenue || 0).toLocaleString("es-CL")}
                      </p>
                    </div>
                    <div>
                      <p className="muted small text-neutral-400">Última Venta</p>
                      <p style={{ fontSize: "1.5rem", fontWeight: "600", margin: "0.25rem 0" }} className="text-neutral-100">
                        {statsQuery.data.lastSaleDate 
                          ? new Date(statsQuery.data.lastSaleDate).toLocaleDateString("es-CL")
                          : "-"
                        }
                      </p>
                    </div>
                  </div>
                  
                  {(() => {
                    const rfm = calculateRFM(statsQuery.data);
                    return (
                      <div className="bg-neutral-900 border border-neutral-700 rounded-lg p-4 mb-4">
                        <h4 className="text-neutral-100 font-semibold mb-3">📊 Análisis RFM</h4>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-3">
                          <div>
                            <p className="text-xs text-neutral-400 mb-1">Recency</p>
                            <p className="text-lg font-bold text-neutral-100">{rfm.recencyScore}/5</p>
                            <p className="text-xs text-neutral-400">{rfm.recency} días</p>
                          </div>
                          <div>
                            <p className="text-xs text-neutral-400 mb-1">Frequency</p>
                            <p className="text-lg font-bold text-neutral-100">{rfm.frequencyScore}/5</p>
                            <p className="text-xs text-neutral-400">{rfm.frequency} ventas</p>
                          </div>
                          <div>
                            <p className="text-xs text-neutral-400 mb-1">Monetary</p>
                            <p className="text-lg font-bold text-neutral-100">{rfm.monetaryScore}/5</p>
                            <p className="text-xs text-neutral-400">${rfm.monetary.toLocaleString("es-CL")}</p>
                          </div>
                          <div>
                            <p className="text-xs text-neutral-400 mb-1">Score Total</p>
                            <p className="text-xl font-bold text-blue-400 mono">{rfm.score}</p>
                          </div>
                        </div>
                        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
                          <p className="text-sm text-neutral-400 mb-1">Segmento Automático</p>
                          <p className="text-lg font-semibold text-neutral-100">{rfm.segment}</p>
                        </div>
                      </div>
                    );
                  })()}
                </>
              )}

              <h4 style={{ marginBottom: "0.5rem", fontSize: "0.9rem" }} className="text-neutral-100">Últimas Ventas</h4>
              {salesHistoryQuery.isLoading && <p className="muted text-neutral-400">Cargando historial...</p>}
              {salesHistoryQuery.isError && <p className="error text-red-400">Error al cargar historial</p>}
              
              {salesHistoryQuery.data && salesHistoryQuery.data.content.length > 0 ? (
                <table className="table">
                  <thead>
                    <tr>
                      <th className="text-neutral-100">Fecha</th>
                      <th className="text-neutral-100">Tipo</th>
                      <th style={{ textAlign: "right" }} className="text-neutral-100">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {salesHistoryQuery.data.content.map((sale) => (
                      <tr key={sale.saleId}>
                        <td className="mono small text-neutral-300">
                          {new Date(sale.saleDate).toLocaleDateString("es-CL")}
                        </td>
                        <td className="text-neutral-300">{sale.docType || "-"}</td>
                        <td className="mono text-neutral-100" style={{ textAlign: "right", fontWeight: "600" }}>
                          ${sale.total.toLocaleString("es-CL")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                salesHistoryQuery.data && <p className="muted small text-neutral-400">No hay ventas registradas</p>
              )}
            </div>
          )}
        </>
      )}

      <div className="active-filter bg-neutral-800 border border-neutral-700 rounded-lg" style={{ display: "flex", gap: "1rem", alignItems: "center", marginTop: "1rem" }}>
        <span className="muted small text-neutral-400">Mostrar:</span>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button
            className={`btn ghost ${activeFilter === null ? "selected" : ""}`}
            type="button"
            onClick={() => setActiveFilter(null)}
          >
            Todos
          </button>
          <button
            className={`btn ghost ${activeFilter === true ? "selected" : ""}`}
            type="button"
            onClick={() => setActiveFilter(true)}
          >
            Activos
          </button>
          <button
            className={`btn ghost ${activeFilter === false ? "selected" : ""}`}
            type="button"
            onClick={() => setActiveFilter(false)}
          >
            Inactivos
          </button>
        </div>
      </div>

      {deleteMutation.isError && <p className="error text-red-400">{(deleteMutation.error as Error)?.message}</p>}
    </div>
  );
});

CustomersCard.displayName = "CustomersCard";

export default CustomersCard;









