import {
  FormEvent,
  KeyboardEvent,
  MouseEvent,
  MutableRefObject,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
} from "react";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createCustomer,
  listCustomers,
  type Customer,
  type CustomerPayload,
} from "../services/client";
import { computeNextPageParam, mergeCustomerPages } from "./customersUtils";
import useDebouncedValue from "../hooks/useDebouncedValue";
import { isValidRut, normalizeRut } from "../utils/rut";
import { parseProblemDetail } from "../utils/problemDetail";

const PAGE_SIZE = 20;
const SORT_ORDER = "name,asc";
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type FeedbackTone = "success" | "error" | null;

type FeedbackState = {
  tone: FeedbackTone;
  message: string | null;
};

type CustomerSelectProps = {
  value: Customer | null;
  onSelect: (customer: Customer) => void;
  onClear?: () => void;
  disabled?: boolean;
  label?: string;
  required?: boolean;
  error?: string | null;
  onErrorDismiss?: () => void;
  debounceMs?: number;
};

type CreateFormState = {
  name: string;
  document: string;
  email: string;
  phone: string;
  address: string;
  commune: string;
};

type CreateFormErrors = Partial<Record<keyof CreateFormState, string>>;

const EMPTY_FORM: CreateFormState = {
  name: "",
  document: "",
  email: "",
  phone: "",
  address: "",
  commune: "",
};

