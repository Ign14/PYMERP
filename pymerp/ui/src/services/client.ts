import axios, { type AxiosResponse } from "axios";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api";
const DEFAULT_COMPANY_ID = import.meta.env.VITE_COMPANY_ID ?? null;

const DEV_FALLBACK_EMAIL = "admin@dev.local";
const DEV_FALLBACK_PASSWORD = "Admin1234";
const DEV_FALLBACK_TOKEN = "dev-session-token";
const DEV_FALLBACK_REFRESH_TOKEN = "dev-session-refresh-token";
const DEV_FALLBACK_COMPANY_ID = DEFAULT_COMPANY_ID ?? "dev-company";
const DEV_FALLBACK_ROLES = ["admin"] as const;
const DEV_FALLBACK_MODULES = [
  "sales",
  "purchases",
  "inventory",
  "customers",
  "suppliers",
  "finances",
  "reports",
  "settings",
] as const;

type StructuredCloneFn = <T>(value: T) => T;
const globalStructuredClone: StructuredCloneFn | undefined = (globalThis as typeof globalThis & {
  structuredClone?: StructuredCloneFn;
}).structuredClone;

function deepClone<T>(value: T): T {
  if (typeof globalStructuredClone === "function") {
    return globalStructuredClone(value);
  }
  return JSON.parse(JSON.stringify(value)) as T;
}

let offlineModeEnabled = false;

function markOffline(context: string) {
  if (!offlineModeEnabled) {
    offlineModeEnabled = true;
    console.warn(
      `[API] Network unreachable during ${context}. Switching to in-memory demo data so the workspace keeps working.`,
    );
  } else {
    console.info(`[API] Using offline demo data for ${context}.`);
  }
}

function maybeClone<T>(value: T): T {
  return deepClone(value);
}

function ensureArray<T>(value: T[] | undefined | null): T[] {
  return Array.isArray(value) ? value : [];
}

function extractFilenameFromDisposition(header: unknown): string | null {
  if (typeof header !== "string") {
    return null;
  }
  const utfMatch = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (utfMatch && utfMatch[1]) {
    try {
      return decodeURIComponent(utfMatch[1]);
    } catch (error) {
      console.warn("[API] Failed to decode UTF-8 filename", error);
    }
  }
  const match = header.match(/filename="?([^";]+)"?/i);
  if (match && match[1]) {
    return match[1];
  }
  return null;
}

export type DocumentFile = {
  blob: Blob;
  filename: string;
  mimeType: string;
};

function createDocumentFileFromResponse(
  response: AxiosResponse<Blob>,
  fallbackName: string,
): DocumentFile {
  const blob = response.data;
  const responseType = typeof blob?.type === "string" && blob.type ? blob.type : undefined;
  const headerType = typeof response.headers["content-type"] === "string"
    ? response.headers["content-type"]
    : undefined;
  const mimeType = responseType || headerType || "application/octet-stream";
  const filename =
    extractFilenameFromDisposition(response.headers["content-disposition"]) || fallbackName;
  return {
    blob,
    filename,
    mimeType,
  };
}

function createFallbackDocumentFile(id: string, action: "preview" | "download"): DocumentFile {
  const title = action === "preview" ? "Vista previa no disponible" : "Descarga no disponible";
  const body = action === "preview"
    ? "No hay contenido disponible sin conexión para mostrar este documento."
    : "No hay archivo disponible para descargar sin conexión.";
  const html = `<!DOCTYPE html><html lang="es"><head><meta charset="utf-8" /><title>${title}</title></head><body><main style="font-family:system-ui,sans-serif;padding:24px;max-width:640px;margin:auto;"><h1 style="margin-bottom:16px;">${title}</h1><p style="margin-bottom:16px;">${body}</p><p style="color:#555;">Documento: ${id}</p></main></body></html>`;
  const blob = new Blob([html], { type: "text/html" });
  return {
    blob,
    filename: `${action}-documento-${id}.html`,
    mimeType: "text/html",
  };
}

const now = new Date();
const iso = (date: Date) => date.toISOString();

function daysAgo(days: number) {
  const date = new Date(now);
  date.setDate(date.getDate() - days);
  return date;
}

type DemoSaleDetail = SaleDetail & { thermalTicket: string };

type DemoState = {
  companies: Company[];
  products: Product[];
  productPrices: Record<string, PriceHistoryEntry[]>;
  suppliers: Supplier[];
  customers: Customer[];
  sales: SaleSummary[];
  saleDetails: Record<string, DemoSaleDetail>;
  purchases: PurchaseSummary[];
  inventorySummary: InventorySummary;
  inventoryAlerts: InventoryAlert[];
  inventorySettings: InventorySettings;
  salesDaily: SalesDailyPoint[];
  purchaseDaily: PurchaseDailyPoint[];
};

