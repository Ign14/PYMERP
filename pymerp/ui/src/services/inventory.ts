import { api, Page, LocationStatus, LocationType } from './client'
export type { LocationStatus, LocationType } from './client'

export type InventoryLocation = {
  id: string
  code?: string | null
  name: string
  description?: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
  type: LocationType
  status: LocationStatus
  businessName?: string | null
  rut?: string | null
}

export type InventoryLocationPayload = {
  code: string
  name: string
  description?: string | null
  enabled?: boolean
  type?: LocationType
  status?: LocationStatus
  businessName?: string | null
  rut?: string | null
}

export type ListInventoryLocationsParams = {
  q?: string
  enabled?: boolean
  page?: number
  size?: number
  sort?: string
}

export async function getLocations(
  params?: ListInventoryLocationsParams
): Promise<Page<InventoryLocation>> {
  const response = await api.get<Page<LocationApiResponse>>('/v1/inventory/locations', {
    params,
  })
  const page = response.data ?? { content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 }
  const content = Array.isArray(page.content) ? page.content : []
  return {
    ...page,
    content: content.map(mapLocationResponse),
  }
}

export async function createLocation(
  payload: InventoryLocationPayload
): Promise<InventoryLocation> {
  const requestBody = buildLocationRequest(payload)
  const { data } = await api.post<LocationApiResponse>('/v1/inventory/locations', requestBody)
  return mapLocationResponse(data)
}

export async function updateLocation(
  id: string,
  payload: InventoryLocationPayload
): Promise<InventoryLocation> {
  const requestBody = buildLocationRequest(payload)
  const { data } = await api.put<LocationApiResponse>(`/v1/inventory/locations/${id}`, requestBody)
  return mapLocationResponse(data)
}

export async function toggleLocationEnabled(id: string, enabled: boolean): Promise<InventoryLocation> {
  const { data } = await api.patch<LocationApiResponse>(`/v1/inventory/locations/${id}`, {
    enabled,
    status: enabled ? 'ACTIVE' : 'BLOCKED',
  })
  return mapLocationResponse(data)
}

const DEFAULT_LOCATION_TYPE: LocationType = 'BODEGA'

type LocationApiResponse = {
  id: string
  code?: string | null
  name: string
  description?: string | null
  enabled?: boolean | null
  type?: LocationType | null
  businessName?: string | null
  rut?: string | null
  status?: LocationStatus | null
  createdAt: string
  updatedAt: string
}

function mapLocationResponse(response: LocationApiResponse): InventoryLocation {
  const status: LocationStatus =
    response.status ?? (response.enabled === false ? 'BLOCKED' : 'ACTIVE')
  return {
    id: response.id,
    code: response.code ?? null,
    name: response.name,
    description: response.description ?? null,
    enabled: status !== 'BLOCKED',
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
    type: response.type ?? DEFAULT_LOCATION_TYPE,
    status,
    businessName: response.businessName ?? null,
    rut: response.rut ?? null,
  }
}

