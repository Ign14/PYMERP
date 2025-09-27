import { FormEvent, useEffect, useMemo, useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createProductPrice,
  deleteProduct,
  fetchProductStock,
  listProductPrices,
  listProducts,
  updateProductStatus,
  PriceChangePayload,
  PriceHistoryEntry,
  Product,
  ProductStock,
} from "../services/client";

import ProductFormDialog from "./dialogs/ProductFormDialog";
import ProductInventoryAlertModal from "./dialogs/ProductInventoryAlertModal";
import ProductQrModal from "./dialogs/ProductQrModal";

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
  const queryClient = useQueryClient();

  const resolveNumericPrice = (value?: string | number | null) => {
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
    const numeric = resolveNumericPrice(value);
    return numeric === null || numeric <= 0 ? "Sin precio" : `$${numeric.toFixed(2)}`;
  };

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
      setPriceForm({ price: resolveNumericPrice(updated.currentPrice) ?? 0 });
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

  const openCriticalStockModal = () => {
    if (!selectedProduct) return;
    setCriticalProduct(selectedProduct);
    setCriticalModalOpen(true);
  };

  const closeCriticalModal = () => {
    setCriticalModalOpen(false);
    setCriticalProduct(null);
  };

  const handleCriticalStockSaved = (updated: Product) => {
    queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
    setSelectedProduct(updated);
    setPriceForm({ price: resolveNumericPrice(updated.currentPrice) ?? 0 });
  };

  const handleProductSaved = (product: Product) => {
    queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
    setSelectedProduct(product);
    setPriceForm({ price: resolveNumericPrice(product.currentPrice) ?? 0 });
    if (qrProduct?.id === product.id) {
      setQrProduct(product);
    }
    if (criticalProduct?.id === product.id) {
      setCriticalProduct(product);
    }
    queryClient.invalidateQueries({ queryKey: ["product", product.id, "stock"] });
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
      setPriceForm({ price: resolveNumericPrice(latest.currentPrice) ?? 0 });
    }
  }, [productsQuery.data, selectedProduct]);

  const selectedPriceLabel = useMemo(() => {
    if (!selectedProduct) return "";
    return formatPriceLabel(selectedProduct.currentPrice);
  }, [selectedProduct]);

  const selectedCriticalStock = useMemo(() => {
    if (!selectedProduct) return 0;
    return resolveNumericPrice(selectedProduct.criticalStock ?? 0) ?? 0;
  }, [selectedProduct]);

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
    <div className="card">
      <div className="card-header">
        <h2>Productos</h2>
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
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>SKU</th>
                  <th>Precio actual</th>
                  <th>Estado</th>
                  <th>QR</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product: Product) => (
                  <tr
                    key={product.id}
                    className={selectedProduct?.id === product.id ? "selected" : undefined}
                    onClick={() => {
                      setSelectedProduct(product);
                      setPriceForm({ price: resolveNumericPrice(product.currentPrice) ?? 0 });
                    }}
                  >
                    <td>{product.name}</td>
                    <td className="mono">{product.sku}</td>
                    <td className="mono">
                      {formatPriceLabel(product.currentPrice)}
                    </td>
                    <td>{product.active ? "Activo" : "Inactivo"}</td>
                    <td>
                      <button
                        className="btn ghost"
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation();
                          openQrModal(product);
                        }}
                      >
                        Ver QR
                      </button>
                    </td>
                </tr>
                ))}
              </tbody>
            </table>
          </div>

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
                <button className="btn ghost" type="button" onClick={openCriticalStockModal}>
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
        onClose={closeCriticalModal}
        onSaved={handleCriticalStockSaved}
      />
    </div>
  );
}