function toOptional(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function validateCreateForm(form: CreateFormState): {
  payload?: CustomerPayload;
  errors: CreateFormErrors;
} {
  const errors: CreateFormErrors = {};
  const nameInput = form.name.trim();

  if (nameInput.length < 3) {
    errors.name = "El nombre debe tener al menos 3 caracteres";
  } else if (nameInput.length > 120) {
    errors.name = "El nombre no puede superar 120 caracteres";
  }

  const documentInput = form.document.trim();
  let normalizedRut: string | undefined;
  if (documentInput.length > 0) {
    if (!isValidRut(documentInput)) {
      errors.document = "Ingresa un RUT válido";
    } else {
      normalizedRut = normalizeRut(documentInput);
    }
  }

  const emailInput = form.email.trim();
  if (emailInput && !EMAIL_REGEX.test(emailInput)) {
    errors.email = "Ingresa un email válido";
  }

  const phoneInput = form.phone.trim();

  if (Object.keys(errors).length > 0) {
    return { errors };
  }

  const payload: CustomerPayload = {
    name: nameInput,
    document: normalizedRut ?? toOptional(form.document),
    email: emailInput ? emailInput.toLowerCase() : undefined,
    phone: phoneInput ? phoneInput.replace(/\s+/g, " ").trim() : undefined,
    address: toOptional(form.address),
    commune: toOptional(form.commune),
  };

  return { payload, errors };
}

function useHighlightReset<T>(
  items: T[],
  dropdownOpen: boolean,
  setIndex: (value: number) => void,
) {
  const previousLength = useRef<number>(items.length);

  useEffect(() => {
    if (!dropdownOpen) {
      previousLength.current = items.length;
      return;
    }
    if (items.length === 0) {
      setIndex(-1);
      previousLength.current = 0;
      return;
    }
    if (items.length !== previousLength.current) {
      setIndex(0);
      previousLength.current = items.length;
    }
  }, [items, dropdownOpen, setIndex]);
}

export default function CustomerSelect({
  value,
  onSelect,
  onClear,
  disabled,
  label = "Cliente",
  required = false,
  error = null,
  onErrorDismiss,
  debounceMs = 400,
}: CustomerSelectProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const [feedback, setFeedback] = useState<FeedbackState>({ tone: null, message: null });
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateFormState>({ ...EMPTY_FORM });
  const [createErrors, setCreateErrors] = useState<CreateFormErrors>({});
  const [createGlobalError, setCreateGlobalError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const blurTimerRef = useRef<number | null>(null);
  const lastValueId = useRef<string | null>(null);
  const debouncedQuery = useDebouncedValue(searchTerm, debounceMs);
  const queryClient = useQueryClient();
  const listboxId = useId();
  const createTitleId = useId();
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const observerRef: MutableRefObject<IntersectionObserver | null> = useRef(null);

  useEffect(() => {
    const currentId = value?.id ?? null;
    if (currentId === lastValueId.current) {
      return;
    }
    lastValueId.current = currentId;
    if (value) {
      setSearchTerm(value.name ?? "");
    } else {
      setSearchTerm("");
    }
  }, [value]);

  const customersQuery = useInfiniteQuery({
    queryKey: ["customers", "picker", debouncedQuery],
    queryFn: async ({ pageParam = 0 }) => {
      const trimmed = debouncedQuery.trim();
      const response = await listCustomers({
        page: pageParam,
        size: PAGE_SIZE,
        sort: SORT_ORDER,
        q: trimmed.length > 0 ? trimmed : undefined,
      });
      return response;
    },
    getNextPageParam: (lastPage) => computeNextPageParam(lastPage),
    initialPageParam: 0,
  });

  const customers = useMemo(() => mergeCustomerPages(customersQuery.data?.pages ?? []), [customersQuery.data]);

  useHighlightReset(customers, dropdownOpen, setHighlightedIndex);

  const hasNextPage = customersQuery.hasNextPage ?? false;
  const isFetchingNextPage = customersQuery.isFetchingNextPage;
  const isLoading = customersQuery.isPending && customers.length === 0;
  const runOnNextFrame = useCallback((callback: () => void) => {
    if (typeof window !== "undefined" && typeof window.requestAnimationFrame === "function") {
      window.requestAnimationFrame(callback);
    } else {
      setTimeout(callback, 0);
    }
  }, []);

  useEffect(() => {
    if (!dropdownOpen) {
      return;
    }
    if (typeof window === "undefined" || typeof window.IntersectionObserver === "undefined") {
      return;
    }
    const node = loadMoreRef.current;
    if (!node) {
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      const [entry] = entries;
      if (entry?.isIntersecting && hasNextPage && !isFetchingNextPage) {
        void customersQuery.fetchNextPage();
      }
    });
    observer.observe(node);
    observerRef.current = observer;
    return () => observer.disconnect();
  }, [dropdownOpen, hasNextPage, isFetchingNextPage, customersQuery]);

  const clearBlurTimer = () => {
    if (blurTimerRef.current) {
      window.clearTimeout(blurTimerRef.current);
      blurTimerRef.current = null;
    }
  };

  const closeDropdown = useCallback(() => {
    setDropdownOpen(false);
    setHighlightedIndex(-1);
  }, []);

  const handleSelect = useCallback(
    (customer: Customer) => {
      onSelect(customer);
      closeDropdown();
      setFeedback({ tone: null, message: null });
      onErrorDismiss?.();
      runOnNextFrame(() => inputRef.current?.blur());
    },
    [closeDropdown, onSelect, onErrorDismiss, runOnNextFrame],
  );

  const handleInputFocus = () => {
    if (disabled) return;
    clearBlurTimer();
    setDropdownOpen(true);
  };

  const handleInputBlur = () => {
    clearBlurTimer();
    blurTimerRef.current = window.setTimeout(() => {
      closeDropdown();
    }, 120);
  };

  const handleInputChange = (event: FormEvent<HTMLInputElement>) => {
    const { value: nextValue } = event.currentTarget;
    setSearchTerm(nextValue);
    setFeedback({ tone: null, message: null });
    if (!dropdownOpen) {
      setDropdownOpen(true);
    }
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (!dropdownOpen && (event.key === "ArrowDown" || event.key === "ArrowUp" || event.key === "Enter")) {
      setDropdownOpen(true);
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      if (customers.length === 0) return;
      setHighlightedIndex((prev) => {
        if (prev + 1 >= customers.length) return prev;
        return prev + 1;
      });
      return;
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      if (customers.length === 0) return;
      setHighlightedIndex((prev) => {
        if (prev <= 0) return 0;
        return prev - 1;
      });
      return;
    }
    if (event.key === "Enter") {
      if (dropdownOpen && highlightedIndex >= 0 && customers[highlightedIndex]) {
        event.preventDefault();
        handleSelect(customers[highlightedIndex]);
      }
      return;
    }
    if (event.key === "Escape") {
      event.preventDefault();
      if (dropdownOpen) {
        closeDropdown();
        return;
      }
      if (searchTerm.length > 0) {
        setSearchTerm("");
        onClear?.();
      }
    }
  };

  const handleOptionMouseDown = (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    clearBlurTimer();
  };

  const openCreateDialog = () => {
    setCreateOpen(true);
    setCreateForm({ ...EMPTY_FORM, name: searchTerm.trim() });
    setCreateErrors({});
    setCreateGlobalError(null);
    closeDropdown();
    runOnNextFrame(() => {
      inputRef.current?.blur();
    });
  };

  const handleCancelCreate = () => {
    if (createMutation.isPending) return;
    setCreateOpen(false);
    setCreateForm({ ...EMPTY_FORM });
    setCreateErrors({});
    setCreateGlobalError(null);
    inputRef.current?.focus();
  };

  const createMutation = useMutation({
    mutationFn: (payload: CustomerPayload) => createCustomer(payload),
    onSuccess: (customer) => {
      queryClient.invalidateQueries({ queryKey: ["customers"], exact: false });
      setFeedback({ tone: "success", message: "Cliente creado" });
      setCreateOpen(false);
      setCreateForm({ ...EMPTY_FORM });
      setCreateErrors({});
      setCreateGlobalError(null);
      handleSelect(customer);
      runOnNextFrame(() => inputRef.current?.focus());
    },
    onError: (error) => {
      const parsed = parseProblemDetail(error);
      const fieldErrors: CreateFormErrors = {};
      for (const [field, message] of Object.entries(parsed.fieldErrors)) {
        if (message && field in EMPTY_FORM) {
          fieldErrors[field as keyof CreateFormState] = message;
        }
      }
      setCreateErrors(fieldErrors);
      setCreateGlobalError(parsed.message ?? "No se pudo crear el cliente");
    },
  });

  const handleCreateSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCreateGlobalError(null);
    const { payload, errors } = validateCreateForm(createForm);
    if (!payload || Object.keys(errors).length > 0) {
      setCreateErrors(errors);
      return;
    }
    createMutation.mutate(payload);
  };

  const renderOptionMeta = (customer: Customer) => {
    const meta: string[] = [];
    if (customer.document) {
      meta.push(customer.document);
    }
    if (customer.email) {
      meta.push(customer.email);
    }
    if (!customer.email && customer.phone) {
      meta.push(customer.phone);
    }
    return meta.join(" • ");
  };

  return (
    <div className="customer-select" ref={containerRef}>
      <label className="customer-select__label" htmlFor={`${listboxId}-input`}>
        {label}
        {required ? <span className="customer-select__required" aria-hidden="true">*</span> : null}
      </label>
      <div className="customer-select__field">
        <div
          className={`customer-select__combobox${dropdownOpen ? " is-open" : ""}${disabled ? " is-disabled" : ""}${
            error ? " has-error" : ""
          }`}
        >
          <input
            id={`${listboxId}-input`}
            ref={inputRef}
            type="text"
            className="customer-select__input"
            role="combobox"
            aria-autocomplete="list"
            aria-expanded={dropdownOpen}
            aria-controls={listboxId}
            aria-activedescendant={
              highlightedIndex >= 0 && customers[highlightedIndex] ? `${listboxId}-option-${highlightedIndex}` : undefined
            }
            aria-describedby={error ? `${listboxId}-error` : undefined}
            placeholder="Buscar por nombre, RUT o email…"
            value={searchTerm}
            onFocus={handleInputFocus}
            onBlur={handleInputBlur}
            onInput={handleInputChange}
            onKeyDown={handleKeyDown}
            disabled={disabled}
            autoComplete="off"
            aria-required={required ? "true" : undefined}
          />
          {value ? (
            <button
              type="button"
              className="customer-select__clear"
              onClick={() => {
                onClear?.();
                setSearchTerm("");
                setFeedback({ tone: null, message: null });
                inputRef.current?.focus();
              }}
              aria-label="Limpiar cliente seleccionado"
              disabled={disabled}
            >
              ×
            </button>
          ) : null}
          <button
            type="button"
            className="customer-select__create-btn"
            onClick={openCreateDialog}
            disabled={disabled}
          >
            + Nuevo cliente
          </button>
        </div>
        {dropdownOpen ? (
          <div className="customer-select__dropdown" role="listbox" id={listboxId}>
            {isLoading ? (
              <div className="customer-select__empty">Buscando clientes…</div>
            ) : customers.length === 0 ? (
              <div className="customer-select__empty" role="status" aria-live="polite">
                <p>Sin resultados. ¿Crear cliente?</p>
                <button type="button" className="btn ghost" onClick={openCreateDialog}>
                  Crear cliente
                </button>
              </div>
            ) : (
              <ul className="customer-select__options">
                {customers.map((customer, index) => {
                  const meta = renderOptionMeta(customer);
                  const isActive = index === highlightedIndex;
                  return (
                    <li key={customer.id}>
                      <button
                        type="button"
                        role="option"
                        id={`${listboxId}-option-${index}`}
                        className={`customer-select__option${isActive ? " is-active" : ""}`}
                        aria-selected={isActive}
                        onMouseDown={handleOptionMouseDown}
                        onClick={() => handleSelect(customer)}
                      >
                        <span className="customer-select__option-name">{customer.name}</span>
                        {meta ? <span className="customer-select__option-meta">{meta}</span> : null}
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
            {customersQuery.isError ? (
              <div className="customer-select__empty customer-select__empty--error" role="alert">
                {(customersQuery.error as Error).message || "No se pudieron cargar los clientes"}
              </div>
            ) : null}
            {hasNextPage ? (
              <div className="customer-select__load-more" ref={loadMoreRef}>
                <button
                  type="button"
                  className="btn ghost"
                  onClick={() => void customersQuery.fetchNextPage()}
                  disabled={isFetchingNextPage}
                >
                  {isFetchingNextPage ? "Cargando más…" : "Cargar más"}
                </button>
              </div>
            ) : null}
          </div>
        ) : null}
        {error ? (
          <p id={`${listboxId}-error`} className="customer-select__feedback customer-select__feedback--error" role="alert">
            {error}
          </p>
        ) : null}
        {feedback.tone && feedback.message ? (
          <p
            className={`customer-select__feedback customer-select__feedback--${feedback.tone}`}
            role="status"
            aria-live="polite"
          >
            {feedback.message}
          </p>
        ) : null}
      </div>

      {createOpen ? (
        <div className="customer-create-overlay" role="dialog" aria-modal="true" aria-labelledby={createTitleId}>
          <div
            className="customer-create-overlay__backdrop"
            onClick={handleCancelCreate}
            role="presentation"
          ></div>
          <section className="customer-create-sheet">
            <header className="customer-create-sheet__header">
              <h3 id={createTitleId}>Nuevo cliente</h3>
              <button
                type="button"
                className="icon-btn"
                onClick={handleCancelCreate}
                aria-label="Cerrar"
                disabled={createMutation.isPending}
              >
                ×
              </button>
            </header>
            <form className="customer-create-sheet__form" onSubmit={handleCreateSubmit} noValidate>
              <label>
                <span>Nombre *</span>
                <input
                  type="text"
                  className={`input${createErrors.name ? " input-error" : ""}`}
                  value={createForm.name}
                  onChange={(event) => {
                    setCreateForm((prev) => ({ ...prev, name: event.target.value }));
                    setCreateErrors((prev) => ({ ...prev, name: undefined }));
                  }}
                  required
                  minLength={3}
                  maxLength={120}
                  autoFocus
                  disabled={createMutation.isPending}
                />
                {createErrors.name ? (
                  <span className="error" role="alert">
                    {createErrors.name}
                  </span>
                ) : null}
              </label>
              <label>
                <span>RUT</span>
                <input
                  type="text"
                  className={`input${createErrors.document ? " input-error" : ""}`}
                  value={createForm.document}
                  onChange={(event) => {
                    setCreateForm((prev) => ({ ...prev, document: event.target.value }));
                    setCreateErrors((prev) => ({ ...prev, document: undefined }));
                  }}
                  placeholder="76.123.456-0"
                  disabled={createMutation.isPending}
                />
                {createErrors.document ? (
                  <span className="error" role="alert">
                    {createErrors.document}
                  </span>
                ) : null}
              </label>
              <label>
                <span>Email</span>
                <input
                  type="email"
                  className={`input${createErrors.email ? " input-error" : ""}`}
                  value={createForm.email}
                  onChange={(event) => {
                    setCreateForm((prev) => ({ ...prev, email: event.target.value }));
                    setCreateErrors((prev) => ({ ...prev, email: undefined }));
                  }}
                  placeholder="contacto@cliente.cl"
                  disabled={createMutation.isPending}
                />
                {createErrors.email ? (
                  <span className="error" role="alert">
                    {createErrors.email}
                  </span>
                ) : null}
              </label>
              <label>
                <span>Teléfono</span>
                <input
                  type="tel"
                  className="input"
                  value={createForm.phone}
                  onChange={(event) => {
                    setCreateForm((prev) => ({ ...prev, phone: event.target.value }));
                  }}
                  placeholder="+56 9 1234 5678"
                  disabled={createMutation.isPending}
                />
              </label>
              <label>
                <span>Dirección</span>
                <input
                  type="text"
                  className="input"
                  value={createForm.address}
                  onChange={(event) => {
                    setCreateForm((prev) => ({ ...prev, address: event.target.value }));
                  }}
                  placeholder="Av. Principal 1234"
                  disabled={createMutation.isPending}
                />
              </label>
              <label>
                <span>Comuna</span>
                <input
                  type="text"
                  className="input"
                  value={createForm.commune}
                  onChange={(event) => {
                    setCreateForm((prev) => ({ ...prev, commune: event.target.value }));
                  }}
                  placeholder="Providencia"
                  disabled={createMutation.isPending}
                />
              </label>
              {createGlobalError ? (
                <div className="customer-create-sheet__alert" role="alert">
                  {createGlobalError}
                </div>
              ) : null}
              <div className="customer-create-sheet__actions">
                <button
                  type="button"
                  className="btn ghost"
                  onClick={handleCancelCreate}
                  disabled={createMutation.isPending}
                >
                  Cancelar
                </button>
                <button className="btn" type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? "Guardando…" : "Guardar"}
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}
