import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  createPurchase,
  listProducts,
  listSuppliers,
  Page,
  Product,
  Supplier,
  PurchaseItemPayload,
  PurchasePayload,
} from "../../services/client";
import Modal from "./Modal";

interface Props {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export default function PurchaseCreateDialog({ open, onClose, onCreated }: Props) {
  const [supplierId, setSupplierId] = useState<string>("");
  const [docType, setDocType] = useState("Factura");
  const [docNumber, setDocNumber] = useState("");
  const [issuedAt, setIssuedAt] = useState(() => new Date().toISOString().slice(0, 16));
  const [items, setItems] = useState<PurchaseItemPayload[]>([]);
  const [productId, setProductId] = useState("");
  const [qty, setQty] = useState(1);
  const [unitCost, setUnitCost] = useState(0);
  const [vatRate, setVatRate] = useState(19);
  const [expDate, setExpDate] = useState("");

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ["products", { dialog: "purchases" }],
    queryFn: () => listProducts({ size: 200 }),
    enabled: open,
  });

  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ["suppliers", { dialog: "purchases" }],
    queryFn: () => listSuppliers(),
    enabled: open,
  });

  useEffect(() => {
    if (open) {
      setIssuedAt(new Date().toISOString().slice(0, 16));
    }
  }, [open]);

  const mutation = useMutation({
    mutationFn: (payload: PurchasePayload) => createPurchase(payload),
    onSuccess: () => {
      onCreated();
      resetForm();
      onClose();
    },
  });

  const productOptions = productsQuery.data?.content ?? [];
  const supplierOptions = suppliersQuery.data ?? [];

  const totals = useMemo(() => {
    const net = items.reduce((acc, item) => acc + item.qty * item.unitCost, 0);
    const vat = items.reduce((acc, item) => acc + item.qty * item.unitCost * ((item.vatRate ?? vatRate) / 100), 0);
    const total = net + vat;
    return { net, vat, total };
  }, [items, vatRate]);

  const addItem = () => {
    if (!productId || qty <= 0 || unitCost <= 0) return;
    setItems((prev) => [
      ...prev,
      { productId, qty, unitCost, vatRate, expDate: expDate || undefined },
    ]);
    setProductId("");
    setQty(1);
    setUnitCost(0);
    setExpDate("");
  };

  const removeItem = (index: number) => {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const resetForm = () => {
    setSupplierId("");
    setDocType("Factura");
    setDocNumber("");
    setItems([]);
    setProductId("");
    setQty(1);
    setUnitCost(0);
    setExpDate("");
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!supplierId || items.length === 0) return;
    const payload: PurchasePayload = {
      supplierId,
      docType,
      docNumber,
      issuedAt: new Date(issuedAt).toISOString(),
      net: Number(totals.net.toFixed(2)),
      vat: Number(totals.vat.toFixed(2)),
      total: Number(totals.total.toFixed(2)),
      items,
    };
    mutation.mutate(payload);
  };

  return (
    <Modal open={open} onClose={onClose} title="Registrar compra">
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>Proveedor *</span>
          <select className="input" value={supplierId} onChange={(e) => setSupplierId(e.target.value)} required>
            <option value="">Selecciona proveedor</option>
            {supplierOptions.map((supplier) => (
              <option key={supplier.id} value={supplier.id}>{supplier.name}</option>
            ))}
          </select>
        </label>

        <label>
          <span>Documento</span>
          <input className="input" value={docType} onChange={(e) => setDocType(e.target.value)} />
        </label>

        <label>
          <span>Número</span>
          <input className="input" value={docNumber} onChange={(e) => setDocNumber(e.target.value)} />
        </label>

        <label>
          <span>Emitida</span>
          <input className="input" type="datetime-local" value={issuedAt} onChange={(e) => setIssuedAt(e.target.value)} />
        </label>

        <div className="line"></div>

        <div className="item-builder">
          <select className="input" value={productId} onChange={(e) => setProductId(e.target.value)}>
            <option value="">Producto</option>
            {productOptions.map((product) => (
              <option key={product.id} value={product.id}>{product.name}</option>
            ))}
          </select>
          <input className="input" type="number" min={1} value={qty} onChange={(e) => setQty(Number(e.target.value))} />
          <input className="input" type="number" step="0.01" min={0} value={unitCost} onChange={(e) => setUnitCost(Number(e.target.value))} />
          <input className="input" type="date" value={expDate} onChange={(e) => setExpDate(e.target.value)} />
          <input className="input" type="number" step="0.1" min={0} value={vatRate} onChange={(e) => setVatRate(Number(e.target.value))} />
          <button type="button" className="btn" onClick={addItem}>Agregar</button>
        </div>

        {items.length > 0 && (
          <div className="table-wrapper compact">
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Cantidad</th>
                  <th>Costo</th>
                  <th>Vence</th>
                  <th>IVA %</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => {
                  const product = productOptions.find((p) => p.id === item.productId);
                  return (
                    <tr key={`${item.productId}-${idx}`}>
                      <td>{product?.name ?? item.productId}</td>
                      <td className="mono">{item.qty}</td>
                      <td className="mono">${item.unitCost.toFixed(2)}</td>
                      <td className="mono">{item.expDate ? new Date(item.expDate).toLocaleDateString() : "-"}</td>
                      <td className="mono">{(item.vatRate ?? vatRate).toFixed(1)}</td>
                      <td><button type="button" className="btn ghost" onClick={() => removeItem(idx)}>Quitar</button></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className="totals-grid">
          <div>
            <span className="muted small">Neto</span>
            <strong>${totals.net.toFixed(2)}</strong>
          </div>
          <div>
            <span className="muted small">IVA</span>
            <strong>${totals.vat.toFixed(2)}</strong>
          </div>
          <div>
            <span className="muted small">Total</span>
            <strong>${totals.total.toFixed(2)}</strong>
          </div>
        </div>

        {mutation.isError && <p className="error">{(mutation.error as Error).message}</p>}

        <div className="buttons">
          <button className="btn" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : "Registrar"}
          </button>
          <button className="btn ghost" type="button" onClick={resetForm}>Limpiar</button>
        </div>
      </form>
    </Modal>
  );
}
