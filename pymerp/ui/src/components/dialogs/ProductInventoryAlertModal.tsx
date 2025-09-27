import { FormEvent, useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { Product, updateProductInventoryAlert } from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  product: Product | null;
  onClose: () => void;
  onSaved?: (product: Product) => void;
};

export default function ProductInventoryAlertModal({ open, product, onClose, onSaved }: Props) {
  const [value, setValue] = useState<string>("0");
  const [localError, setLocalError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (criticalStock: number) => {
      if (!product) throw new Error("Producto no seleccionado");
      return updateProductInventoryAlert(product.id, criticalStock);
    },
    onSuccess: (updated) => {
      onSaved?.(updated);
      onClose();
    },
  });

  useEffect(() => {
    if (!open || !product) {
      setValue("0");
      setLocalError(null);
      return;
    }
    const numeric = Number(product.criticalStock ?? 0);
    setValue(Number.isFinite(numeric) && numeric >= 0 ? numeric.toString() : "0");
    setLocalError(null);
    mutation.reset();
  }, [open, product]);

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    const numeric = Number(value);
    if (!Number.isFinite(numeric) || numeric < 0) {
      setLocalError("Ingresa un stock válido (mayor o igual a 0)");
      return;
    }
    setLocalError(null);
    mutation.mutate(numeric);
  };

  const title = product ? `Stock crítico - ${product.name}` : "Stock crítico";

  return (
    <Modal open={open} title={title} onClose={() => !mutation.isPending && onClose()}>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          <span>Stock crítico</span>
          <input
            className="input"
            type="number"
            min="0"
            step="1"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            disabled={mutation.isPending}
            autoFocus
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
        {localError && <p className="error">{localError}</p>}
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