const demoState: DemoState = {
  companies: [
    {
      id: "cmp-demo-001",
      businessName: "Demo Comercial SpA",
      rut: "76.123.456-0",
      businessActivity: "Retail",
      address: "Av. Siempre Viva 742, Santiago",
      commune: "Providencia",
      phone: "+56 2 2345 6677",
      email: "contacto@democomercial.cl",
      receiptFooterMessage: "¡Gracias por tu compra!",
      createdAt: iso(daysAgo(180)),
      updatedAt: iso(daysAgo(20)),
    },
    {
      id: "cmp-demo-002",
      businessName: "Servicios Norte Ltda.",
      rut: "77.987.654-3",
      businessActivity: "Servicios",
      address: "Av. Costanera 1200, Antofagasta",
      commune: "Antofagasta",
      phone: "+56 55 278 9900",
      email: "administracion@serviciosnorte.cl",
      receiptFooterMessage: "Servicio garantizado",
      createdAt: iso(daysAgo(95)),
      updatedAt: iso(daysAgo(10)),
    },
    {
      id: "cmp-demo-003",
      businessName: "Distribuidora Andina",
      rut: "78.321.654-K",
      businessActivity: "Logística",
      address: "Camino a Melipilla 15000, Maipú",
      commune: "Maipú",
      phone: "+56 2 2388 1122",
      email: "ventas@andina.cl",
      receiptFooterMessage: "Despachos en 24 horas",
      createdAt: iso(daysAgo(45)),
      updatedAt: iso(daysAgo(3)),
    },
  ],
  products: [
    {
      id: "prd-demo-001",
      sku: "SKU-1001",
      name: "Caf en grano premium 1Kg",
      description: "Tueste medio alto, origen Colombia",
      category: "Alimentos",
      barcode: "7800001001002",
      currentPrice: 11990,
      imageUrl: undefined,
      qrUrl: undefined,
      stock: 120,
      criticalStock: 40,
      active: true,
    },
    {
      id: "prd-demo-002",
      sku: "SKU-1002",
      name: "Azcar orgánica 1Kg",
      description: "Certificada comercio justo",
      category: "Alimentos",
      barcode: "7800001002001",
      currentPrice: 3290,
      imageUrl: undefined,
      qrUrl: undefined,
      stock: 68,
      criticalStock: 25,
      active: true,
    },
    {
      id: "prd-demo-003",
      sku: "SKU-2040",
      name: "Vasos biodegradables 12oz (pack 50)",
      description: "Pulpa de caña, compostable",
      category: "Insumos",
      barcode: "7801234567005",
      currentPrice: 8990,
      imageUrl: undefined,
      qrUrl: undefined,
      stock: 22,
      criticalStock: 30,
      active: true,
    },
    {
      id: "prd-demo-004",
      sku: "SKU-3055",
      name: "Botella vidrio reusable 600ml",
      description: "Incluye funda térmica",
      category: "Retail",
      barcode: "7801231234568",
      currentPrice: 7490,
      imageUrl: undefined,
      qrUrl: undefined,
      stock: 6,
      criticalStock: 15,
      active: false,
    },
  ],
  productPrices: {
    "prd-demo-001": [
      { id: "pp-001", productId: "prd-demo-001", price: 10990, validFrom: iso(daysAgo(90)) },
      { id: "pp-002", productId: "prd-demo-001", price: 11990, validFrom: iso(daysAgo(28)) },
    ],
    "prd-demo-002": [
      { id: "pp-003", productId: "prd-demo-002", price: 2990, validFrom: iso(daysAgo(120)) },
      { id: "pp-004", productId: "prd-demo-002", price: 3290, validFrom: iso(daysAgo(32)) },
    ],
    "prd-demo-003": [
      { id: "pp-005", productId: "prd-demo-003", price: 8990, validFrom: iso(daysAgo(62)) },
    ],
    "prd-demo-004": [
      { id: "pp-006", productId: "prd-demo-004", price: 7490, validFrom: iso(daysAgo(200)) },
    ],
  },
  suppliers: [
    {
      id: "sup-demo-001",
      name: "Proveedor Andes",
      rut: "76.555.111-1",
      businessActivity: "Distribución de alimentos",
      address: "Av. Los Leones 1200, Providencia",
      commune: "Providencia",
      phone: "+56 2 2456 7788",
      email: "ventas@proveedorandes.cl",
    },
    {
      id: "sup-demo-002",
      name: "Granos Latino",
      rut: "77.444.333-2",
      businessActivity: "Importación de granos",
      address: "Camino a Noviciado 2345, Pudahuel",
      commune: "Pudahuel",
      phone: "+56 2 2988 1100",
      email: "contacto@granoslatino.cl",
    },
    {
      id: "sup-demo-003",
      name: "Eco Packaging",
      rut: "79.222.888-5",
      businessActivity: "Envases sustentables",
      address: "Av. República 980, Santiago",
      commune: "Santiago",
      phone: "+56 2 2311 4455",
      email: "ventas@ecopackaging.cl",
    },
  ],
  customers: [
    {
      id: "cus-demo-001",
      name: "Cafetería Plaza",
      address: "Av. Providencia 1234, Santiago",
      lat: -33.4263,
      lng: -70.6209,
      phone: "+56 2 2345 6677",
      email: "contacto@cafeteriaplaza.cl",
      segment: "HORECA",
    },
    {
      id: "cus-demo-002",
      name: "MiniMarket Central",
      address: "Av. Alemania 890, Temuco",
      phone: "+56 45 212 3344",
      email: "compras@mmcentral.cl",
      segment: "Retail",
    },
    {
      id: "cus-demo-003",
      name: "Corporación Sur",
      address: "Los Carrera 450, Concepción",
      phone: "+56 41 276 0011",
      email: "abastecimiento@corsur.cl",
      segment: "Empresas",
    },
    {
      id: "cus-demo-004",
      name: "Coffeelab Boutique",
      address: "Av. Peru 345, Antofagasta",
      phone: "+56 55 284 9988",
      email: "ventas@coffeelab.cl",
      segment: "HORECA",
    },
    {
      id: "cus-demo-005",
      name: "Vecinos Gourmet",
      address: "Av. Italia 987, Santiago",
      phone: "+56 2 2567 7789",
      email: "hola@vecinosgourmet.cl",
    },
  ],
  sales: [
    {
      id: "sal-demo-001",
      customerId: "cus-demo-001",
      customerName: "Cafetería Plaza",
      docType: "Factura",
      paymentMethod: "transferencia",
      status: "emitida",
      net: 950000,
      vat: 180500,
      total: 1130500,
      issuedAt: iso(daysAgo(2)),
    },
    {
      id: "sal-demo-002",
      customerId: "cus-demo-004",
      customerName: "Coffeelab Boutique",
      docType: "Boleta",
      paymentMethod: "tarjeta",
      status: "emitida",
      net: 280000,
      vat: 53200,
      total: 333200,
      issuedAt: iso(daysAgo(4)),
    },
    {
      id: "sal-demo-003",
      customerId: "cus-demo-002",
      customerName: "MiniMarket Central",
      docType: "Factura",
      paymentMethod: "credito",
      status: "cancelled",
      net: 410000,
      vat: 77900,
      total: 487900,
      issuedAt: iso(daysAgo(9)),
    },
  ],
  saleDetails: {
    "sal-demo-001": {
      id: "sal-demo-001",
      issuedAt: iso(daysAgo(2)),
      docType: "Factura",
      paymentMethod: "transferencia",
      status: "emitida",
      customer: { id: "cus-demo-001", name: "Cafetería Plaza" },
      items: [
        {
          productId: "prd-demo-001",
          productName: "Caf en grano premium 1Kg",
          qty: 80,
          unitPrice: 11990,
          discount: 0,
          lineTotal: 959200,
        },
        {
          productId: "prd-demo-003",
          productName: "Vasos biodegradables 12oz (pack 50)",
          qty: 50,
          unitPrice: 8990,
          discount: 0,
          lineTotal: 449500,
        },
      ],
      net: 950000,
      vat: 180500,
      total: 1130500,
      thermalTicket:
        "PYMERP\nVenta sal-demo-001\nCliente: Cafetería Plaza\nTotal: $1.130.500\nGracias por su compra",
    },
    "sal-demo-002": {
      id: "sal-demo-002",
      issuedAt: iso(daysAgo(4)),
      docType: "Boleta",
      paymentMethod: "tarjeta",
      status: "emitida",
      customer: { id: "cus-demo-004", name: "Coffeelab Boutique" },
      items: [
        {
          productId: "prd-demo-001",
          productName: "Caf en grano premium 1Kg",
          qty: 20,
          unitPrice: 11990,
          discount: 0,
          lineTotal: 239800,
        },
        {
          productId: "prd-demo-002",
          productName: "Azcar orgánica 1Kg",
          qty: 40,
          unitPrice: 3290,
          discount: 0,
          lineTotal: 131600,
        },
      ],
      net: 280000,
      vat: 53200,
      total: 333200,
      thermalTicket: "PYMERP\nVenta sal-demo-002\nTotal: $333.200\nEmitida con POS",
    },
    "sal-demo-003": {
      id: "sal-demo-003",
      issuedAt: iso(daysAgo(9)),
      docType: "Factura",
      paymentMethod: "credito",
      status: "cancelled",
      customer: { id: "cus-demo-002", name: "MiniMarket Central" },
      items: [
        {
          productId: "prd-demo-003",
          productName: "Vasos biodegradables 12oz (pack 50)",
          qty: 25,
          unitPrice: 8990,
          discount: 0,
          lineTotal: 224750,
        },
        {
          productId: "prd-demo-002",
          productName: "Azcar orgánica 1Kg",
          qty: 60,
          unitPrice: 3290,
          discount: 0,
          lineTotal: 197400,
        },
      ],
      net: 410000,
      vat: 77900,
      total: 487900,
      thermalTicket: "PYMERP\nVenta sal-demo-003\nESTADO: Cancelada",
    },
  },
  purchases: [
    {
      id: "pur-demo-001",
      supplierId: "sup-demo-001",
      supplierName: "Proveedor Andes",
      docType: "Factura",
      docNumber: "F-4432",
      status: "received",
      net: 540000,
      vat: 102600,
      total: 642600,
      issuedAt: iso(daysAgo(6)),
    },
    {
      id: "pur-demo-002",
      supplierId: "sup-demo-003",
      supplierName: "Eco Packaging",
      docType: "Factura",
      docNumber: "F-8891",
      status: "received",
      net: 215000,
      vat: 40850,
      total: 255850,
      issuedAt: iso(daysAgo(11)),
    },
    {
      id: "pur-demo-003",
      supplierId: "sup-demo-002",
      supplierName: "Granos Latino",
      docType: "Factura",
      docNumber: "F-5501",
      status: "cancelled",
      net: 180000,
      vat: 34200,
      total: 214200,
      issuedAt: iso(daysAgo(18)),
    },
  ],
  inventorySummary: {
    totalValue: 1585000,
    activeProducts: 3,
    inactiveProducts: 1,
    totalProducts: 4,
    lowStockAlerts: 2,
    lowStockThreshold: 12,
  },
  inventoryAlerts: [
    {
      lotId: "lot-demo-001",
      productId: "prd-demo-002",
      qtyAvailable: 8,
      createdAt: iso(daysAgo(1)),
      expDate: iso(daysAgo(-60)),
    },
    {
      lotId: "lot-demo-002",
      productId: "prd-demo-003",
      qtyAvailable: 5,
      createdAt: iso(daysAgo(3)),
      expDate: iso(daysAgo(-120)),
    },
  ],
  inventorySettings: {
    lowStockThreshold: 12,
    updatedAt: iso(daysAgo(5)),
  },
  salesDaily: Array.from({ length: 14 }).map((_, index) => {
    const daysBack = 13 - index;
    const total = 350000 + (index % 5) * 42000;
    return {
      date: iso(daysAgo(daysBack)).slice(0, 10),
      total,
      count: 4 + (index % 3),
    };
  }),
  purchaseDaily: Array.from({ length: 14 }).map((_, index) => {
    const daysBack = 13 - index;
    const total = 220000 + (index % 4) * 38000;
    return {
      date: iso(daysAgo(daysBack)).slice(0, 10),
      total,
      count: 2 + (index % 2),
    };
  }),
};

