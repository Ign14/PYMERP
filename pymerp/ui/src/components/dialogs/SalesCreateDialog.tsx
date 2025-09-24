import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  createSale,
  listCustomers,
  listProducts,
  Page,
  Product,
  Customer,
  SaleItemPayload,
  SalePayload,
  SaleRes,
} from "../../services/client";
import Modal from "./Modal";
import { SALE_DOCUMENT_TYPES, SALE_PAYMENT_METHODS } from "../../constants/sales";

interface Props {
  open: boolean;
  onClose: () => void;
  onCreated: (sale: SaleRes) => void;
}

type DocumentOption = (typeof SALE_DOCUMENT_TYPES)[number];
type PaymentOption = (typeof SALE_PAYMENT_METHODS)[number];

const DEFAULT_DOC_TYPE: DocumentOption["value"] = SALE_DOCUMENT_TYPES[0].value;
const DEFAULT_PAYMENT_METHOD: PaymentOption["value"] = SALE_PAYMENT_METHODS[2].value;

export default function SalesCreateDialog({ open, onClose, onCreated }: Props) {
  const [customerId, setCustomerId] = useState<string>("");
  const [docType, setDocType] = useState<DocumentOption["value"]>(DEFAULT_DOC_TYPE);
  const [paymentMethod, setPaymentMethod] = useState<PaymentOption["value"]>(DEFAULT_PAYMENT_METHOD);
  const [items, setItems] = useState<SaleItemPayload[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<string>("");
  const [qty, setQty] = useState(1);
  const [unitPrice, setUnitPrice] = useState<number>(0);

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ["products", { dialog: "sales" }],
    queryFn: () => listProducts({ size: 200 }),
    enabled: open,
    staleTime: 30_000,
  });

  const customersQuery = useQuery<Page<Customer>, Error>({
    queryKey: ["customers", { dialog: "sales" }],
    queryFn: () => listCustomers({ size: 200 }),
    enabled: open,
    staleTime: 30_000,
  });

  const createMutation = useMutation({
    mutationFn: (payload: SalePayload) => createSale(payload),
    onSuccess: (sale) => {
      onCreated(sale);
      resetForm();
      onClose();
    },
  });

  const productOptions = productsQuery.data?.content ?? [];
  const customerOptions = customersQuery.data?.content ?? [];

  const total = useMemo(() => {
    return items.reduce((acc, item) => acc + item.qty * item.unitPrice - (item.discount ?? 0), 0);
  }, [items]);

  const addItem = () => {
    if (!selectedProductId) return;
    const product = productOptions.find((p) => p.id === selectedProductId);
    if (!product) return;
    if (qty <= 0 || unitPrice <= 0) return;
    setItems((prev) => [...prev, { productId: selectedProductId, qty, unitPrice }]);
    setSelectedProductId("");
    setQty(1);
    setUnitPrice(Number(product.currentPrice ?? 0));
  };

  const removeItem = (index: number) => {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const resetForm = () => {
    setCustomerId("");
    setDocType(DEFAULT_DOC_TYPE);
    setPaymentMethod(DEFAULT_PAYMENT_METHOD);
    setItems([]);
    setSelectedProductId("");
    setQty(1);
    setUnitPrice(0);
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!customerId || items.length === 0) {
      return;
    }
    const payload: SalePayload = {
      customerId,
      docType,
      paymentMethod,
      items,
    };
    createMutation.mutate(payload);
  };

  return (
    <Modal open={open} onClose={onClose} title="Registrar venta">
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>Cliente *</span>
          <select className="input" value={customerId} onChange={(e) => setCustomerId(e.target.value)} required>
            <option value="">Selecciona cliente</option>
            {customerOptions.map((customer) => (
              <option key={customer.id} value={customer.id}>{customer.name}</option>
            ))}
          </select>
        </label>

        <label>
          <span>Documento</span>
          <select className="input" value={docType} onChange={(e) => setDocType(e.target.value as DocumentOption["value"])}>
            {SALE_DOCUMENT_TYPES.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>

        <label>
          <span>Metodo de pago</span>
          <select className="input" value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value as PaymentOption["value"])}>
            {SALE_PAYMENT_METHODS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>

        <div className="line"></div>

        <div className="item-builder">
          <select className="input" value={selectedProductId} onChange={(e) => {
            const id = e.target.value;
            setSelectedProductId(id);
            const product = productOptions.find((p) => p.id === id);
            if (product?.currentPrice) {
              setUnitPrice(Number(product.currentPrice));
            }
          }}>
            <option value="">Selecciona producto</option>
            {productOptions.map((product) => (
              <option key={product.id} value={product.id}>{product.name}</option>
            ))}
          </select>
          <input className="input" type="number" min={1} value={qty} onChange={(e) => setQty(Number(e.target.value))} />
          <input className="input" type="number" step="0.01" min={0} value={unitPrice} onChange={(e) => setUnitPrice(Number(e.target.value))} />
          <button type="button" className="btn" onClick={addItem}>Agregar</button>
        </div>

        {items.length > 0 && (
          <div className="table-wrapper compact">
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Cantidad</th>
                  <th>Precio</th>
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
                      <td className="mono">${item.unitPrice.toFixed(2)}</td>
                      <td>
                        <button type="button" className="btn ghost" onClick={() => removeItem(idx)}>Quitar</button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className="total-section">
          <span className="muted">Total estimado</span>
          <strong>${total.toFixed(2)}</strong>
        </div>

        {createMutation.isError && <p className="error">{(createMutation.error as Error).message}</p>}

        <div className="buttons">
          <button className="btn" type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Guardando..." : "Registrar"}
          </button>
          <button className="btn ghost" type="button" onClick={resetForm}>
            Limpiar
          </button>
        </div>
      </form>
    </Modal>
  );
}
