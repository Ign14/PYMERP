import { forwardRef, useCallback, useEffect, useImperativeHandle, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { deleteSupplier, listSuppliers, Supplier } from "../services/client";
import SupplierFormDialog from "./dialogs/SupplierFormDialog";

type Props = Record<string, never>;

export type SuppliersCardHandle = {
  openCreate: () => void;
};

const SuppliersCard = forwardRef<SuppliersCardHandle, Props>((_, ref) => {
  const queryClient = useQueryClient();
  const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);

  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ["suppliers"],
    queryFn: listSuppliers,
    refetchOnWindowFocus: false,
  });

  const openCreate = useCallback(() => {
    setEditingSupplier(null);
    setDialogOpen(true);
  }, []);

  const closeDialog = useCallback(() => {
    setDialogOpen(false);
    setEditingSupplier(null);
  }, []);

  useImperativeHandle(ref, () => ({
    openCreate,
  }), [openCreate]);

  useEffect(() => {
    if (!selectedSupplier) return;
    const updated = suppliersQuery.data?.find((item) => item.id === selectedSupplier.id);
    if (!updated) {
      setSelectedSupplier(null);
    } else if (updated !== selectedSupplier) {
      setSelectedSupplier(updated);
    }
  }, [suppliersQuery.data, selectedSupplier]);

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteSupplier(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
      if (selectedSupplier?.id === id) {
        setSelectedSupplier(null);
      }
    },
  });

  const handleSaved = (supplier: Supplier) => {
    queryClient.invalidateQueries({ queryKey: ["suppliers"] });
    setSelectedSupplier(supplier);
    closeDialog();
  };

  const handleEdit = () => {
    if (!selectedSupplier) return;
    setEditingSupplier(selectedSupplier);
    setDialogOpen(true);
  };

  const handleDelete = () => {
    if (!selectedSupplier || deleteMutation.isPending) return;
    if (window.confirm(`Eliminar proveedor ${selectedSupplier.name}?`)) {
      deleteMutation.mutate(selectedSupplier.id);
    }
  };

  const { data, isLoading, isError, error, refetch, isFetching } = suppliersQuery;

  return (
    <div className="card">
      <div className="card-header">
        <h2>Proveedores</h2>
        <button className="btn" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? "Cargando" : "Refrescar"}
        </button>
      </div>

      <div className="inline-actions">
        <button className="btn" type="button" onClick={openCreate}>
          + Proveedor
        </button>
        <button className="btn ghost" type="button" onClick={handleEdit} disabled={!selectedSupplier}>
          Editar
        </button>
        <button
          className="btn ghost"
          type="button"
          onClick={handleDelete}
          disabled={!selectedSupplier || deleteMutation.isPending}
        >
          {deleteMutation.isPending ? "Eliminando..." : "Eliminar"}
        </button>
      </div>
      {deleteMutation.isError && (
        <p className="error">{(deleteMutation.error as Error)?.message ?? "No se pudo eliminar el proveedor"}</p>
      )}

      {isLoading && <p>Loading...</p>}
      {isError && <p className="error">{error?.message ?? "No se pudieron cargar los proveedores"}</p>}

      {!isLoading && !isError && (
        <ul className="list">
          {(data ?? []).map((supplier) => (
            <li
              key={supplier.id}
              className={selectedSupplier?.id === supplier.id ? "selected" : undefined}
              onClick={() => setSelectedSupplier(supplier)}
            >
              <strong>{supplier.name}</strong>
              {supplier.rut ? <span className="mono small"> rut {supplier.rut}</span> : null}
            </li>
          ))}
          {(data ?? []).length === 0 && <li className="muted">Sin proveedores</li>}
        </ul>
      )}

      <SupplierFormDialog
        open={dialogOpen}
        supplier={editingSupplier}
        onClose={closeDialog}
        onSaved={handleSaved}
      />
    </div>
  );
});

SuppliersCard.displayName = "SuppliersCard";

export default SuppliersCard;