function nextId(prefix: string): string {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeString(value?: string | null) {
  return value?.toLowerCase().normalize("NFD").replace(/\p{Diacritic}/gu, "") ?? "";
}

function matchesQuery(target: string | undefined | null, query: string) {
  if (!query) {
    return true;
  }
  if (!target) {
    return false;
  }
  return normalizeString(target).includes(query);
}

function paginate<T>(items: T[], page = 0, size = 20): Page<T> {
  const safeSize = size > 0 ? size : 20;
  const start = page * safeSize;
  const content = items.slice(start, start + safeSize);
  const totalElements = items.length;
  const totalPages = safeSize > 0 ? Math.ceil(totalElements / safeSize) : 0;
  return {
    content,
    totalElements,
    totalPages,
    size: safeSize,
    number: page,
    hasNext: totalPages > 0 ? page + 1 < totalPages : false,
  } as Page<T> & { hasNext: boolean };
}

function withOfflineFallback<T>(context: string, operation: () => Promise<T>, fallback: () => T): Promise<T> {
  if (offlineModeEnabled) {
    return Promise.resolve(maybeClone(fallback()));
  }
  return operation().catch((error) => {
    if (isNetworkError(error)) {
      markOffline(context);
      return maybeClone(fallback());
    }
    throw error;
  });
}

function withOfflineVoid(context: string, operation: () => Promise<void>, fallback: () => void): Promise<void> {
  if (offlineModeEnabled) {
    fallback();
    return Promise.resolve();
  }
  return operation().catch((error) => {
    if (isNetworkError(error)) {
      markOffline(context);
      fallback();
      return;
    }
    throw error;
  });
}

function recalcInventorySummary() {
  const activeProducts = demoState.products.filter((product) => product.active).length;
  demoState.inventorySummary = {
    totalValue: Number(demoState.inventorySummary.totalValue) || 0,
    activeProducts,
    inactiveProducts: demoState.products.length - activeProducts,
    totalProducts: demoState.products.length,
    lowStockAlerts: demoState.inventoryAlerts.length,
    lowStockThreshold: demoState.inventorySettings.lowStockThreshold,
  };
}

const fallbackHealth: HealthResponse = {
  status: "UP",
  components: { database: { status: "UP" }, disk: { status: "UP", free: "32GB" } },
  details: {
    environment: "demo",
    lastSync: iso(daysAgo(0)),
  },
};

function fallbackListCompanies() {
  return demoState.companies;
}

function fallbackCreateCompany(payload: CreateCompanyPayload): Company {
  const nowIso = new Date().toISOString();
  const company: Company = {
    id: nextId("cmp-demo"),
    businessName: payload.businessName,
    rut: payload.rut,
    businessActivity: payload.businessActivity,
    address: payload.address,
    commune: payload.commune,
    phone: payload.phone,
    email: payload.email?.toLowerCase(),
    receiptFooterMessage: payload.receiptFooterMessage,
    createdAt: nowIso,
    updatedAt: nowIso,
  };
  demoState.companies = [...demoState.companies, company];
  return company;
}

function fallbackUpdateCompany(id: string, payload: UpdateCompanyPayload): Company {
  const index = demoState.companies.findIndex((company) => company.id === id);
  if (index === -1) {
    throw new Error("Company not found");
  }
  const existing = demoState.companies[index];
  const updated: Company = {
    ...existing,
    businessName: payload.businessName,
    rut: payload.rut,
    businessActivity: payload.businessActivity,
    address: payload.address,
    commune: payload.commune,
    phone: payload.phone,
    email: payload.email?.toLowerCase(),
    receiptFooterMessage: payload.receiptFooterMessage,
    updatedAt: new Date().toISOString(),
  };
  demoState.companies = [
    ...demoState.companies.slice(0, index),
    updated,
    ...demoState.companies.slice(index + 1),
  ];
  return updated;
}

function fallbackDeleteCompany(id: string) {
  demoState.companies = demoState.companies.filter((company) => company.id !== id);
}

function fallbackListProducts(params?: { q?: string; page?: number; size?: number; status?: "active" | "inactive" | "all" }) {
  const query = normalizeString(params?.q ?? "");
  const status = params?.status ?? "active";
  const filtered = demoState.products.filter((product) => {
    if (status !== "all" && product.active !== (status === "active")) {
      return false;
    }
    if (!query) {
      return true;
    }
    return [product.name, product.sku, product.barcode, product.description].some((value) => matchesQuery(value, query));
  });
  const page = params?.page ?? 0;
  const size = params?.size ?? 20;
  return paginate(filtered, page, size);
}

function fallbackCreateProduct(payload: ProductPayload): Product {
  const product: Product = {
    id: nextId("prd-demo"),
    sku: payload.sku || `SKU-${Math.floor(Math.random() * 9999)}`,
    name: payload.name,
    description: payload.description,
    category: payload.category,
    barcode: payload.barcode,
    imageUrl: payload.imageUrl,
    qrUrl: undefined,
    stock: 0,
    criticalStock: 0,
    currentPrice: 0,
    active: true,
  };
  demoState.products = [product, ...demoState.products];
  demoState.productPrices[product.id] = [];
  recalcInventorySummary();
  return product;
}

function fallbackUpdateProduct(id: string, payload: ProductPayload): Product {
  const index = demoState.products.findIndex((product) => product.id === id);
  if (index === -1) {
    throw new Error("Producto no encontrado");
  }
  const updated: Product = {
    ...demoState.products[index],
    ...payload,
  };
  demoState.products = demoState.products.map((product, i) => (i === index ? updated : product));
  recalcInventorySummary();
  return updated;
}

function fallbackUpdateProductStatus(id: string, active: boolean): Product {
  const index = demoState.products.findIndex((product) => product.id === id);
  if (index === -1) {
    throw new Error("Producto no encontrado");
  }
  const updated: Product = { ...demoState.products[index], active };
  demoState.products = demoState.products.map((product, i) => (i === index ? updated : product));
  recalcInventorySummary();
  return updated;
}

function fallbackDeleteProduct(id: string) {
  demoState.products = demoState.products.filter((product) => product.id !== id);
  delete demoState.productPrices[id];
  recalcInventorySummary();
}

function cleanupOptional(value?: string | null): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function toProductPayload(form: ProductFormData): ProductPayload {
  const payload: ProductPayload = {
    sku: form.sku.trim(),
    name: form.name.trim(),
    description: cleanupOptional(form.description),
    category: cleanupOptional(form.category),
    barcode: cleanupOptional(form.barcode),
  };
  if (form.imageFile) {
    payload.imageUrl = null;
  } else if (form.imageUrl === null) {
    payload.imageUrl = null;
  } else if (typeof form.imageUrl === "string") {
    const trimmed = form.imageUrl.trim();
    payload.imageUrl = trimmed.length > 0 ? trimmed : null;
  }
  return payload;
}

function buildProductFormData(payload: ProductPayload, imageFile?: File | null): FormData {
  const body = new FormData();
  body.append(
    "product",
    new Blob([JSON.stringify(payload)], {
      type: "application/json",
    }),
  );
  if (imageFile instanceof File) {
    body.append("image", imageFile);
  }
  return body;
}

function fallbackProductStock(productId: string): ProductStock {
  const lots: ProductStockLot[] = [
    {
      lotId: `${productId}-lot-1`,
      quantity: 42,
      location: "Bodega central",
      expiresAt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 120).toISOString(),
    },
    {
      lotId: `${productId}-lot-2`,
      quantity: 18,
      location: "Tienda principal",
      expiresAt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 45).toISOString(),
    },
  ];
  const total = lots.reduce((sum, lot) => sum + lot.quantity, 0);
  const productIndex = demoState.products.findIndex((product) => product.id === productId);
  if (productIndex !== -1) {
    demoState.products = demoState.products.map((product, index) =>
      index === productIndex ? { ...product, stock: total } : product,
    );
  }
  return { productId, total, lots };
}

