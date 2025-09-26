import { useMemo, useState } from "react";
import logo from "../../assets/logo.png";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export const NAV_ITEMS = [
  { to: "/", label: "Panel", icon: "DB", module: "dashboard" },
  { to: "/sales", label: "Ventas", icon: "VE", module: "sales" },
  { to: "/purchases", label: "Compras", icon: "CO", module: "purchases" },
  { to: "/inventory", label: "Inventario", icon: "IN", module: "inventory" },
  { to: "/customers", label: "Clientes", icon: "CL", module: "customers" },
  { to: "/suppliers", label: "Proveedores", icon: "PR", module: "suppliers" },
  { to: "/finances", label: "Finanzas", icon: "FI", module: "finances" },
  { to: "/reports", label: "Reportes", icon: "RP", module: "reports" },
  { to: "/settings", label: "Configuracion", icon: "CF", module: "settings" },
] as const;

export default function LayoutShell() {
  const { session, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  const allowedModules = useMemo(() => {
    const modules = new Set(session?.modules ?? []);
    modules.add("dashboard");
    return modules;
  }, [session?.modules]);

  const visibleNavItems = useMemo(() => {
    if (!allowedModules || allowedModules.size === 0) {
      return NAV_ITEMS;
    }
    return NAV_ITEMS.filter((item) => allowedModules.has(item.module));
  }, [allowedModules]);

  return (
    <div className="layout">
      <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="sidebar-header">
          <img src={logo} alt="Logo empresa" className="brand-logo brand-logo--sidebar" />
          <button className="icon-btn mobile-only" onClick={() => setSidebarOpen(false)} aria-label="Cerrar menu">
            Close
          </button>
        </div>
        <nav>
          {visibleNavItems.map((item) => {
            const isRoot = item.to === "/";
            const isActive = location.pathname === item.to || (!isRoot && location.pathname.startsWith(item.to));
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={`nav-item ${isActive ? "active" : ""}`}
                onClick={() => setSidebarOpen(false)}
                end={isRoot}
              >
                <span className="nav-icon" aria-hidden>{item.icon}</span>
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </aside>

      <div className="content">
        <header className="topbar">
          <div className="topbar-left">
            <button className="icon-btn desktop-hidden" onClick={() => setSidebarOpen(true)} aria-label="Abrir menu">
              Menu
            </button>
            <div className="topbar-brand">
              <img src={logo} alt="Logo empresa" className="brand-logo brand-logo--topbar" />
              <p className="muted small">Control integral de operaciones y finanzas</p>
            </div>
          </div>
          <div className="topbar-right">
            <span className="muted small hide-mobile">{session?.email}</span>
            <button className="btn ghost" onClick={logout}>Salir</button>
          </div>
        </header>

        <main className="page">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
