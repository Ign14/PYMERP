import "./App.css";

import { Navigate, Route, Routes } from "react-router-dom";
import { Suspense, lazy, useMemo } from "react";
import { useAuth } from "./context/AuthContext";
import LayoutShell, { NAV_ITEMS } from "./layout/LayoutShell";

const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const SalesPage = lazy(() => import("./pages/SalesPage"));
const PurchasesPage = lazy(() => import("./pages/PurchasesPage"));
const InventoryPage = lazy(() => import("./pages/InventoryPage"));
const CustomersPage = lazy(() => import("./pages/CustomersPage"));
const SuppliersPage = lazy(() => import("./pages/SuppliersPage"));
const FinancesPage = lazy(() => import("./pages/FinancesPage"));
const ReportsPage = lazy(() => import("./pages/ReportsPage"));
const SettingsPage = lazy(() => import("./pages/SettingsPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));

type ModuleRoute = {
  index?: boolean;
  path?: string;
  module: string;
  element: JSX.Element;
};

const ROUTES: ModuleRoute[] = [
  { index: true, module: "dashboard", element: <DashboardPage /> },
  { path: "sales", module: "sales", element: <SalesPage /> },
  { path: "purchases", module: "purchases", element: <PurchasesPage /> },
  { path: "inventory", module: "inventory", element: <InventoryPage /> },
  { path: "customers", module: "customers", element: <CustomersPage /> },
  { path: "suppliers", module: "suppliers", element: <SuppliersPage /> },
  { path: "finances", module: "finances", element: <FinancesPage /> },
  { path: "reports", module: "reports", element: <ReportsPage /> },
  { path: "settings", module: "settings", element: <SettingsPage /> },
];

function RoutesShell() {
  const { isAuthenticated, session } = useAuth();
  const modules = session?.modules ?? [];

  const allowedModules = useMemo(() => {
    const set = new Set(modules);
    set.add("dashboard");
    return set;
  }, [modules]);

  const defaultPath = useMemo(() => {
    const first = NAV_ITEMS.find((item) => allowedModules.has(item.module));
    return first ? first.to : "/";
  }, [allowedModules]);

  return (
    <Suspense fallback={<div className="page"><p>Cargando...</p></div>}>
      <Routes>
        <Route path="/login" element={isAuthenticated ? <Navigate to={defaultPath} replace /> : <LoginPage />} />
        <Route
          path="/"
          element={isAuthenticated ? <LayoutShell /> : <Navigate to="/login" replace />}
        >
          {ROUTES.map((config) => {
            const element = allowedModules.has(config.module)
              ? config.element
              : <Navigate to={defaultPath} replace />;
            if (config.index) {
              return <Route key="index" index element={element} />;
            }
            return <Route key={config.path} path={config.path} element={element} />;
          })}
        </Route>
      </Routes>
    </Suspense>
  );
}

export default function App() {
  return <RoutesShell />;
}
