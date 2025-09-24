import { FormEvent, useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import {
  Product,
  ProductPayload,
  createProduct,
  updateProduct,
} from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  product?: Product | null;
  onClose: () => void;
  onSaved?: (product: Product) => void;
};

const EMPTY_FORM: ProductPayload = {
  sku: "",
  name: "",
  description: "",
  category: "",
  barcode: "",
  imageUrl: "",
};

export default function ProductFormDialog({ open, product, onClose, onSaved }: Props) {
  const [form, setForm] = useState<ProductPayload>(EMPTY_FORM);

  useEffect(() => {
    if (!open) return;
    if (product) {
      setForm({
        sku: product.sku ?? "",
        name: product.name ?? "",
        description: product.description ?? "",
        category: product.category ?? "",
        barcode: product.barcode ?? "",
        imageUrl: product.imageUrl ?? "",
      });
    } else {
      setForm(EMPTY_FORM);
    }
  }, [open, product]);

  const mutation = useMutation({
    mutationFn: (payload: ProductPayload) => {
      if (product) {
        return updateProduct(product.id, payload);
      }
      return createProduct(payload);
    },
    onSuccess: (saved) => {
      onSaved?.(saved);
      onClose();
    },
  });

  useEffect(() => {
    if (!open) {
      mutation.reset();
    }
  }, [open, mutation]);

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    const sku = form.sku.trim();
    const name = form.name.trim();
    if (!sku) {
      window.alert("El SKU es obligatorio");
      return;
    }
    if (!name) {
      window.alert("El nombre es obligatorio");
      return;
    }
    mutation.mutate({
      sku,
      name,
      description: form.description?.trim() || undefined,
      category: form.category?.trim() || undefined,
      barcode: form.barcode?.trim() || undefined,
      imageUrl: form.imageUrl?.trim() || undefined,
    });
  };

  const title = product ? "Editar producto" : "Nuevo producto";

  return (
    <Modal open={open} title={title} onClose={() => !mutation.isPending && onClose()}>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          <span>SKU *</span>
          <input
            className="input"
            value={form.sku}
            onChange={(e) => setForm((prev) => ({ ...prev, sku: e.target.value }))}
            placeholder="SKU-001"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Nombre *</span>
          <input
            className="input"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Producto demo"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Categoria</span>
          <input
            className="input"
            value={form.category ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, category: e.target.value }))}
            placeholder="Bebidas"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Codigo de barras</span>
          <input
            className="input"
            value={form.barcode ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, barcode: e.target.value }))}
            placeholder="7890000000"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Descripcion</span>
          <textarea
            className="input"
            value={form.description ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
            placeholder="Notas internas"
            rows={3}
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Imagen (URL)</span>
          <input
            className="input"
            value={form.imageUrl ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, imageUrl: e.target.value }))}
            placeholder="https://..."
            disabled={mutation.isPending}
          />
        </label>
        <div className="buttons">
          <button className="btn" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : "Guardar"}
          </button>
          <button className="btn ghost" type="button" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </button>
        </div>
        {mutation.isError && <p className="error">{resolveErrorMessage(mutation.error)}</p>}
      </form>
    </Modal>
  );
}

function resolveErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const detail = error.response?.data as { detail?: string; message?: string; error?: string } | string | undefined;
    if (typeof detail === "string" && detail.trim().length > 0) {
      return detail;
    }
    if (detail && typeof detail === "object") {
      if (typeof detail.detail === "string" && detail.detail.trim().length > 0) {
        return detail.detail;
      }
      if (typeof detail.message === "string" && detail.message.trim().length > 0) {
        return detail.message;
      }
      if (typeof detail.error === "string" && detail.error.trim().length > 0) {
        return detail.error;
      }
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "No se pudo guardar";
}