function fallbackUpdateProductInventoryAlert(id: string, criticalStock: number): Product {
  const index = demoState.products.findIndex((product) => product.id === id);
  if (index === -1) {
    throw new Error("Producto no encontrado");
  }
  const updated: Product = {
    ...demoState.products[index],
    criticalStock,
  };
  demoState.products = demoState.products.map((product, i) => (i === index ? updated : product));
  return updated;
}

function fallbackFetchProductQrBlob(productId: string): Blob {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
    <rect width="120" height="120" fill="#0b0d17" />
    <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#ffffff" font-size="18">${productId.slice(-4)}</text>
  </svg>`;
  return new Blob([svg], { type: "image/svg+xml" });
}

function fallbackListProductPrices(productId: string, params?: { page?: number; size?: number }) {
  const entries = ensureArray(demoState.productPrices[productId]).sort((a, b) =>
    b.validFrom.localeCompare(a.validFrom),
  );
  return paginate(entries, params?.page ?? 0, params?.size ?? entries.length);
}

function fallbackCreateProductPrice(productId: string, payload: PriceChangePayload): PriceHistoryEntry {
  const entry: PriceHistoryEntry = {
    id: nextId("pp-demo"),
    productId,
    price: payload.price,
    validFrom: payload.validFrom ?? new Date().toISOString(),
  };
  const prices = ensureArray(demoState.productPrices[productId]);
  demoState.productPrices[productId] = [entry, ...prices];
  const index = demoState.products.findIndex((product) => product.id === productId);
  if (index !== -1) {
    const updated = { ...demoState.products[index], currentPrice: payload.price };
    demoState.products = demoState.products.map((product, i) => (i === index ? updated : product));
  }
  return entry;
}

function fallbackListSuppliers() {
  return demoState.suppliers;
}

function fallbackCreateSupplier(payload: SupplierPayload): Supplier {
  const supplier: Supplier = {
    id: nextId("sup-demo"),
    name: payload.name,
    rut: payload.rut,
    address: payload.address ?? null,
    commune: payload.commune ?? null,
    businessActivity: payload.businessActivity ?? null,
    phone: payload.phone ?? null,
    email: payload.email ?? null,
  };
  demoState.suppliers = [...demoState.suppliers, supplier];
  return supplier;
}

function fallbackUpdateSupplier(id: string, payload: SupplierPayload): Supplier {
  const index = demoState.suppliers.findIndex((supplier) => supplier.id === id);
  if (index === -1) {
    throw new Error("Proveedor no encontrado");
  }
  const updated: Supplier = { ...demoState.suppliers[index], ...payload };
  demoState.suppliers = demoState.suppliers.map((supplier, i) => (i === index ? updated : supplier));
  return updated;
}

function fallbackDeleteSupplier(id: string) {
  demoState.suppliers = demoState.suppliers.filter((supplier) => supplier.id !== id);
}

function fallbackListCustomers(params: ListCustomersParams = {}): Page<Customer> {
  const query = normalizeString(params.q ?? "");
  const segment = params.segment?.trim();
  const filtered = demoState.customers.filter((customer) => {
    if (segment && segment.length > 0) {
      if (segment === UNASSIGNED_SEGMENT_CODE) {
        if (customer.segment && customer.segment.trim()) {
          return false;
        }
      } else if (normalizeString(customer.segment) !== normalizeString(segment)) {
        return false;
      }
    }
    if (!query) {
      return true;
    }
    return [customer.name, customer.email, customer.phone, customer.address].some((value) => matchesQuery(value, query));
  });
  return paginate(filtered, params.page ?? 0, params.size ?? 20);
}

function fallbackCreateCustomer(payload: CustomerPayload): Customer {
  const customer: Customer = {
    id: nextId("cus-demo"),
    name: payload.name,
    address: payload.address,
    lat: payload.lat ?? null,
    lng: payload.lng ?? null,
    phone: payload.phone,
    email: payload.email,
    segment: payload.segment,
  };
  demoState.customers = [customer, ...demoState.customers];
  return customer;
}

function fallbackUpdateCustomer(id: string, payload: CustomerPayload): Customer {
  const index = demoState.customers.findIndex((customer) => customer.id === id);
  if (index === -1) {
    throw new Error("Cliente no encontrado");
  }
  const updated: Customer = {
    ...demoState.customers[index],
    ...payload,
    lat: payload.lat ?? null,
    lng: payload.lng ?? null,
  };
  demoState.customers = demoState.customers.map((customer, i) => (i === index ? updated : customer));
  return updated;
}

function fallbackDeleteCustomer(id: string) {
  demoState.customers = demoState.customers.filter((customer) => customer.id !== id);
}

function fallbackListCustomerSegments(): CustomerSegmentSummary[] {
  const summary = new Map<string, CustomerSegmentSummary>();
  for (const customer of demoState.customers) {
    const code = customer.segment && customer.segment.trim() ? customer.segment : UNASSIGNED_SEGMENT_CODE;
    const normalizedCode = code;
    if (!summary.has(normalizedCode)) {
      summary.set(normalizedCode, {
        code: normalizedCode,
        segment: code === UNASSIGNED_SEGMENT_CODE ? "Sin segmentar" : customer.segment ?? "Sin segmentar",
        total: 0,
      });
    }
    summary.get(normalizedCode)!.total += 1;
  }
  return Array.from(summary.values());
}

function generateSaleThermalTicket(sale: SaleDetail): string {
  return [
    "PYMERP DEMO",
    `Documento: ${sale.docType}`,
    `Venta: ${sale.id}`,
    `Cliente: ${sale.customer?.name ?? "Mostrador"}`,
    `Total: $${Number(sale.total).toLocaleString("es-CL")}`,
    "Gracias por utilizar el modo demo",
  ].join("\n");
}

function fallbackListSales(params: ListSalesParams = {}): Page<SaleSummary> {
  const query = normalizeString(params.search ?? "");
  const filtered = demoState.sales.filter((sale) => {
    if (params.status && params.status.trim()) {
      if (normalizeString(sale.status) !== normalizeString(params.status)) {
        return false;
      }
    }
    if (params.docType && params.docType.trim()) {
      if (normalizeString(sale.docType) !== normalizeString(params.docType)) {
        return false;
      }
    }
    if (params.paymentMethod && params.paymentMethod.trim()) {
      if (normalizeString(sale.paymentMethod) !== normalizeString(params.paymentMethod)) {
        return false;
      }
    }
    if (params.from && sale.issuedAt < params.from) {
      return false;
    }
    if (params.to && sale.issuedAt > params.to) {
      return false;
    }
    if (!query) {
      return true;
    }
    return [sale.docType, sale.paymentMethod, sale.customerName, sale.customerId, sale.id]
      .filter(Boolean)
      .some((value) => matchesQuery(String(value), query));
  });
  return paginate(filtered, params.page ?? 0, params.size ?? 20);
}

function fallbackGetSaleDetail(id: string): SaleDetail {
  const detail = demoState.saleDetails[id];
  if (!detail) {
    throw new Error("Venta no encontrada");
  }
  return detail;
}

function fallbackCreateSale(payload: SalePayload): SaleRes {
  const id = nextId("sal-demo");
  const total = payload.items.reduce((acc, item) => acc + item.qty * item.unitPrice - (item.discount ?? 0), 0);
  const vat = Math.round(total * 0.19);
  const net = total - vat;
  const summary: SaleSummary = {
    id,
    customerId: payload.customerId,
    customerName: demoState.customers.find((customer) => customer.id === payload.customerId)?.name,
    docType: payload.docType,
    paymentMethod: payload.paymentMethod ?? "transferencia",
    status: "emitida",
    net,
    vat,
    total,
    issuedAt: new Date().toISOString(),
  };
  demoState.sales = [summary, ...demoState.sales];
  const detail: SaleDetail = {
    ...summary,
    customer: summary.customerId
      ? { id: summary.customerId, name: summary.customerName ?? "Cliente" }
      : null,
    items: payload.items.map((item) => ({
      productId: item.productId,
      productName: demoState.products.find((product) => product.id === item.productId)?.name ?? item.productId,
      qty: item.qty,
      unitPrice: item.unitPrice,
      discount: item.discount ?? 0,
      lineTotal: item.qty * item.unitPrice - (item.discount ?? 0),
    })),
    thermalTicket: "",
  };
  detail.thermalTicket = generateSaleThermalTicket(detail);
  demoState.saleDetails[id] = detail as DemoSaleDetail;
  return {
    id,
    customerId: summary.customerId,
    customerName: summary.customerName,
    status: summary.status,
    net: summary.net,
    vat: summary.vat,
    total: summary.total,
    issuedAt: summary.issuedAt,
    docType: summary.docType ?? "Factura",
    paymentMethod: summary.paymentMethod ?? "transferencia",
  };
}

function fallbackUpdateSale(id: string, payload: SaleUpdatePayload): SaleRes {
  const index = demoState.sales.findIndex((sale) => sale.id === id);
  if (index === -1) {
    throw new Error("Venta no encontrada");
  }
  const updated: SaleSummary = { ...demoState.sales[index], ...payload };
  demoState.sales = demoState.sales.map((sale, i) => (i === index ? updated : sale));
  const detail = demoState.saleDetails[id];
  if (detail) {
    demoState.saleDetails[id] = {
      ...detail,
      docType: updated.docType ?? detail.docType,
      paymentMethod: updated.paymentMethod ?? detail.paymentMethod,
      status: updated.status ?? detail.status,
      thermalTicket: generateSaleThermalTicket({ ...detail, ...updated }),
    };
  }
  return {
    id: updated.id,
    customerId: updated.customerId,
    customerName: updated.customerName,
    status: updated.status,
    net: updated.net,
    vat: updated.vat,
    total: updated.total,
    issuedAt: updated.issuedAt,
    docType: updated.docType ?? "Factura",
    paymentMethod: updated.paymentMethod ?? "transferencia",
  };
}

function fallbackCancelSale(id: string): SaleRes {
  return fallbackUpdateSale(id, { status: "cancelled" });
}

function fallbackListSalesDaily(days = 14): SalesDailyPoint[] {
  return demoState.salesDaily.slice(-days);
}

function parseWindowToDays(window: string): number {
  const trimmed = window.trim();
  const match = /^(\d+)\s*d$/i.exec(trimmed);
  if (!match) {
    return 14;
  }
  const parsed = Number.parseInt(match[1], 10);
  return Number.isNaN(parsed) ? 14 : Math.max(1, parsed);
}

function fallbackListSalesTrend({ from, to }: SalesTrendParams): SalesDailyPoint[] {
  if (!from || !to || from > to) {
    return [];
  }
  return demoState.salesDaily.filter((point) => point.date >= from && point.date <= to);
}

function fallbackGetSalesWindowMetrics(window: string): SalesWindowMetrics {
  const days = parseWindowToDays(window);
  const data = fallbackListSalesDaily(days);
  const totalWithTax = data.reduce((acc, point) => acc + (point.total ?? 0), 0);
  const documentCount = data.reduce((acc, point) => acc + (point.count ?? 0), 0);
  const dailyAverage = days > 0 ? totalWithTax / days : 0;
  return {
    window,
    totalWithTax,
    dailyAverage,
    documentCount,
  };
}

function fallbackGetSalesSummary(period: SalesPeriod): SalesPeriodSummary {
  const now = new Date();
  const end = new Date(now);
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  switch (period) {
    case "week":
      start.setDate(start.getDate() - 6);
      break;
    case "month":
      start.setDate(1);
      break;
    default:
      break;
  }
  const endTime = end.getTime();
  const startTime = start.getTime();
  const filtered = demoState.sales.filter((sale) => {
    if (!sale.issuedAt) {
      return false;
    }
    if (sale.status?.toLowerCase() === "cancelled") {
      return false;
    }
    const issuedTime = new Date(sale.issuedAt).getTime();
    if (Number.isNaN(issuedTime)) {
      return false;
    }
    return issuedTime >= startTime && issuedTime <= endTime;
  });
  const total = filtered.reduce((acc, sale) => acc + (sale.total ?? 0), 0);
  const net = filtered.reduce((acc, sale) => acc + (sale.net ?? 0), 0);
  return {
    period,
    total,
    net,
    count: filtered.length,
  };
}

function fallbackListDocumentsGrouped(params: ListDocumentsParams = {}): DocumentsGroupedResponse {
  const size = params.size && params.size > 0 ? params.size : 10;
  const salesPage = params.salesPage ?? 0;
  const purchasesPage = params.purchasesPage ?? 0;
  const salesDocuments = [...demoState.sales]
    .map((sale) => ({
      id: sale.id,
      direction: "sales" as const,
      type: sale.docType ?? "Documento",
      number: sale.id,
      issuedAt: sale.issuedAt,
      total: sale.total,
      status: sale.status,
    }))
    .sort((a, b) => (b.issuedAt ?? "").localeCompare(a.issuedAt ?? ""));
  const purchaseDocuments = [...demoState.purchases]
    .map((purchase) => ({
      id: purchase.id,
      direction: "purchases" as const,
      type: purchase.docType ?? "Documento",
      number: purchase.docNumber ?? purchase.id,
      issuedAt: purchase.issuedAt,
      total: purchase.total,
      status: purchase.status,
    }))
    .sort((a, b) => (b.issuedAt ?? "").localeCompare(a.issuedAt ?? ""));
  return {
    sales: paginate(salesDocuments, salesPage, size),
    purchases: paginate(purchaseDocuments, purchasesPage, size),
  };
}

function fallbackGetDocumentPreview(id: string): DocumentFile {
  return createFallbackDocumentFile(id, "preview");
}

function fallbackDownloadDocument(id: string): DocumentFile {
  return createFallbackDocumentFile(id, "download");
}

function fallbackListPurchases(params: ListPurchasesParams = {}): Page<PurchaseSummary> {
  const query = normalizeString(params.search ?? "");
  const filtered = demoState.purchases.filter((purchase) => {
    if (params.status && params.status.trim()) {
      if (normalizeString(purchase.status) !== normalizeString(params.status)) {
        return false;
      }
    }
    if (params.docType && params.docType.trim()) {
      if (normalizeString(purchase.docType) !== normalizeString(params.docType)) {
        return false;
      }
    }
    if (params.from && purchase.issuedAt < params.from) {
      return false;
    }
    if (params.to && purchase.issuedAt > params.to) {
      return false;
    }
    if (!query) {
      return true;
    }
    return [purchase.docNumber, purchase.supplierName, purchase.supplierId].some((value) => matchesQuery(value, query));
  });
  return paginate(filtered, params.page ?? 0, params.size ?? 20);
}

function fallbackUpdatePurchase(id: string, payload: PurchaseUpdatePayload): PurchaseSummary {
  const index = demoState.purchases.findIndex((purchase) => purchase.id === id);
  if (index === -1) {
    throw new Error("Compra no encontrada");
  }
  const updated: PurchaseSummary = { ...demoState.purchases[index], ...payload };
  demoState.purchases = demoState.purchases.map((purchase, i) => (i === index ? updated : purchase));
  return updated;
}

function fallbackCancelPurchase(id: string): PurchaseSummary {
  return fallbackUpdatePurchase(id, { status: "cancelled" });
}

function fallbackCreatePurchase(payload: PurchasePayload): { id: string } {
  const id = nextId("pur-demo");
  const summary: PurchaseSummary = {
    id,
    supplierId: payload.supplierId,
    supplierName: demoState.suppliers.find((supplier) => supplier.id === payload.supplierId)?.name,
    docType: payload.docType,
    docNumber: payload.docNumber,
    status: "received",
    net: payload.net,
    vat: payload.vat,
    total: payload.total,
    issuedAt: payload.issuedAt,
  };
  demoState.purchases = [summary, ...demoState.purchases];
  return { id };
}

function fallbackListPurchaseDaily(days = 14): PurchaseDailyPoint[] {
  return demoState.purchaseDaily.slice(-days);
}

function fallbackListInventoryAlerts(threshold?: number): InventoryAlert[] {
  if (typeof threshold === "number") {
    return demoState.inventoryAlerts.filter((alert) => Number(alert.qtyAvailable) <= threshold);
  }
  return demoState.inventoryAlerts;
}

function fallbackGetInventorySummary(): InventorySummary {
  recalcInventorySummary();
  return demoState.inventorySummary;
}

function fallbackGetInventorySettings(): InventorySettings {
  return demoState.inventorySettings;
}

function fallbackUpdateInventorySettings(payload: { lowStockThreshold: number }): InventorySettings {
  demoState.inventorySettings = {
    lowStockThreshold: payload.lowStockThreshold,
    updatedAt: new Date().toISOString(),
  };
  recalcInventorySummary();
  return demoState.inventorySettings;
}

function fallbackCreateInventoryAdjustment(payload: InventoryAdjustmentPayload): InventoryAdjustmentResponse {
  const direction = payload.direction ?? "increase";
  const quantity = direction === "decrease" ? -Math.abs(payload.quantity) : Math.abs(payload.quantity);
  const alertIndex = demoState.inventoryAlerts.findIndex((alert) => alert.productId === payload.productId);
  if (alertIndex !== -1) {
    const alert = demoState.inventoryAlerts[alertIndex];
    const updatedQty = Number(alert.qtyAvailable) + quantity;
    demoState.inventoryAlerts[alertIndex] = {
      ...alert,
      qtyAvailable: updatedQty,
      createdAt: new Date().toISOString(),
      expDate: payload.expDate ?? alert.expDate ?? null,
    };
  } else if (direction === "decrease") {
    demoState.inventoryAlerts = [
      ...demoState.inventoryAlerts,
      {
        lotId: nextId("lot-demo"),
        productId: payload.productId,
        qtyAvailable: Math.max(0, payload.quantity),
        createdAt: new Date().toISOString(),
        expDate: payload.expDate ?? null,
      },
    ];
  }
  recalcInventorySummary();
  return {
    productId: payload.productId,
    appliedQuantity: payload.quantity,
    direction,
  };
}

function isNetworkError(error: unknown): boolean {
  return axios.isAxiosError(error) && !error.response;
}

function createDevFallbackResponse(): LoginResponse {
  return {
    token: DEV_FALLBACK_TOKEN,
    expiresIn: 86_400,
    refreshToken: DEV_FALLBACK_REFRESH_TOKEN,
    refreshExpiresIn: 2_592_000,
    companyId: DEV_FALLBACK_COMPANY_ID,
    email: DEV_FALLBACK_EMAIL,
    name: "Administrador Demo",
    roles: [...DEV_FALLBACK_ROLES],
    modules: [...DEV_FALLBACK_MODULES],
  };
}

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
  businessName: string;
  rut: string;
  businessActivity?: string;
  address?: string;
  commune?: string;
  phone?: string;
  email?: string;
  receiptFooterMessage?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type Product = {
  id: string;
  sku: string;
  name: string;
  description?: string;
  category?: string;
  barcode?: string;
  imageUrl?: string | null;
  qrUrl?: string;
  stock?: number | string | null;
  criticalStock?: number | string;
  currentPrice?: number | string;
  active: boolean;
};

export type ProductPayload = {
  sku: string;
  name: string;
  description?: string;
  category?: string;
  barcode?: string;
  imageUrl?: string | null;
};

export type ProductFormData = ProductPayload & {
  imageFile?: File | null;
};

export type ProductStockLot = {
  lotId: string;
  quantity: number;
  location?: string | null;
  expiresAt?: string | null;
};

export type ProductStock = {
  productId: string;
  total: number;
  lots: ProductStockLot[];
};

export type Supplier = {
  id: string;
  name: string;
  rut?: string;
  address?: string | null;
  commune?: string | null;
  businessActivity?: string | null;
  phone?: string | null;
  email?: string | null;
};

export type SupplierPayload = {
  name: string;
  rut: string;
  address?: string | null;
  commune?: string | null;
  businessActivity?: string | null;
  phone?: string | null;
  email?: string | null;
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

export type CompanyPayload = {
  businessName: string;
  rut: string;
  businessActivity?: string;
  address?: string;
  commune?: string;
  phone?: string;
  email?: string;
  receiptFooterMessage?: string;
};

export type CreateCompanyPayload = CompanyPayload;

export type UpdateCompanyPayload = CompanyPayload;

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

export type SimpleCaptchaPayload = {
  a: number;
  b: number;
  answer: string;
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

export type AccountRequestPayload = {
  rut: string;
  fullName: string;
  address: string;
  email: string;
  companyName: string;
  password: string;
  confirmPassword: string;
  captcha: SimpleCaptchaPayload;
};

export type AccountRequestResponse = {
  id: string;
  status: string;
  createdAt: string;
  message: string;
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
  tax?: number;
  lineTotal: number;
};

export type SaleDetail = {
  id: string;
  issuedAt: string;
  docType: string;
  paymentMethod: string;
  status: string;
  customer?: { id: string; name: string } | null;
  supplier?: { id: string; name: string } | null;
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

export type SalesTrendParams = {
  from: string;
  to: string;
};

export type SalesWindowMetrics = {
  window: string;
  totalWithTax: number;
  dailyAverage: number;
  documentCount: number;
};

export type SalesPeriod = "today" | "week" | "month";

export type SalesPeriodSummary = {
  period: SalesPeriod;
  total: number;
  net: number;
  count: number;
};

export type ListDocumentsParams = {
  salesPage?: number;
  purchasesPage?: number;
  size?: number;
};

export type DocumentSummary = {
  id: string;
  direction: "sales" | "purchases";
  type: string;
  number?: string;
  issuedAt?: string;
  total: number;
  status: string;
};

export type DocumentsGroupedResponse = {
  sales: Page<DocumentSummary>;
  purchases: Page<DocumentSummary>;
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
  try {
    const { data } = await api.post<LoginResponse>("/v1/auth/login", payload);
    setSession({ token: data.token, companyId: data.companyId, refreshToken: data.refreshToken });
    return data;
  } catch (error) {
    if (isNetworkError(error) && payload.email === DEV_FALLBACK_EMAIL && payload.password === DEV_FALLBACK_PASSWORD) {
      const fallback = createDevFallbackResponse();
      console.warn("Login API unreachable. Using local development session.");
      setSession({ token: fallback.token, companyId: fallback.companyId, refreshToken: fallback.refreshToken });
      return fallback;
    }
    throw error;
  }
}

export async function refreshAuth(payload: RefreshPayload): Promise<LoginResponse> {
  if (!payload.refreshToken) {
    throw new Error("Refresh token is required");
  }
  try {
    const { data } = await api.post<LoginResponse>("/v1/auth/refresh", null, {
      headers: { ["X-Refresh-Token"]: payload.refreshToken },
    });
    setSession({ token: data.token, companyId: data.companyId, refreshToken: data.refreshToken });
    return data;
  } catch (error) {
    if (isNetworkError(error) && payload.refreshToken === DEV_FALLBACK_REFRESH_TOKEN) {
      const fallback = createDevFallbackResponse();
      console.warn("Refresh API unreachable. Keeping local development session active.");
      setSession({ token: fallback.token, companyId: fallback.companyId, refreshToken: fallback.refreshToken });
      return fallback;
    }
    throw error;
  }
}

export async function submitAccountRequest(payload: AccountRequestPayload): Promise<AccountRequestResponse> {
  const body = {
    rut: payload.rut,
    fullName: payload.fullName,
    address: payload.address,
    email: payload.email,
    companyName: payload.companyName,
    password: payload.password,
    confirmPassword: payload.confirmPassword,
    captcha: payload.captcha,
  };
  const { data } = await api.post<AccountRequestResponse>("/v1/requests", body);
  return data;
}

export function getCurrentRefreshToken() {
  return currentRefreshToken;
}

export function fetchHealth(): Promise<HealthResponse> {
  return withOfflineFallback(
    "fetchHealth",
    async () => {
      const { data } = await api.get<HealthResponse>("/actuator/health");
      return data;
    },
    () => fallbackHealth,
  );
}

export function listCompanies(): Promise<Company[]> {
  return withOfflineFallback(
    "listCompanies",
    async () => {
      const { data } = await api.get<Company[]>("/v1/companies");
      return data;
    },
    () => fallbackListCompanies(),
  );
}

export function createCompany(payload: CreateCompanyPayload): Promise<Company> {
  return withOfflineFallback(
    "createCompany",
    async () => {
      const { data } = await api.post<Company>("/v1/companies", payload);
      return data;
    },
    () => fallbackCreateCompany(payload),
  );
}

export function updateCompany(id: string, payload: UpdateCompanyPayload): Promise<Company> {
  return withOfflineFallback(
    "updateCompany",
    async () => {
      const { data } = await api.put<Company>(`/v1/companies/${id}`, payload);
      return data;
    },
    () => fallbackUpdateCompany(id, payload),
  );
}

export function deleteCompany(id: string): Promise<void> {
  return withOfflineVoid(
    "deleteCompany",
    async () => {
      await api.delete(`/v1/companies/${id}`);
    },
    () => fallbackDeleteCompany(id),
  );
}

export function listProducts(
  params?: { q?: string; page?: number; size?: number; status?: "active" | "inactive" | "all" },
): Promise<Page<Product>> {
  return withOfflineFallback(
    "listProducts",
    async () => {
      const { data } = await api.get<Page<Product>>(`/v1/products`, { params });
      return data;
    },
    () => fallbackListProducts(params),
  );
}

export function createProduct(form: ProductFormData): Promise<Product> {
  const payload = toProductPayload(form);
  return withOfflineFallback(
    "createProduct",
    async () => {
      const body = buildProductFormData(payload, form.imageFile ?? null);
      const { data } = await api.post<Product>("/v1/products", body, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return data;
    },
    () => fallbackCreateProduct(payload),
  );
}

export function updateProduct(id: string, form: ProductFormData): Promise<Product> {
  const payload = toProductPayload(form);
  return withOfflineFallback(
    "updateProduct",
    async () => {
      const body = buildProductFormData(payload, form.imageFile ?? null);
      const { data } = await api.put<Product>(`/v1/products/${id}`, body, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return data;
    },
    () => fallbackUpdateProduct(id, payload),
  );
}

export function updateProductStatus(id: string, active: boolean): Promise<Product> {
  return withOfflineFallback(
    "updateProductStatus",
    async () => {
      const { data } = await api.patch<Product>(`/v1/products/${id}/status`, { active });
      return data;
    },
    () => fallbackUpdateProductStatus(id, active),
  );
}

export function fetchProductStock(id: string): Promise<ProductStock> {
  return withOfflineFallback(
    "fetchProductStock",
    async () => {
      const { data } = await api.get<ProductStock>(`/v1/products/${id}/stock`);
      return data;
    },
    () => fallbackProductStock(id),
  );
}

export function updateProductInventoryAlert(id: string, criticalStock: number): Promise<Product> {
  return withOfflineFallback(
    "updateProductInventoryAlert",
    async () => {
      const { data } = await api.patch<Product>(`/v1/products/${id}/inventory-alert`, { criticalStock });
      return data;
    },
    () => fallbackUpdateProductInventoryAlert(id, criticalStock),
  );
}

export function fetchProductQrBlob(id: string, options?: { download?: boolean }): Promise<Blob> {
  return withOfflineFallback(
    "fetchProductQrBlob",
    async () => {
      const { data } = await api.get<Blob>(`/v1/products/${id}/qr`, {
        responseType: "blob",
        params: options?.download ? { download: true } : undefined,
      });
      return data;
    },
    () => fallbackFetchProductQrBlob(id),
  );
}

export function deleteProduct(id: string): Promise<void> {
  return withOfflineVoid(
    "deleteProduct",
    () => api.delete(`/v1/products/${id}`),
    () => fallbackDeleteProduct(id),
  );
}

export function listSuppliers(): Promise<Supplier[]> {
  return withOfflineFallback(
    "listSuppliers",
    async () => {
      const { data } = await api.get<Supplier[]>("/v1/suppliers");
      return data;
    },
    () => fallbackListSuppliers(),
  );
}

export function createSupplier(payload: SupplierPayload): Promise<Supplier> {
  return withOfflineFallback(
    "createSupplier",
    async () => {
      const { data } = await api.post<Supplier>("/v1/suppliers", payload);
      return data;
    },
    () => fallbackCreateSupplier(payload),
  );
}

export function updateSupplier(id: string, payload: SupplierPayload): Promise<Supplier> {
  return withOfflineFallback(
    "updateSupplier",
    async () => {
      const { data } = await api.put<Supplier>(`/v1/suppliers/${id}`, payload);
      return data;
    },
    () => fallbackUpdateSupplier(id, payload),
  );
}

export function deleteSupplier(id: string): Promise<void> {
  return withOfflineVoid(
    "deleteSupplier",
    () => api.delete(`/v1/suppliers/${id}`),
    () => fallbackDeleteSupplier(id),
  );
}

export function listCustomers(params: ListCustomersParams = {}): Promise<Page<Customer>> {
  return withOfflineFallback(
    "listCustomers",
    async () => {
      const requestedPage = params.page ?? 0;
      const requestedSize = params.size ?? 20;
      const queryParams: Record<string, unknown> = {
        page: requestedPage,
        size: requestedSize,
        sort: params.sort ?? "createdAt,desc",
      };
      if (params.q && params.q.trim()) {
        queryParams.q = params.q.trim();
      }
      if (params.segment && params.segment.trim()) {
        queryParams.segment = params.segment.trim();
      }
      const { data } = await api.get<Page<Customer>>("/v1/customers", { params: queryParams });
      const raw = data as Page<Customer> & { page?: number; items?: Customer[]; total?: number; hasNext?: boolean };
      const content = raw.content ?? raw.items ?? [];
      const size = raw.size ?? requestedSize;
      const totalElements =
        typeof raw.totalElements === "number"
          ? raw.totalElements
          : typeof raw.total === "number"
          ? raw.total
          : content.length;
      const number =
        typeof raw.number === "number"
          ? raw.number
          : typeof raw.page === "number"
          ? raw.page
          : requestedPage;
      const totalPages =
        typeof raw.totalPages === "number"
          ? raw.totalPages
          : size > 0
          ? Math.ceil(totalElements / size)
          : 0;
      const hasNext =
        typeof raw.hasNext === "boolean"
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
    },
    () => fallbackListCustomers(params),
  );
}

export function listCustomerSegments(): Promise<CustomerSegmentSummary[]> {
  return withOfflineFallback(
    "listCustomerSegments",
    async () => {
      const { data } = await api.get<CustomerSegmentSummary[]>("/v1/customers/segments");
      return data;
    },
    () => fallbackListCustomerSegments(),
  );
}

export function createCustomer(payload: CustomerPayload): Promise<Customer> {
  return withOfflineFallback(
    "createCustomer",
    async () => {
      const { data } = await api.post<Customer>("/v1/customers", payload);
      return data;
    },
    () => fallbackCreateCustomer(payload),
  );
}

export function updateCustomer(id: string, payload: CustomerPayload): Promise<Customer> {
  return withOfflineFallback(
    "updateCustomer",
    async () => {
      const { data } = await api.put<Customer>(`/v1/customers/${id}`, payload);
      return data;
    },
    () => fallbackUpdateCustomer(id, payload),
  );
}

export function deleteCustomer(id: string): Promise<void> {
  return withOfflineVoid(
    "deleteCustomer",
    () => api.delete(`/v1/customers/${id}`),
    () => fallbackDeleteCustomer(id),
  );
}

export function listProductPrices(
  productId: string,
  params?: { page?: number; size?: number },
): Promise<Page<PriceHistoryEntry>> {
  return withOfflineFallback(
    "listProductPrices",
    async () => {
      const { data } = await api.get<Page<PriceHistoryEntry>>(`/v1/products/${productId}/prices`, { params });
      return data;
    },
    () => fallbackListProductPrices(productId, params),
  );
}

export function createProductPrice(productId: string, payload: PriceChangePayload): Promise<PriceHistoryEntry> {
  return withOfflineFallback(
    "createProductPrice",
    async () => {
      const { data } = await api.post<PriceHistoryEntry>(`/v1/products/${productId}/prices`, payload);
      return data;
    },
    () => fallbackCreateProductPrice(productId, payload),
  );
}

export function listSales(params: ListSalesParams = {}): Promise<Page<SaleSummary>> {
  return withOfflineFallback(
    "listSales",
    async () => {
      const { data } = await api.get<Page<SaleSummary>>("/v1/sales", { params });
      return data;
    },
    () => fallbackListSales(params),
  );
}

export function updateSale(id: string, payload: SaleUpdatePayload): Promise<SaleRes> {
  return withOfflineFallback(
    "updateSale",
    async () => {
      const { data } = await api.put<SaleRes>(`/v1/sales/${id}`, payload);
      return data;
    },
    () => fallbackUpdateSale(id, payload),
  );
}

export function cancelSale(id: string): Promise<SaleRes> {
  return withOfflineFallback(
    "cancelSale",
    async () => {
      const { data } = await api.post<SaleRes>(`/v1/sales/${id}/cancel`, {});
      return data;
    },
    () => fallbackCancelSale(id),
  );
}

export function getSaleDetail(id: string): Promise<SaleDetail> {
  return withOfflineFallback(
    "getSaleDetail",
    async () => {
      const { data } = await api.get<SaleDetail>(`/v1/sales/${id}`);
      return data;
    },
    () => fallbackGetSaleDetail(id),
  );
}

export function listSalesTrend(params: SalesTrendParams): Promise<SalesDailyPoint[]> {
  return withOfflineFallback(
    "listSalesTrend",
    async () => {
      const { data } = await api.get<SalesDailyPoint[]>("/v1/sales/trend", { params });
      return data;
    },
    () => fallbackListSalesTrend(params),
  );
}

export function getSalesWindowMetrics(window = "14d"): Promise<SalesWindowMetrics> {
  const safeWindow = window && window.trim().length > 0 ? window : "14d";
  return withOfflineFallback(
    "getSalesWindowMetrics",
    async () => {
      const { data } = await api.get<SalesWindowMetrics>("/v1/sales/metrics", {
        params: { window: safeWindow },
      });
      return data;
    },
    () => fallbackGetSalesWindowMetrics(safeWindow),
  );
}

export function getSalesSummaryByPeriod(period: SalesPeriod): Promise<SalesPeriodSummary> {
  return withOfflineFallback(
    "getSalesSummaryByPeriod",
    async () => {
      const { data } = await api.get<SalesPeriodSummary>("/v1/sales/metrics/summary", {
        params: { period },
      });
      return data;
    },
    () => fallbackGetSalesSummary(period),
  );
}

export function listDocumentsGrouped(params: ListDocumentsParams = {}): Promise<DocumentsGroupedResponse> {
  return withOfflineFallback(
    "listDocumentsGrouped",
    async () => {
      const { data } = await api.get<DocumentsGroupedResponse>("/v1/documents", {
        params: {
          groupBy: "type",
          salesPage: params.salesPage,
          purchasesPage: params.purchasesPage,
          size: params.size,
        },
      });
      return data;
    },
    () => fallbackListDocumentsGrouped(params),
  );
}

export function getDocumentPreview(id: string): Promise<DocumentFile> {
  return withOfflineFallback(
    "getDocumentPreview",
    async () => {
      const response = await api.get<Blob>(`/v1/documents/${id}`, { responseType: "blob" });
      return createDocumentFileFromResponse(response, `${id}.pdf`);
    },
    () => fallbackGetDocumentPreview(id),
  );
}

export function downloadDocument(id: string): Promise<DocumentFile> {
  return withOfflineFallback(
    "downloadDocument",
    async () => {
      const response = await api.get<Blob>(`/v1/documents/${id}/download`, { responseType: "blob" });
      return createDocumentFileFromResponse(response, `${id}.pdf`);
    },
    () => fallbackDownloadDocument(id),
  );
}

export function getDocumentDetailUrl(id: string): string {
  return api.getUri({ url: `/v1/documents/${id}` });
}

export function listSalesDaily(days = 14): Promise<SalesDailyPoint[]> {
  return withOfflineFallback(
    "listSalesDaily",
    async () => {
      const { data } = await api.get<SalesDailyPoint[]>("/v1/sales/metrics/daily", { params: { days } });
      return data;
    },
    () => fallbackListSalesDaily(days),
  );
}

export function createSale(payload: SalePayload): Promise<SaleRes> {
  return withOfflineFallback(
    "createSale",
    async () => {
      const { data } = await api.post<SaleRes>("/v1/sales", payload);
      return data;
    },
    () => fallbackCreateSale(payload),
  );
}

export function listPurchases(params: ListPurchasesParams = {}): Promise<Page<PurchaseSummary>> {
  return withOfflineFallback(
    "listPurchases",
    async () => {
      const { data } = await api.get<Page<PurchaseSummary>>("/v1/purchases", { params });
      return data;
    },
    () => fallbackListPurchases(params),
  );
}

export function updatePurchase(id: string, payload: PurchaseUpdatePayload): Promise<PurchaseSummary> {
  return withOfflineFallback(
    "updatePurchase",
    async () => {
      const { data } = await api.put<PurchaseSummary>(`/v1/purchases/${id}`, payload);
      return data;
    },
    () => fallbackUpdatePurchase(id, payload),
  );
}

export function cancelPurchase(id: string): Promise<PurchaseSummary> {
  return withOfflineFallback(
    "cancelPurchase",
    async () => {
      const { data } = await api.post<PurchaseSummary>(`/v1/purchases/${id}/cancel`, {});
      return data;
    },
    () => fallbackCancelPurchase(id),
  );
}

export function listPurchaseDaily(days = 14): Promise<PurchaseDailyPoint[]> {
  return withOfflineFallback(
    "listPurchaseDaily",
    async () => {
      const { data } = await api.get<PurchaseDailyPoint[]>("/v1/purchases/metrics/daily", { params: { days } });
      return data;
    },
    () => fallbackListPurchaseDaily(days),
  );
}

export function createPurchase(payload: PurchasePayload): Promise<{ id: string }> {
  return withOfflineFallback(
    "createPurchase",
    async () => {
      const { data } = await api.post<{ id: string }>("/v1/purchases", payload);
      return data;
    },
    () => fallbackCreatePurchase(payload),
  );
}

export function listInventoryAlerts(threshold?: number): Promise<InventoryAlert[]> {
  return withOfflineFallback(
    "listInventoryAlerts",
    async () => {
      const params = typeof threshold === "number" ? { threshold } : {};
      const { data } = await api.get<InventoryAlert[]>("/v1/inventory/alerts", { params });
      return data;
    },
    () => fallbackListInventoryAlerts(threshold),
  );
}

export function getInventorySummary(): Promise<InventorySummary> {
  return withOfflineFallback(
    "getInventorySummary",
    async () => {
      const { data } = await api.get<InventorySummary>("/v1/inventory/summary");
      return data;
    },
    () => fallbackGetInventorySummary(),
  );
}

export function getInventorySettings(): Promise<InventorySettings> {
  return withOfflineFallback(
    "getInventorySettings",
    async () => {
      const { data } = await api.get<InventorySettings>("/v1/inventory/settings");
      return data;
    },
    () => fallbackGetInventorySettings(),
  );
}

export function updateInventorySettings(payload: { lowStockThreshold: number }): Promise<InventorySettings> {
  return withOfflineFallback(
    "updateInventorySettings",
    async () => {
      const { data } = await api.put<InventorySettings>("/v1/inventory/settings", payload);
      return data;
    },
    () => fallbackUpdateInventorySettings(payload),
  );
}

export function createInventoryAdjustment(payload: InventoryAdjustmentPayload): Promise<InventoryAdjustmentResponse> {
  return withOfflineFallback(
    "createInventoryAdjustment",
    async () => {
      const { data } = await api.post<InventoryAdjustmentResponse>("/v1/inventory/adjustments", payload);
      return data;
    },
    () => fallbackCreateInventoryAdjustment(payload),
  );
}







