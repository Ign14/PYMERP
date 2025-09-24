import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  createInventoryAdjustment,
  listProducts,
  InventoryAdjustmentPayload,
  Page,
  Product,
} from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  onClose: () => void;
  onApplied?: () => void;
};

type Direction = "increase" | "decrease";

const EMPTY_FORM = {
  productId: "",
  direction: "increase" as Direction,
  quantity: "0",
  unitCost: "0",
  reason: "",
  expDate: "",
};

export default function InventoryAdjustmentDialog({ open, onClose, onApplied }: Props) {
  const [form, setForm] = useState(EMPTY_FORM);

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM);
    }
  }, [open]);

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ["products", { dialog: "inventory-adjustment" }],
    queryFn: () => listProducts({ size: 200, status: "all" }),
    enabled: open,
  });

  const products = useMemo(() => productsQuery.data?.content ?? [], [productsQuery.data]);

  const mutation = useMutation({
    mutationFn: () => {
      if (!form.productId) {
        throw new Error("Selecciona un producto");
      }
      const quantity = Number(form.quantity);
      if (!Number.isFinite(quantity) || quantity <= 0) {
        throw new Error("La cantidad debe ser mayor a cero");
      }
      const direction = form.direction;
      const unitCost = Number(form.unitCost || 0);
      if (direction === "increase" && (Number.isNaN(unitCost) || unitCost < 0)) {
        throw new Error("El costo unitario debe ser positivo");
      }
      const payload: InventoryAdjustmentPayload = {
        productId: form.productId,
        quantity,
        direction,
        reason: form.reason.trim() || "Ajuste manual",
        unitCost: direction === "increase" ? unitCost : undefined,
        expDate: form.expDate || undefined,
      };
      return createInventoryAdjustment(payload);
    },
    onSuccess: () => {
      onApplied?.();
      setForm(EMPTY_FORM);
      onClose();
    },
  });

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isPending) return;
    mutation.mutate();
  };

  return (
    <Modal
      open={open}
      title="Ajuste de stock"
      onClose={() => {
        if (!mutation.isPending) {
          onClose();
        }
      }}
    >
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>Producto *</span>
          <select
            className="input"
            value={form.productId}
            onChange={(e) => setForm((prev) => ({ ...prev, productId: e.target.value }))}
            disabled={mutation.isPending}
            required
          >
            <option value="">Selecciona un producto</option>
            {products.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Tipo de ajuste</span>
          <select
            className="input"
            value={form.direction}
            onChange={(e) => setForm((prev) => ({ ...prev, direction: e.target.value as Direction }))}
            disabled={mutation.isPending}
          >
            <option value="increase">Incremento</option>
            <option value="decrease">Disminucion</option>
          </select>
        </label>

        <label>
          <span>Cantidad *</span>
          <input
            className="input"
            type="number"
            min="0"
            step="0.01"
            value={form.quantity}
            onChange={(e) => setForm((prev) => ({ ...prev, quantity: e.target.value }))}
            disabled={mutation.isPending}
            required
          />
        </label>

        {form.direction === "increase" && (
          <label>
            <span>Costo unitario</span>
            <input
              className="input"
              type="number"
              min="0"
              step="0.01"
              value={form.unitCost}
              onChange={(e) => setForm((prev) => ({ ...prev, unitCost: e.target.value }))}
              disabled={mutation.isPending}
            />
          </label>
        )}

        <label>
          <span>Fecha de vencimiento</span>
          <input
            className="input"
            type="date"
            value={form.expDate}
            onChange={(e) => setForm((prev) => ({ ...prev, expDate: e.target.value }))}
            disabled={mutation.isPending}
          />
        </label>

        <label>
          <span>Motivo</span>
          <textarea
            className="input"
            rows={3}
            value={form.reason}
            onChange={(e) => setForm((prev) => ({ ...prev, reason: e.target.value }))}
            placeholder="Regularizacion, inventario, merma, etc."
            disabled={mutation.isPending}
          />
        </label>

        {mutation.isError && (
          <p className="error">{(mutation.error as Error)?.message ?? "No se pudo registrar el ajuste"}</p>
        )}

        <div className="buttons">
          <button className="btn" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : "Registrar"}
          </button>
          <button className="btn ghost" type="button" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  );
}