function normalizeText(value?: string | null): string | undefined {
  if (!value) return undefined
  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

function buildLocationRequest(payload: InventoryLocationPayload) {
  const normalizedCode = payload.code?.trim()
  const normalizedName = payload.name?.trim()
  const status: LocationStatus =
    payload.status ?? (payload.enabled === false ? 'BLOCKED' : 'ACTIVE')
  const type: LocationType = payload.type ?? DEFAULT_LOCATION_TYPE

  if (!normalizedCode) {
    throw new Error('El código de la ubicación es obligatorio')
  }

  if (!normalizedName) {
    throw new Error('El nombre de la ubicación es obligatorio')
  }

  return {
    code: normalizedCode,
    name: normalizedName,
    description: normalizeText(payload.description),
    enabled: payload.enabled ?? (status !== 'BLOCKED'),
    type,
    status,
    businessName: normalizeText(payload.businessName),
    rut: normalizeText(payload.rut),
  }
}

type InventoryLotListItemResponse = {
  lotId: string
  product: { id: string; name: string; sku: string }
  supplier?: { id: string; name: string } | null
  location?: { id: string; name: string } | null
  qtyAvailable: string | number
  qtyReserved: string | number
  status: string
  fechaIngreso: string
  fechaExpiracion?: string | null
}

function toLotEntity(response: InventoryLotListItemResponse): InventoryLotListItem {
  return {
    lotId: response.lotId,
    product: response.product,
    supplier: response.supplier ?? null,
    location: response.location ?? null,
    qtyAvailable: toNumber(response.qtyAvailable),
    qtyReserved: toNumber(response.qtyReserved),
    status: response.status,
    fechaIngreso: response.fechaIngreso,
    fechaExpiracion: response.fechaExpiracion ?? null,
  }
}

function toNumber(value: string | number | null | undefined): number {
  if (typeof value === 'number') {
    return value
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

function toNullableNumber(value: string | number | null | undefined): number | null {
  if (value === null || value === undefined) {
    return null
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

export type InventoryLotListItem = {
  lotId: string
  product: { id: string; name: string; sku: string }
  supplier: { id: string; name: string } | null
  location: { id: string; name: string } | null
  qtyAvailable: number
  qtyReserved: number
  status: string
  fechaIngreso: string
  fechaExpiracion: string | null
}

export type ListInventoryLotsParams = {
  q?: string
  status?: string
  locationId?: string
  supplierId?: string
  productId?: string
  page?: number
  size?: number
  sort?: string
  dateFrom?: string
  dateTo?: string
}

export async function getLots(
  params?: ListInventoryLotsParams
): Promise<Page<InventoryLotListItem>> {
  const response = await api.get<Page<InventoryLotListItemResponse>>('/v1/inventory/lots', {
    params,
  })
  return {
    ...response.data,
    content: response.data.content.map(toLotEntity),
  }
}

type InventoryMovementLocationResponse = {
  id?: string | null
  name?: string | null
}

type InventoryMovementHistoryEntryResponse = {
  id: string
  type: string
  qtyChange: string | number
  beforeQty?: string | number | null
  afterQty?: string | number | null
  productId?: string | null
  lotId?: string | null
  locationFrom?: InventoryMovementLocationResponse | null
  locationTo?: InventoryMovementLocationResponse | null
  userId?: string | null
  traceId?: string | null
  refType?: string | null
  refId?: string | null
  createdAt: string
  reasonCode?: string | null
  note?: string | null
}

export type InventoryMovementHistoryEntry = {
  id: string
  type: string
  qtyChange: number
  beforeQty: number | null
  afterQty: number | null
  productId: string | null
  lotId: string | null
  locationFrom: InventoryMovementLocation | null
  locationTo: InventoryMovementLocation | null
  userId: string | null
  traceId: string | null
  refType: string | null
  refId: string | null
  createdAt: string
  reasonCode: string | null
  note: string | null
}

export type InventoryMovementLocation = {
  id: string | null
  name: string | null
}

function toMovementLocation(
  response?: InventoryMovementLocationResponse | null
): InventoryMovementLocation | null {
  if (!response) {
    return null
  }
  return {
    id: response.id ?? null,
    name: response.name ?? null,
  }
}

function toInventoryMovement(response: InventoryMovementHistoryEntryResponse): InventoryMovementHistoryEntry {
  return {
    id: response.id,
    type: response.type,
    qtyChange: toNumber(response.qtyChange),
    beforeQty: toNullableNumber(response.beforeQty),
    afterQty: toNullableNumber(response.afterQty),
    productId: response.productId ?? null,
    lotId: response.lotId ?? null,
    locationFrom: toMovementLocation(response.locationFrom),
    locationTo: toMovementLocation(response.locationTo),
    userId: response.userId ?? null,
    traceId: response.traceId ?? null,
    refType: response.refType ?? null,
    refId: response.refId ?? null,
    createdAt: response.createdAt,
    reasonCode: response.reasonCode ?? null,
    note: response.note ?? null,
  }
}

export type ListInventoryMovementsParams = {
  productId?: string
  lotId?: string
  type?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
  sort?: string
}

export async function getMovements(
  params?: ListInventoryMovementsParams
): Promise<Page<InventoryMovementHistoryEntry>> {
  const response = await api.get<Page<InventoryMovementHistoryEntryResponse>>('/v1/inventory/movements', {
    params,
  })
  return {
    ...response.data,
    content: response.data.content.map(toInventoryMovement),
  }
}

export async function assignLotLocation(
  lotId: string,
  locationId: string
): Promise<InventoryLotListItem> {
  const { data } = await api.put<InventoryLotListItemResponse>(
    `/v1/inventory/lots/${lotId}/location/${locationId}`
  )
  return toLotEntity(data)
}

type StockByLocationApiResponse = {
  productId: string
  locationId: string
  locationName: string
  availableQty: string | number
}

export type StockByLocation = {
  productId: string
  locationId: string
  locationName: string
  availableQty: number
}

function toStockByLocationEntity(response: StockByLocationApiResponse): StockByLocation {
  return {
    productId: response.productId,
    locationId: response.locationId,
    locationName: response.locationName,
    availableQty: toNumber(response.availableQty),
  }
}

export async function getStockByProduct(productId: string): Promise<StockByLocation[]> {
  if (!productId) {
    return []
  }
  const { data } = await api.get<StockByLocationApiResponse[]>(
    '/v1/inventory/stock/by-product',
    {
      params: { productId },
    }
  )
  return data.map(toStockByLocationEntity)
}
