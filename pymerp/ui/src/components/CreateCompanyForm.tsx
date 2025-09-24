import { FormEvent, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createCompany, type CreateCompanyPayload } from "../services/client";

type Props = {
  onCreated?: () => void;
};

export default function CreateCompanyForm({ onCreated }: Props) {
  const [form, setForm] = useState<CreateCompanyPayload>({ name: "", rut: "" });
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createCompany,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["companies"], exact: false });
      setForm({ name: "", rut: "" });
      setNotice("Company created");
      setError(null);
      onCreated?.();
    },
    onError: (err: Error) => {
      setError(err.message ?? "Could not create company");
      setNotice(null);
    },
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setNotice(null);
    setError(null);
    const name = form.name?.trim();
    if (!name) {
      setError("Name is required");
      return;
    }
    mutation.mutate({
      name,
      rut: form.rut?.trim() || undefined,
    });
  };

  return (
    <div className="card">
      <h2>New company</h2>
      <form onSubmit={submit} className="form-grid">
        <label>
          <span>Name *</span>
          <input
            value={form.name}
            onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
            placeholder="Dev Company"
            className="input"
          />
        </label>
        <label>
          <span>RUT</span>
          <input
            value={form.rut ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, rut: e.target.value }))}
            placeholder="76.000.000-0"
            className="input"
          />
        </label>
        <div className="buttons">
          <button className="btn" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Saving..." : "Create"}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => {
              setForm({ name: "", rut: "" });
              setNotice(null);
              setError(null);
            }}
          >
            Clear
          </button>
        </div>
        {error && <p className="error">{error}</p>}
        {notice && <p className="success">{notice}</p>}
      </form>
    </div>
  );
}
