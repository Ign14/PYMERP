import { FormEvent, useEffect, useMemo, useState } from "react";
import axios from "axios";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createProductPrice,
  deleteProduct,
  fetchProductStock,
  listProductPrices,
  listProducts,
  updateProductInventoryAlert,
  updateProductStatus,
  PriceChangePayload,
  PriceHistoryEntry,
  Product,
  ProductStock,
} from "../services/client";

import ProductFormDialog from "./dialogs/ProductFormDialog";
import ProductInventoryAlertModal from "./dialogs/ProductInventoryAlertModal";
import ProductQrModal from "./dialogs/ProductQrModal";
import ProductImageModal from "./dialogs/ProductImageModal";
import placeholderImage from "../../assets/product-placeholder.svg";

const DEFAULT_PAGE_SIZE = 10;

export default function ProductsCard() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<"active" | "inactive" | "all">("active");
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [priceForm, setPriceForm] = useState<PriceChangePayload>({ price: 0 });
  const [productDialogOpen, setProductDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [qrModalOpen, setQrModalOpen] = useState(false);
  const [qrProduct, setQrProduct] = useState<Product | null>(null);
  const [criticalModalOpen, setCriticalModalOpen] = useState(false);
  const [criticalProduct, setCriticalProduct] = useState<Product | null>(null);
  const [pendingCriticalStock, setPendingCriticalStock] = useState<Record<string, number>>({});
  const [criticalErrors, setCriticalErrors] = useState<Record<string, string>>({});
  const [savingCritical, setSavingCritical] = useState(false);
  const [statusMessage, setStatusMessage] = useState<
    | {
        type: "success" | "error";
        text: string;
      }
    | null
  >(null);
  const [imageModalOpen, setImageModalOpen] = useState(false);
  const [imageProduct, setImageProduct] = useState<Product | null>(null);
  const [imageSrc, setImageSrc] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const resolveNumeric = (value?: string | number | null) => {
    if (typeof value === "number") {
      return Number.isFinite(value) ? value : null;
    }
    if (typeof value === "string" && value.trim().length > 0) {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    }
    return null;
  };

  const formatPriceLabel = (value?: string | number | null) => {
    const numeric = resolveNumeric(value);
    return numeric === null || numeric <= 0 ? "Sin precio" : `$${numeric.toFixed(2)}`;
  };

  const dirty = useMemo(() => Object.keys(pendingCriticalStock).length > 0, [pendingCriticalStock]);

  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!dirty) {
        return;
      }
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [dirty]);

  const resolveStockValue = (value?: string | number | null) => {
    const numeric = resolveNumeric(value);
    return numeric === null || Number.isNaN(numeric) ? 0 : numeric;
  };

  const getDisplayCriticalStock = (product: Product) => {
    if (!product) return 0;
    const pending = pendingCriticalStock[product.id];
    if (typeof pending === "number") {
      return pending;
    }
    return resolveStockValue(product.criticalStock ?? 0);
  };

  const getDisplayStock = (product: Product) => {
    if (!product) return 0;
    return resolveStockValue(product.stock ?? 0);
  };

  const isStockCritical = (product: Product) => {
    const stockValue = getDisplayStock(product);
    const criticalValue = getDisplayCriticalStock(product);
    return criticalValue > 0 && stockValue <= criticalValue;
  };

  const hasPendingCriticalChange = (productId: string) =>
    Object.prototype.hasOwnProperty.call(pendingCriticalStock, productId);

  useEffect(() => {
    setPage(0);
  }, [statusFilter]);

  const productsQuery = useQuery({
    queryKey: ["products", { q: search, page, status: statusFilter }],
    queryFn: () => listProducts({
      q: search || undefined,
      page,
      size: DEFAULT_PAGE_SIZE,
      status: statusFilter,
    }),
    placeholderData: keepPreviousData,
  });

  const pricesQuery = useQuery({
    queryKey: ["product-prices", selectedProduct?.id],
    queryFn: () => {
      if (!selectedProduct) {
        throw new Error("Producto no seleccionado");
      }
      return listProductPrices(selectedProduct.id, { size: 5 });
    },
    enabled: !!selectedProduct,
  });

  const stockQuery = useQuery<ProductStock>({
    queryKey: ["product", selectedProduct?.id, "stock"],
    queryFn: () => {
      if (!selectedProduct) {
        throw new Error("Producto no seleccionado");
      }
      return fetchProductStock(selectedProduct.id);
    },
    enabled: !!selectedProduct,
  });

  const priceMutation = useMutation({
    mutationFn: async () => {
      if (!selectedProduct) throw new Error("Select a product first");
      return createProductPrice(selectedProduct.id, priceForm);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
      queryClient.invalidateQueries({ queryKey: ["product-prices", selectedProduct?.id] });
      setPriceForm({ price: 0 });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteProduct(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
      queryClient.removeQueries({ queryKey: ["product-prices", id], exact: true });
      if (selectedProduct?.id === id) {
        setSelectedProduct(null);
        setPriceForm({ price: 0 });
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: (active: boolean) => {
      if (!selectedProduct) throw new Error("Producto no seleccionado");
      return updateProductStatus(selectedProduct.id, active);
    },
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
      setSelectedProduct(updated);
      setPriceForm({ price: resolveNumeric(updated.currentPrice) ?? 0 });
    },
  });

  const openCreateDialog = () => {
    setEditingProduct(null);
    setProductDialogOpen(true);
  };

  const openEditDialog = () => {
    if (!selectedProduct) return;
    setEditingProduct(selectedProduct);
    setProductDialogOpen(true);
  };

  const closeProductDialog = () => {
    setProductDialogOpen(false);
    setEditingProduct(null);
  };

  const openQrModal = (product?: Product | null) => {
    const target = product ?? selectedProduct;
    if (!target) return;
    setQrProduct(target);
    setQrModalOpen(true);
  };

  const closeQrModal = () => {
    setQrModalOpen(false);
    setQrProduct(null);
  };

  const openCriticalStockModal = (product?: Product | null) => {
    const target = product ?? selectedProduct;
    if (!target) return;
    setCriticalProduct(target);
    setCriticalModalOpen(true);
  };

  const closeCriticalModal = () => {
    setCriticalModalOpen(false);
    setCriticalProduct(null);
  };

  const handleCriticalStockSubmit = (value: number) => {
    if (!criticalProduct) return;
    const productId = criticalProduct.id;
    const baseValue = resolveStockValue(criticalProduct.criticalStock ?? 0);
    setPendingCriticalStock((prev) => {
      const next = { ...prev };
      if (value === baseValue) {
        delete next[productId];
      } else {
        next[productId] = value;
      }
      return next;
    });
    setCriticalErrors((prev) => {
      if (!prev[productId]) {
        return prev;
      }
      const { [productId]: _removed, ...rest } = prev;
      return rest;
    });
    setStatusMessage(null);
    if (selectedProduct?.id === productId) {
      setSelectedProduct({ ...selectedProduct, criticalStock: value });
    }
    setCriticalModalOpen(false);
    setCriticalProduct(null);
  };

  const handleSaveCriticalChanges = async () => {
    const entries = Object.entries(pendingCriticalStock);
    if (entries.length === 0 || savingCritical) {
      return;
    }
    setSavingCritical(true);
    setStatusMessage(null);
    const remaining = { ...pendingCriticalStock };
    const updatedProducts: Product[] = [];

    for (const [productId, value] of entries) {
      try {
        const updated = await updateProductInventoryAlert(productId, value);
        updatedProducts.push(updated);
        delete remaining[productId];
        setCriticalErrors((prev) => {
          if (!prev[productId]) {
            return prev;
          }
          const { [productId]: _removed, ...rest } = prev;
          return rest;
        });
      } catch (error) {
        const parsed = resolveProblemDetail(error);
        const productName = productsQuery.data?.content?.find((item) => item.id === productId)?.name ?? productId;
        setCriticalErrors((prev) => ({
          ...prev,
          [productId]: parsed.fieldMessage ?? parsed.message,
        }));
        setPendingCriticalStock(remaining);
        setStatusMessage({
          type: "error",
          text: `${productName}: ${parsed.message}`,
        });
        setSavingCritical(false);
        return;
      }
    }

    setPendingCriticalStock({});
    setStatusMessage({ type: "success", text: "Cambios guardados correctamente." });
    setSavingCritical(false);
    queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
    updatedProducts.forEach((product) => {
      queryClient.invalidateQueries({ queryKey: ["product", product.id, "stock"] });
    });
    if (selectedProduct) {
      const refreshed = updatedProducts.find((item) => item.id === selectedProduct.id);
      if (refreshed) {
        setSelectedProduct(refreshed);
        setPriceForm({ price: resolveNumeric(refreshed.currentPrice) ?? 0 });
      }
    }
  };

  const handleProductSaved = (product: Product) => {
    queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
    setSelectedProduct(product);
    setPriceForm({ price: resolveNumeric(product.currentPrice) ?? 0 });
    if (qrProduct?.id === product.id) {
      setQrProduct(product);
    }
    setPendingCriticalStock((prev) => {
      if (!prev[product.id]) {
        return prev;
      }
      const { [product.id]: _removed, ...rest } = prev;
      return rest;
    });
    setCriticalErrors((prev) => {
      if (!prev[product.id]) {
        return prev;
      }
      const { [product.id]: _removed, ...rest } = prev;
      return rest;
    });
    queryClient.invalidateQueries({ queryKey: ["product", product.id, "stock"] });
  };

  const openImageModal = (product: Product) => {
    const src = typeof product.imageUrl === "string" && product.imageUrl.trim().length > 0 ? product.imageUrl : placeholderImage;
    setImageProduct(product);
    setImageSrc(src);
    setImageModalOpen(true);
  };

  const closeImageModal = () => {
    setImageModalOpen(false);
    setImageProduct(null);
    setImageSrc(null);
  };

  const handleDelete = () => {
    if (!selectedProduct || deleteMutation.isPending) return;
    if (window.confirm(`Eliminar producto ${selectedProduct.name}?`)) {
      deleteMutation.mutate(selectedProduct.id);
    }
  };

  const toggleStatus = () => {
    if (!selectedProduct || statusMutation.isPending) return;
    statusMutation.mutate(!selectedProduct.active);
  };

  const products: Product[] = productsQuery.data?.content ?? [];
  const totalPages = productsQuery.data?.totalPages ?? 1;

  useEffect(() => {
    if (!selectedProduct) return;
    const latest = productsQuery.data?.content?.find((item) => item.id === selectedProduct.id);
    if (!latest) {
      setSelectedProduct(null);
      setPriceForm({ price: 0 });
      return;
    }
    if (latest !== selectedProduct) {
      setSelectedProduct(latest);
      setPriceForm({ price: resolveNumeric(latest.currentPrice) ?? 0 });
    }
  }, [productsQuery.data, selectedProduct]);

  const selectedPriceLabel = useMemo(() => {
    if (!selectedProduct) return "";
    return formatPriceLabel(selectedProduct.currentPrice);
  }, [selectedProduct]);

  const selectedCriticalStock = useMemo(() => {
    if (!selectedProduct) return 0;
    return getDisplayCriticalStock(selectedProduct);
  }, [selectedProduct, pendingCriticalStock]);

  const onSubmitPrice = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedProduct) return;
    if (!priceForm.price || priceForm.price <= 0) {
      alert("Ingresa un precio valido");
      return;
    }
    priceMutation.mutate();
  };

  return (
    <div className="card products-card">
      <div className="products-banner">
        <div>
          <h2>Productos</h2>
          <p className="muted small">
            {dirty
              ? "Tienes cambios sin guardar en los stocks críticos."
              : "Administra tu catálogo, imágenes y alertas de stock."}
          </p>
        </div>
        <button
          className="btn primary"
          type="button"
          onClick={handleSaveCriticalChanges}
          disabled={!dirty || savingCritical}
        >
          {savingCritical ? "Guardando..." : "Guardar cambios"}
        </button>
      </div>

      {statusMessage && (
        <div className={`status-message ${statusMessage.type}`} role="status">
          {statusMessage.text}
        </div>
      )}

      <div className="card-header">
        <div className="inline-actions">
          <input
            className="input"
            placeholder="Buscar por nombre, SKU o codigo"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
          <select
            className="input"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as "active" | "inactive" | "all")}
          >
            <option value="active">Activos</option>
            <option value="inactive">Inactivos</option>
            <option value="all">Todos</option>
          </select>
          <button
            className="btn"
            onClick={() => queryClient.invalidateQueries({ queryKey: ["products"] })}
            disabled={productsQuery.isFetching}
          >
            {productsQuery.isFetching ? "Cargando" : "Refrescar"}
          </button>
        </div>
      </div>

      <div className="inline-actions">
        <button className="btn" type="button" onClick={openCreateDialog}>
          + Producto
        </button>
        <button className="btn ghost" type="button" onClick={openEditDialog} disabled={!selectedProduct}>
          Editar
        </button>
        <button className="btn ghost" type="button" onClick={() => openQrModal(selectedProduct)} disabled={!selectedProduct}>
          Ver QR
        </button>
        <button
          className="btn ghost"
          type="button"
          onClick={toggleStatus}
          disabled={!selectedProduct || statusMutation.isPending}
        >
          {statusMutation.isPending ? "Actualizando..." : selectedProduct?.active ? "Desactivar" : "Activar"}
        </button>
        <button
          className="btn ghost"
          type="button"
          onClick={handleDelete}
          disabled={!selectedProduct || deleteMutation.isPending}
        >
          {deleteMutation.isPending ? "Eliminando..." : "Eliminar"}
        </button>
      </div>

      {dirty && (
        <p className="muted small notice-dirty">Recuerda guardar para aplicar los cambios pendientes.</p>
      )}
      {deleteMutation.isError && (
        <p className="error">{(deleteMutation.error as Error)?.message ?? "No se pudo eliminar el producto"}</p>
      )}

      {statusMutation.isError && (
        <p className="error">{(statusMutation.error as Error)?.message ?? "No se pudo actualizar el estado"}</p>
      )}

      {productsQuery.isLoading && <p>Loading...</p>}
      {productsQuery.isError && <p className="error">{productsQuery.error.message}</p>}

      {!productsQuery.isLoading && !productsQuery.isError && (
        <>
          <div className="products-grid">
            {products.map((product: Product) => {
              const pendingValue = pendingCriticalStock[product.id];
              const stockValue = getDisplayStock(product);
              const criticalValue = getDisplayCriticalStock(product);
              const criticalError = criticalErrors[product.id];
              const imageSource =
                typeof product.imageUrl === "string" && product.imageUrl.trim().length > 0
                  ? product.imageUrl
                  : placeholderImage;
              const isSelected = selectedProduct?.id === product.id;
              const isCritical = isStockCritical(product);
              const hasPending = hasPendingCriticalChange(product.id);

              return (
                <div
                  key={product.id}
                  className={`product-card${isSelected ? " product-card--selected" : ""}${isCritical ? " product-card--critical" : ""}${hasPending ? " product-card--pending" : ""}`}
                  onClick={() => {
                    setSelectedProduct(product);
                    setPriceForm({ price: resolveNumeric(product.currentPrice) ?? 0 });
                  }}
                >
                  <div className="product-card-image" onClick={(e) => {
                    e.stopPropagation();
                    openImageModal(product);
                  }}>
                    <img src={imageSource} alt={product.name} />
                    {!product.active && (
                      <div className="product-badge product-badge--inactive">Inactivo</div>
                    )}
                    {isCritical && product.active && (
                      <div className="product-badge product-badge--critical">⚠️ Stock Bajo</div>
                    )}
                  </div>
                  
                  <div className="product-card-body">
                    <div className="product-card-header">
                      <h4 className="product-card-title">{product.name}</h4>
                      <span className="product-card-sku mono small muted">{product.sku}</span>
                    </div>

                    <div className="product-card-stats">
                      <div className="product-stat">
                        <span className="product-stat-label muted small">Precio</span>
                        <span className="product-stat-value mono">{formatPriceLabel(product.currentPrice)}</span>
                      </div>
                      <div className="product-stat">
                        <span className="product-stat-label muted small">Stock</span>
                        <span className={`product-stat-value ${isCritical ? "text-critical" : ""}`}>
                          {stockValue}
                          {isCritical && <span className="stock-indicator">⚠️</span>}
                        </span>
                      </div>
                      <div className="product-stat">
                        <span className="product-stat-label muted small">Crítico</span>
                        <span className={`product-stat-value${hasPending ? " text-pending" : ""}`}>
                          {criticalValue}
                          {hasPending && <span className="pending-indicator">●</span>}
                        </span>
                      </div>
                    </div>

                    {criticalError && (
                      <p className="error small" style={{ marginTop: "0.5rem" }}>{criticalError}</p>
                    )}

                    <div className="product-card-actions">
                      <button
                        className="btn ghost small"
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          openCriticalStockModal(product);
                        }}
                      >
                        Stock crítico
                      </button>
                      <button
                        className="btn ghost small"
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          openQrModal(product);
                        }}
                      >
                        QR
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {products.length === 0 && (
            <div className="muted" style={{ textAlign: "center", padding: "2rem" }}>
              <p>No se encontraron productos</p>
            </div>
          )}

          {totalPages > 1 && (
            <div className="pagination">
              <button className="btn" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                Anterior
              </button>
              <span className="muted">Pagina {page + 1} de {totalPages}</span>
              <button
                className="btn"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              >
                Siguiente
              </button>
            </div>
          )}

          {selectedProduct && (
            <>
              <div className="panel">
                <h3 className="panel-title">Precio</h3>
                <p className="muted">Actual: {selectedPriceLabel}</p>

                <p className="muted">Estado: {selectedProduct.active ? "Activo" : "Inactivo"}</p>

                <form className="form-inline" onSubmit={onSubmitPrice}>
                  <input
                    className="input"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Nuevo precio"
                    value={priceForm.price}
                    onChange={(e) => setPriceForm((prev) => ({ ...prev, price: Number(e.target.value) }))}
                  />
                  <button className="btn" type="submit" disabled={priceMutation.isPending}>
                    {priceMutation.isPending ? "Guardando..." : "Actualizar"}
                  </button>
                </form>

                {priceMutation.isError && <p className="error">{(priceMutation.error as Error)?.message}</p>}

                <div className="table-wrapper compact">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Precio</th>
                        <th>Desde</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(pricesQuery.data?.content ?? []).map((entry: PriceHistoryEntry) => (
                        <tr key={entry.id}>
                          <td className="mono">{`$${entry.price.toFixed(2)}`}</td>
                          <td className="mono small">{new Date(entry.validFrom).toLocaleString()}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="panel">
                <h3 className="panel-title">Inventario</h3>
                <p className="muted">
                  Stock total: {stockQuery.isLoading ? "Cargando..." : stockQuery.data ? stockQuery.data.total : "Sin datos"}
                </p>
                <p className="muted">Stock crítico configurado: {selectedCriticalStock}</p>
                <button className="btn ghost" type="button" onClick={() => openCriticalStockModal()}>
                  Definir stock crítico
                </button>
                {stockQuery.isError && (
                  <p className="error">{(stockQuery.error as Error)?.message ?? "No se pudo obtener el stock"}</p>
                )}
                {stockQuery.isLoading && <p className="muted">Cargando lotes...</p>}
                {stockQuery.data && stockQuery.data.lots.length > 0 && (
                  <div className="table-wrapper compact">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Lote</th>
                          <th>Cantidad</th>
                          <th>Ubicación</th>
                          <th>Vence</th>
                        </tr>
                      </thead>
                      <tbody>
                        {stockQuery.data.lots.map((lot) => (
                          <tr key={lot.lotId}>
                            <td className="mono small">{lot.lotId}</td>
                            <td className="mono">{lot.quantity}</td>
                            <td>{lot.location ?? "—"}</td>
                            <td className="mono small">
                              {lot.expiresAt ? new Date(lot.expiresAt).toLocaleDateString() : "—"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                {stockQuery.data && stockQuery.data.lots.length === 0 && !stockQuery.isLoading && (
                  <p className="muted small">No hay lotes registrados para este producto.</p>
                )}
              </div>
            </>
          )}
        </>
      )}
      <ProductFormDialog
        open={productDialogOpen}
        product={editingProduct}
        onClose={closeProductDialog}
        onSaved={handleProductSaved}
      />
      <ProductQrModal open={qrModalOpen} product={qrProduct} onClose={closeQrModal} />
      <ProductInventoryAlertModal
        open={criticalModalOpen}
        product={criticalProduct}
        pendingValue={criticalProduct ? pendingCriticalStock[criticalProduct.id] : undefined}
        error={criticalProduct ? criticalErrors[criticalProduct.id] : undefined}
        submitting={savingCritical}
        onClose={closeCriticalModal}
        onSubmit={handleCriticalStockSubmit}
      />
      <ProductImageModal open={imageModalOpen} product={imageProduct} imageUrl={imageSrc} onClose={closeImageModal} />
    </div>
  );
}

type ProblemDetailPayload = {
  title?: string;
  detail?: string;
  message?: string;
  error?: string;
  errors?: Record<string, string[] | string | undefined>;
};

function resolveProblemDetail(error: unknown): { message: string; fieldMessage?: string } {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ProblemDetailPayload | string | undefined;
    if (typeof data === "string" && data.trim().length > 0) {
      return { message: data };
    }
    if (data && typeof data === "object") {
      const errors = data.errors ?? {};
      let fieldMessage: string | undefined;
      const criticalErrors = errors.criticalStock;
      if (Array.isArray(criticalErrors) && criticalErrors.length > 0) {
        fieldMessage = criticalErrors[0];
      } else if (typeof criticalErrors === "string" && criticalErrors.trim().length > 0) {
        fieldMessage = criticalErrors;
      }

      const message =
        [data.detail, data.message, data.error, data.title]
          .find((value) => typeof value === "string" && value.trim().length > 0)?.trim() ??
        error.response?.statusText ??
        "No se pudo guardar";

      return { message, fieldMessage };
    }
    const statusMessage = error.response?.statusText;
    if (statusMessage) {
      return { message: statusMessage };
    }
  }
  if (error instanceof Error && error.message) {
    return { message: error.message };
  }
  return { message: "No se pudo guardar" };
}












