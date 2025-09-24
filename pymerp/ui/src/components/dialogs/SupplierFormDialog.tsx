import { FormEvent, useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import {
  Supplier,
  SupplierPayload,
  createSupplier,
  updateSupplier,
} from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  supplier?: Supplier | null;
  onClose: () => void;
  onSaved?: (supplier: Supplier) => void;
};

const EMPTY_FORM: SupplierPayload = {
  name: "",
  rut: "",
};

export default function SupplierFormDialog({ open, supplier, onClose, onSaved }: Props) {
  const [form, setForm] = useState<SupplierPayload>(EMPTY_FORM);

  useEffect(() => {
    if (!open) return;
    if (supplier) {
      setForm({
        name: supplier.name ?? "",
        rut: supplier.rut ?? "",
      });
    } else {
      setForm(EMPTY_FORM);
    }
  }, [open, supplier]);

  const mutation = useMutation({
    mutationFn: (payload: SupplierPayload) => {
      if (supplier) {
        return updateSupplier(supplier.id, payload);
      }
      return createSupplier(payload);
    },
    onSuccess: (saved) => {
      onSaved?.(saved);
      onClose();
    },
  });

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    const name = form.name.trim();
    if (!name) {
      window.alert("El nombre es obligatorio");
      return;
    }
    mutation.mutate({
      name,
      rut: form.rut?.trim() || undefined,
    });
  };

  const title = supplier ? "Editar proveedor" : "Nuevo proveedor";

  return (
    <Modal open={open} title={title} onClose={() => !mutation.isPending && onClose()}>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          <span>Nombre *</span>
          <input
            className="input"
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Proveedor demo"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>RUT</span>
          <input
            className="input"
            value={form.rut ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, rut: e.target.value }))}
            placeholder="76.123.456-k"
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
        {mutation.isError && (
          <p className="error">{(mutation.error as Error)?.message ?? "No se pudo guardar"}</p>
        )}
      </form>
    </Modal>
  );
}
