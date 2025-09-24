import {
  FormEvent,
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
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createCustomer,
  deleteCustomer,
  listCustomers,
  Customer,
  CustomerPayload,
} from "../services/client";
import { computeNextPageParam, mergeCustomerPages } from "./customersUtils";

type Props = {
  segmentFilter?: string | null;
  segmentLabel?: string | null;
  onClearSegment?: () => void;
};

export type CustomersCardHandle = {
  focusCreate: () => void;
  clearForm: () => void;
};

const PAGE_SIZE = 20;
const SORT_ORDER = "createdAt,desc";

type FormState = Omit<CustomerPayload, "lat" | "lng"> & {
  latText: string;
  lngText: string;
};

const createEmptyForm = (): FormState => ({
  name: "",
  phone: "",
  email: "",
  address: "",
  segment: "",
  latText: "",
  lngText: "",
});

const CustomersCard = forwardRef<CustomersCardHandle, Props>((props, ref) => {
  const { segmentFilter, segmentLabel, onClearSegment } = props;
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [form, setForm] = useState<FormState>(() => createEmptyForm());
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null);
  const nameInputRef = useRef<HTMLInputElement | null>(null);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const pagesLoggedRef = useRef(0);

  useImperativeHandle(
    ref,
    () => ({
      focusCreate: () => {
        nameInputRef.current?.focus();
      },
      clearForm: () => {
        setForm(createEmptyForm());
        nameInputRef.current?.focus();
      },
    }),
    []
  );

  useEffect(() => {
    pagesLoggedRef.current = 0;
  }, [query, segmentFilter]);

  const fetchPage = useCallback(
    async (pageParam: number) => {
      const trimmed = query.trim();
      const start = performance.now();
      const response = await listCustomers({
        q: trimmed.length ? trimmed : undefined,
        segment: segmentFilter ?? undefined,
        page: pageParam,
        size: PAGE_SIZE,
        sort: SORT_ORDER,
      });
      const elapsed = Math.round(performance.now() - start);
      const count = response.content?.length ?? 0;
      console.info(`[Customers] page ${pageParam} fetched (${count} items) in ${elapsed}ms`);
      return response;
    },
    [query, segmentFilter]
  );

  const customersQuery = useInfiniteQuery({
    queryKey: ["customers", query, segmentFilter ?? null],
    queryFn: ({ pageParam = 0 }) => fetchPage(pageParam),
    getNextPageParam: (lastPage) => computeNextPageParam(lastPage),
    initialPageParam: 0,
  });

  const { fetchNextPage, isFetchingNextPage, refetch: refetchCustomers } = customersQuery;
  const flattenedCustomers = useMemo(() => {
    return mergeCustomerPages(customersQuery.data?.pages ?? []);
  }, [customersQuery.data]);

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

  const selectedCustomer = useMemo(() => {
    if (!selectedCustomerId) return null;
    return flattenedCustomers.find((customer) => customer.id === selectedCustomerId) ?? null;
  }, [flattenedCustomers, selectedCustomerId]);

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

  const createMutation = useMutation({
    mutationFn: (payload: CustomerPayload) => createCustomer(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers"], exact: false });
      setForm(createEmptyForm());
      nameInputRef.current?.focus();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCustomer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["customers"], exact: false });
    },
  });

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!form.name || !form.name.trim()) {
      alert("El nombre es obligatorio");
      return;
    }
    const parseCoordinate = (value: string): number | null => {
      if (!value || !value.trim()) {
        return null;
      }
      const parsed = Number(value.trim());
      return Number.isFinite(parsed) ? parsed : null;
    };

    createMutation.mutate({
      name: form.name.trim(),
      phone: form.phone?.trim() || undefined,
      email: form.email?.trim() || undefined,
      address: form.address?.trim() || undefined,
      segment: form.segment?.trim() || undefined,
      lat: parseCoordinate(form.latText),
      lng: parseCoordinate(form.lngText),
    });
  };

  const errorMessage = customersQuery.isError
    ? (customersQuery.error as Error | { message?: string })?.message ?? "No se pudieron cargar los clientes"
    : undefined;
  const showEmptyState = !isInitialLoading && !customersQuery.isError && flattenedCustomers.length === 0;

  return (
    <div className="card">
      <div className="card-header">
        <h2>Clientes</h2>
        <input
          className="input"
          placeholder="Buscar por nombre"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {segmentLabel && (
        <div className="active-filter">
          <span>Filtrado por segmento: {segmentLabel}</span>
          {onClearSegment && (
            <button className="btn ghost" type="button" onClick={onClearSegment}>
              Limpiar
            </button>
          )}
        </div>
      )}

      {isInitialLoading && <p>Loading...</p>}
      {!isInitialLoading && customersQuery.isError && flattenedCustomers.length === 0 && (
        <div className="error">
          <p>{errorMessage}</p>
          <button className="btn ghost" type="button" onClick={() => refetchCustomers()}>
            Reintentar
          </button>
        </div>
      )}

      {!customersQuery.isError && flattenedCustomers.length > 0 && (
        <ul className="list" aria-live="polite">
          {flattenedCustomers.map((customer: Customer) => {
            const isSelected = selectedCustomerId === customer.id;
            return (
              <li
                key={customer.id}
                className={`list-row${isSelected ? " selected" : ""}`}
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
                  <strong>{customer.name}</strong>
                  {customer.segment ? <div className="mono small">{customer.segment}</div> : null}
                  {customer.email ? <div className="mono small">{customer.email}</div> : null}
                  {customer.phone ? <div className="mono small">{customer.phone}</div> : null}
                  {customer.address ? <div className="mono small">{customer.address}</div> : null}
                </div>
                <button
                  className="btn ghost"
                  onClick={(event: MouseEvent<HTMLButtonElement>) => {
                    event.stopPropagation();
                    deleteMutation.mutate(customer.id);
                  }}
                  disabled={deleteMutation.isPending}
                >
                  Eliminar
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {showEmptyState && (
        <div className="muted">
          <p>Sin clientes</p>
          <p className="small">Crea tu primer cliente usando el formulario debajo.</p>
        </div>
      )}

      {customersQuery.isError && flattenedCustomers.length > 0 && (
        <div className="error inline">
          <span>{errorMessage}</span>
          <button className="btn ghost" type="button" onClick={() => refetchCustomers()}>
            Reintentar
          </button>
        </div>
      )}

      <div ref={loadMoreRef} aria-hidden="true" />

      {isFetchingNextPage && <p className="muted">Cargando mas...</p>}
      {!hasNextPage && flattenedCustomers.length > 0 && <p className="muted">No hay mas resultados</p>}

      {selectedCustomer && (
        <div className="map-panel">
          <div className="map-header">
            <div>
              <strong>{selectedCustomer.name}</strong>
              {selectedCustomer.address ? (
                <p className="muted small">{selectedCustomer.address}</p>
              ) : (
                <p className="muted small">Sin dirección registrada</p>
              )}
            </div>
            {mapExternalUrl && (
              <a className="btn ghost" href={mapExternalUrl} target="_blank" rel="noreferrer">
                Ver en Google Maps
              </a>
            )}
          </div>
          {mapEmbedUrl ? (
            <div className="map-preview">
              <iframe title={`Ubicación de ${selectedCustomer.name}`} src={mapEmbedUrl} loading="lazy" allowFullScreen />
            </div>
          ) : (
            <p className="muted small">Registra una dirección o coordenadas para visualizar el mapa.</p>
          )}
        </div>
      )}

      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          <span>Nombre *</span>
          <input
            className="input"
            ref={nameInputRef}
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Cliente demo"
          />
        </label>
        <label>
          <span>Email</span>
          <input
            className="input"
            value={form.email ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))}
            placeholder="cliente@correo.com"
          />
        </label>
        <label>
          <span>Telefono</span>
          <input
            className="input"
            value={form.phone ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, phone: e.target.value }))}
            placeholder="+56 9 0000 0000"
          />
        </label>
        <label>
          <span>Direccion</span>
          <input
            className="input"
            value={form.address ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, address: e.target.value }))}
            placeholder="Av. Demo 123"
          />
        </label>
        <label>
          <span>Segmento</span>
          <input
            className="input"
            value={form.segment ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, segment: e.target.value }))}
            placeholder="Retail"
          />
        </label>
        <label>
          <span>Latitud</span>
          <input
            className="input"
            type="number"
            step="any"
            value={form.latText}
            onChange={(e) => setForm((prev) => ({ ...prev, latText: e.target.value }))}
            placeholder="-33.4489"
          />
        </label>
        <label>
          <span>Longitud</span>
          <input
            className="input"
            type="number"
            step="any"
            value={form.lngText}
            onChange={(e) => setForm((prev) => ({ ...prev, lngText: e.target.value }))}
            placeholder="-70.6693"
          />
        </label>
        <div className="buttons">
          <button className="btn" type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Guardando..." : "Crear"}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => {
              setForm(createEmptyForm());
              nameInputRef.current?.focus();
            }}
          >
            Limpiar
          </button>
        </div>
        {createMutation.isError && <p className="error">{(createMutation.error as Error)?.message}</p>}
        {deleteMutation.isError && <p className="error">{(deleteMutation.error as Error)?.message}</p>}
      </form>
    </div>
  );
});

CustomersCard.displayName = "CustomersCard";

export default CustomersCard;









