import axios from "axios";
import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "admin@dev.local", password: "Admin1234" });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await login({ email: form.email.trim(), password: form.password });
      navigate("/");
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && !err.response) {
        setError("No se pudo conectar con el servidor. Verifica tu conexión e inténtalo nuevamente.");
        return;
      }
      const detail = (err as any)?.response?.data?.detail ?? (err as Error)?.message ?? "Credenciales inválidas";
      setError(detail);
    } finally {
      setLoading(false);
    }
  };

  if (isAuthenticated) {
    navigate("/");
  }

  return (
    <div className="auth-container">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>Iniciar sesión</h1>
        <p className="muted">Usa las credenciales de tu cuenta para acceder al panel.</p>

        <label className="auth-label">
          <span>Email</span>
          <input
            className="input"
            type="email"
            value={form.email}
            onChange={(e) => setForm((prev) => ({ ...prev, email: e.target.value }))}
            autoComplete="email"
            required
          />
        </label>

        <label className="auth-label">
          <span>Contraseña</span>
          <input
            className="input"
            type="password"
            value={form.password}
            onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
            autoComplete="current-password"
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button className="btn" type="submit" disabled={loading}>
          {loading ? "Ingresando..." : "Entrar"}
        </button>

        <p className="muted small">
          ¿Problemas? Contacta al administrador de tu compañía.
        </p>
        <p className="muted small">
          Dev quicklinks: <Link to="/" className="btn-link">Dashboard</Link>
        </p>
      </form>
    </div>
  );
}
