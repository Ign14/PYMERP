import axios from "axios";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api";
const DEFAULT_COMPANY_ID = import.meta.env.VITE_COMPANY_ID ?? null;

let authToken: string | null = null;
let activeCompanyId: string | null = DEFAULT_COMPANY_ID;
let currentRefreshToken: string | null = null;

export const api = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const headers = config.headers ?? {};
  if (authToken) {
    headers["Authorization"] = `Bearer ${authToken}`;
  }
  if (activeCompanyId) {
    headers["X-Company-Id"] = activeCompanyId;
  }
  config.headers = headers;
  return config;
});

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export type HealthResponse = {
  status: string;
  components?: Record<string, unknown>;
  details?: Record<string, unknown>;
};

export type Company = {
  id: string;
  name: string;
  rut?: string;
  industry?: string;
  createdAt?: string;
};

export type Product = {
  id: string;
  sku: string;
  name: string;
  description?: string;
  category?: string;
  barcode?: string;
  imageUrl?: string;
  currentPrice?: number | string;
  active: boolean;
};

export type ProductPayload = {
  sku: string;
  name: string;
  description?: string;
  category?: string;
  barcode?: string;
  imageUrl?: string;
};

export type Supplier = {
  id: string;
  name: string;
  rut?: string;
};

export type SupplierPayload = {
  name: string;
  rut?: string;
};

export type Customer = {
  id: string;
  name: string;
  address?: string;
  lat?: number | string | null;
  lng?: number | string | null;
  phone?: string;
  email?: string;
  segment?: string;
};

export type CreateCompanyPayload = {
  name: string;
  rut?: string;
};

