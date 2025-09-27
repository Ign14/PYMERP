import { FormEvent, useEffect, useMemo, useState } from "react";
import { Product } from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  product: Product | null;
  pendingValue?: number | null;
  submitting?: boolean;
  error?: string | null;
  onClose: () => void;
  onSubmit: (value: number) => void;
};

export default function ProductInventoryAlertModal({
  open,
  product,
  pendingValue,
  submitting = false,
  error,
  onClose,
  onSubmit,
}: Props) {
  const [value, setValue] = useState<string>("0");
  const [localError, setLocalError] = useState<string | null>(null);

  const initialValue = useMemo(() => {
    if (typeof pendingValue === "number") {
      return pendingValue;
    }
    const numeric = Number(product?.criticalStock ?? 0);
    return Number.isFinite(numeric) && numeric >= 0 ? numeric : 0;
  }, [pendingValue, product?.criticalStock]);

  useEffect(() => {
    if (!open || !product) {
      setValue("0");
      setLocalError(null);
      return;
    }
    setValue(String(initialValue));
    setLocalError(null);
  }, [open, product?.id, initialValue]);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const numeric = Number(value);
    if (!Number.isFinite(numeric) || numeric < 0) {
      setLocalError("Ingresa un stock válido (mayor o igual a 0)");
      return;
    }
    setLocalError(null);
    onSubmit(numeric);
  };

  const handleClose = () => {
    if (!submitting) {
      onClose();
    }
  };

  const title = product ? `Stock crítico - ${product.name}` : "Stock crítico";

  const hasError = !!localError || !!error;

  return (
    <Modal open={open} title={title} onClose={handleClose}>
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          <span>Stock crítico</span>
          <input
            className={`input${hasError ? " input-error" : ""}`}
            type="number"
            min="0"
            step="1"
            value={value}
            onChange={(event) => setValue(event.target.value)}
            disabled={submitting}
            data-autofocus
            inputMode="numeric"
          />
        </label>
        <div className="buttons">
          <button className="btn" type="submit" disabled={submitting}>
            {submitting ? "Guardando..." : "Guardar"}
          </button>
          <button className="btn ghost" type="button" onClick={handleClose} disabled={submitting}>
            Cancelar
          </button>
        </div>
        {localError && <p className="error">{localError}</p>}
        {!localError && error && <p className="error">{error}</p>}
      </form>
    </Modal>
  );
}