export type ListCustomersParams = {
  q?: string;
  segment?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export type CustomerPayload = {
  name: string;
  address?: string;
  lat?: number | null;
  lng?: number | null;
  phone?: string;
  email?: string;
  segment?: string;
};

export const UNASSIGNED_SEGMENT_CODE = "__UNASSIGNED__";

export type CustomerSegmentSummary = {
  segment: string;
  code: string;
  total: number;
};

export type PriceHistoryEntry = {
  id: string;
  productId: string;
  price: number;
  validFrom: string;
};

export type PriceChangePayload = {
  price: number;
  validFrom?: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type RefreshPayload = {
  refreshToken: string;
};

export type LoginResponse = {
  token: string;
  expiresIn: number;
  refreshToken: string;
  refreshExpiresIn: number;
  companyId: string;
  email: string;
  name: string;
  roles: string[];
  modules: string[];
};

export type AuthSession = LoginResponse & {
  expiresAt: number;
  refreshExpiresAt: number;
};

export type SaleItemPayload = {
  productId: string;
  qty: number;
  unitPrice: number;
  discount?: number;
};

export type SalePayload = {
  customerId: string;
  paymentMethod?: string;
  docType: string;
  items: SaleItemPayload[];
};

export type SaleRes = {
  id: string;
  customerId?: string;
  customerName?: string;
  customerName?: string;
  status: string;
  net: number;
  vat: number;
  total: number;
  issuedAt: string;
  docType: string;
  paymentMethod: string;
};

export type SaleSummary = {
  id: string;
  customerId?: string;
  customerName?: string;
  docType?: string;
  paymentMethod?: string;
  status: string;
  net: number;
  vat: number;
  total: number;
  issuedAt: string;
};

export type SaleDetailLine = {
  productId?: string;
  productName: string;
  qty: number;
  unitPrice: number;
  discount: number;
  lineTotal: number;
};

export type SaleDetail = {
  id: string;
  issuedAt: string;
  docType: string;
  paymentMethod: string;
  status: string;
  customer?: { id: string; name: string } | null;
  items: SaleDetailLine[];
  net: number;
  vat: number;
  total: number;
  thermalTicket: string;
};

export type SaleUpdatePayload = {
  docType?: string;
  paymentMethod?: string;
  status?: string;
};

export type SalesDailyPoint = {
  date: string;
  total: number;
  count: number;
};

export type PurchaseItemPayload = {
  productId: string;
  qty: number;
  unitCost: number;
  vatRate?: number;
  mfgDate?: string;
  expDate?: string;
};

export type PurchasePayload = {
  supplierId: string;
  docType: string;
  docNumber: string;
  net: number;
  vat: number;
  total: number;
  pdfUrl?: string;
  issuedAt: string;
  items: PurchaseItemPayload[];
};

export type PurchaseSummary = {
  id: string;
  supplierId?: string;
  supplierName?: string;
  docType?: string;
  docNumber?: string;
  status: string;
  net: number;
  vat: number;
  total: number;
  issuedAt: string;
};


export type PurchaseUpdatePayload = {
  docType?: string;
  docNumber?: string;
  status?: string;
};

export type PurchaseDailyPoint = {
  date: string;
  total: number;
  count: number;
};

export type InventoryAlert = {
  lotId: string;
  productId: string;
  qtyAvailable: number;
  createdAt: string;
  expDate?: string | null;
};

export type InventorySummary = {
  totalValue: number | string;
  activeProducts: number;
  inactiveProducts: number;
  totalProducts: number;
  lowStockAlerts: number;
  lowStockThreshold: number | string;
};

export type InventorySettings = {
  lowStockThreshold: number | string;
  updatedAt: string;
};

export type InventoryAdjustmentPayload = {
  productId: string;
  quantity: number;
  reason: string;
  direction: "increase" | "decrease";
  unitCost?: number;
  lotId?: string;
  mfgDate?: string;
  expDate?: string;
};

export type InventoryAdjustmentResponse = {
  productId: string;
  appliedQuantity: number | string;
  direction: string;
};
export type ListSalesParams = {
  page?: number;
  size?: number;
  status?: string;
  docType?: string;
  paymentMethod?: string;
  search?: string;
  from?: string;
  to?: string;
};

export type ListPurchasesParams = {
  page?: number;
  size?: number;
  status?: string;
  docType?: string;
  search?: string;
  from?: string;
  to?: string;
};

export function setSession(session: { token?: string | null; companyId?: string | null; refreshToken?: string | null }) {
  if (Object.prototype.hasOwnProperty.call(session, "token")) {
    authToken = session.token ?? null;
  }
  if (Object.prototype.hasOwnProperty.call(session, "companyId")) {
    activeCompanyId = session.companyId ?? DEFAULT_COMPANY_ID;
  }
  if (Object.prototype.hasOwnProperty.call(session, "refreshToken")) {
    currentRefreshToken = session.refreshToken ?? null;
  }
}

export function clearSession() {
  authToken = null;
  activeCompanyId = DEFAULT_COMPANY_ID;
  currentRefreshToken = null;
}

export async function login(payload: LoginPayload): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>("/v1/auth/login", payload);
  setSession({ token: data.token, companyId: data.companyId, refreshToken: data.refreshToken });
  return data;
}

export async function refreshAuth(payload: RefreshPayload): Promise<LoginResponse> {
  if (!payload.refreshToken) {
    throw new Error("Refresh token is required");
  }
  const { data } = await api.post<LoginResponse>("/v1/auth/refresh", null, {
    headers: { ["X-Refresh-Token"]: payload.refreshToken },
  });
  setSession({ token: data.token, companyId: data.companyId, refreshToken: data.refreshToken });
  return data;
}

export function getCurrentRefreshToken() {
  return currentRefreshToken;
}

export async function fetchHealth(): Promise<HealthResponse> {
  const { data } = await api.get<HealthResponse>("/actuator/health");
  return data;
}

export async function listCompanies(): Promise<Company[]> {
  const { data } = await api.get<Company[]>("/v1/companies");
  return data;
}

export async function createCompany(payload: CreateCompanyPayload): Promise<Company> {
  const { data } = await api.post<Company>("/v1/companies", payload);
  return data;
}

export async function listProducts(params?: { q?: string; page?: number; size?: number; status?: "active" | "inactive" | "all" }): Promise<Page<Product>> {
  const { data } = await api.get<Page<Product>>(`/v1/products`, { params });
  return data;
}

export async function createProduct(payload: ProductPayload): Promise<Product> {
  const { data } = await api.post<Product>("/v1/products", payload);
  return data;
}

export async function updateProduct(id: string, payload: ProductPayload): Promise<Product> {
  const { data } = await api.put<Product>(`/v1/products/${id}`, payload);
  return data;
}

export async function updateProductStatus(id: string, active: boolean): Promise<Product> {
  const { data } = await api.patch<Product>(`/v1/products/${id}/status`, { active });
  return data;
}

export async function deleteProduct(id: string): Promise<void> {
  await api.delete(`/v1/products/${id}`);
}

export async function listSuppliers(): Promise<Supplier[]> {
  const { data } = await api.get<Supplier[]>("/v1/suppliers");
  return data;
}

export async function createSupplier(payload: SupplierPayload): Promise<Supplier> {
  const { data } = await api.post<Supplier>("/v1/suppliers", payload);
  return data;
}

export async function updateSupplier(id: string, payload: SupplierPayload): Promise<Supplier> {
  const { data } = await api.put<Supplier>(`/v1/suppliers/${id}`, payload);
  return data;
}

export async function deleteSupplier(id: string): Promise<void> {
  await api.delete(`/v1/suppliers/${id}`);
}

export async function listCustomers(params: ListCustomersParams = {}): Promise<Page<Customer>> {
  const requestedPage = params.page ?? 0;
  const requestedSize = params.size ?? 20;
  const queryParams: Record<string, unknown> = {
    page: requestedPage,
    size: requestedSize,
    sort: params.sort ?? 'createdAt,desc',
  };
  if (params.q && params.q.trim()) {
    queryParams.q = params.q.trim();
  }
  if (params.segment && params.segment.trim()) {
    queryParams.segment = params.segment.trim();
  }
  const { data } = await api.get<Page<Customer>>('/v1/customers', { params: queryParams });
  const raw = data as Page<Customer> & { page?: number; items?: Customer[]; total?: number; hasNext?: boolean };
  const content = raw.content ?? raw.items ?? [];
  const size = raw.size ?? requestedSize;
  const totalElements =
    typeof raw.totalElements === 'number'
      ? raw.totalElements
      : typeof raw.total === 'number'
      ? raw.total
      : content.length;
  const number =
    typeof raw.number === 'number'
      ? raw.number
      : typeof raw.page === 'number'
      ? raw.page
      : requestedPage;
  const totalPages =
    typeof raw.totalPages === 'number'
      ? raw.totalPages
      : size > 0
      ? Math.ceil(totalElements / size)
      : 0;
  const hasNext =
    typeof raw.hasNext === 'boolean'
      ? raw.hasNext
      : totalPages > 0
      ? number + 1 < totalPages
      : content.length === size;

  return {
    ...raw,
    content,
    size,
    totalElements,
    totalPages,
    number,
    hasNext,
  };
}

export async function listCustomerSegments(): Promise<CustomerSegmentSummary[]> {
  const { data } = await api.get<CustomerSegmentSummary[]>("/v1/customers/segments");
  return data;
}

export async function createCustomer(payload: CustomerPayload): Promise<Customer> {
  const { data } = await api.post<Customer>("/v1/customers", payload);
  return data;
}

export async function updateCustomer(id: string, payload: CustomerPayload): Promise<Customer> {
  const { data } = await api.put<Customer>(`/v1/customers/${id}`, payload);
  return data;
}

export async function deleteCustomer(id: string): Promise<void> {
  await api.delete(`/v1/customers/${id}`);
}

export async function listProductPrices(productId: string, params?: { page?: number; size?: number }): Promise<Page<PriceHistoryEntry>> {
  const { data } = await api.get<Page<PriceHistoryEntry>>(`/v1/products/${productId}/prices`, { params });
  return data;
}

export async function createProductPrice(productId: string, payload: PriceChangePayload): Promise<PriceHistoryEntry> {
  const { data } = await api.post<PriceHistoryEntry>(`/v1/products/${productId}/prices`, payload);
  return data;
}

export async function listSales(params: ListSalesParams = {}): Promise<Page<SaleSummary>> {
  const { data } = await api.get<Page<SaleSummary>>("/v1/sales", {
    params,
  });
  return data;
}

export async function updateSale(id: string, payload: SaleUpdatePayload): Promise<SaleRes> {
  const { data } = await api.put<SaleRes>(`/v1/sales/${id}`, payload);
  return data;
}

export async function cancelSale(id: string): Promise<SaleRes> {
  const { data } = await api.post<SaleRes>(`/v1/sales/${id}/cancel`, {});
  return data;
}

export async function getSaleDetail(id: string): Promise<SaleDetail> {
  const { data } = await api.get<SaleDetail>(`/v1/sales/${id}`);
  return data;
}

export async function listSalesDaily(days = 14): Promise<SalesDailyPoint[]> {
  const { data } = await api.get<SalesDailyPoint[]>("/v1/sales/metrics/daily", { params: { days } });
  return data;
}

export async function createSale(payload: SalePayload): Promise<SaleRes> {
  const { data } = await api.post<SaleRes>("/v1/sales", payload);
  return data;
}

export async function listPurchases(params: ListPurchasesParams = {}): Promise<Page<PurchaseSummary>> {
  const { data } = await api.get<Page<PurchaseSummary>>("/v1/purchases", {
    params,
  });
  return data;
}

export async function updatePurchase(id: string, payload: PurchaseUpdatePayload): Promise<PurchaseSummary> {
  const { data } = await api.put<PurchaseSummary>(`/v1/purchases/${id}`, payload);
  return data;
}

export async function cancelPurchase(id: string): Promise<PurchaseSummary> {
  const { data } = await api.post<PurchaseSummary>(`/v1/purchases/${id}/cancel`, {});
  return data;
}

export async function listPurchaseDaily(days = 14): Promise<PurchaseDailyPoint[]> {
  const { data } = await api.get<PurchaseDailyPoint[]>("/v1/purchases/metrics/daily", { params: { days } });
  return data;
}

export async function createPurchase(payload: PurchasePayload): Promise<{ id: string }> {
  const { data } = await api.post<{ id: string }>("/v1/purchases", payload);
  return data;
}

export async function listInventoryAlerts(threshold?: number): Promise<InventoryAlert[]> {
  const params = typeof threshold === "number" ? { threshold } : {};
  const { data } = await api.get<InventoryAlert[]>("/v1/inventory/alerts", { params });
  return data;
}

export async function getInventorySummary(): Promise<InventorySummary> {
  const { data } = await api.get<InventorySummary>("/v1/inventory/summary");
  return data;
}

export async function getInventorySettings(): Promise<InventorySettings> {
  const { data } = await api.get<InventorySettings>("/v1/inventory/settings");
  return data;
}

export async function updateInventorySettings(payload: { lowStockThreshold: number }): Promise<InventorySettings> {
  const { data } = await api.put<InventorySettings>("/v1/inventory/settings", payload);
  return data;
}

export async function createInventoryAdjustment(payload: InventoryAdjustmentPayload): Promise<InventoryAdjustmentResponse> {
  const { data } = await api.post<InventoryAdjustmentResponse>("/v1/inventory/adjustments", payload);
  return data;
}







