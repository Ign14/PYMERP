import { useMemo } from "react";

type CustomerStat = {
  customerId: string;
  customerName: string;
  totalRevenue: number;
  purchaseCount: number;
  avgTicket: number;
  lastPurchaseDate: string;
};

type SalesTopCustomersPanelProps = {
  startDate?: string;
  endDate?: string;
};

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString("es-CL")}`;
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString("es-CL", { day: "numeric", month: "short" });
}

// Datos de demostración
function generateTopCustomers(): CustomerStat[] {
  return [
    { customerId: "C001", customerName: "Empresa TechSolutions SpA", totalRevenue: 15800000, purchaseCount: 42, avgTicket: 376190, lastPurchaseDate: "2024-01-25" },
    { customerId: "C002", customerName: "Comercial Del Norte Ltda", totalRevenue: 12400000, purchaseCount: 38, avgTicket: 326316, lastPurchaseDate: "2024-01-24" },
    { customerId: "C003", customerName: "Servicios Integrales SA", totalRevenue: 9800000, purchaseCount: 28, avgTicket: 350000, lastPurchaseDate: "2024-01-23" },
    { customerId: "C004", customerName: "Distribuidora Central", totalRevenue: 8500000, purchaseCount: 52, avgTicket: 163462, lastPurchaseDate: "2024-01-25" },
    { customerId: "C005", customerName: "Constructora Horizonte", totalRevenue: 7200000, purchaseCount: 15, avgTicket: 480000, lastPurchaseDate: "2024-01-22" },
    { customerId: "C006", customerName: "Retail Express SpA", totalRevenue: 6900000, purchaseCount: 64, avgTicket: 107813, lastPurchaseDate: "2024-01-25" },
    { customerId: "C007", customerName: "Alimentos Frescos SA", totalRevenue: 5800000, purchaseCount: 31, avgTicket: 187097, lastPurchaseDate: "2024-01-21" },
    { customerId: "C008", customerName: "Transporte Rápido Ltda", totalRevenue: 4500000, purchaseCount: 22, avgTicket: 204545, lastPurchaseDate: "2024-01-20" },
    { customerId: "C009", customerName: "Farmacia Salud Total", totalRevenue: 3800000, purchaseCount: 48, avgTicket: 79167, lastPurchaseDate: "2024-01-25" },
    { customerId: "C010", customerName: "Mueblería El Roble", totalRevenue: 3200000, purchaseCount: 9, avgTicket: 355556, lastPurchaseDate: "2024-01-19" },
  ];
}

export default function SalesTopCustomersPanel({ startDate, endDate }: SalesTopCustomersPanelProps) {
  const topCustomers = useMemo(() => generateTopCustomers(), []);
  const maxRevenue = topCustomers.length > 0 ? topCustomers[0].totalRevenue : 1;

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">👥 Top 10 Clientes</h2>
        <p className="text-sm text-neutral-400">Clientes con mayor volumen de compras</p>
      </header>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-neutral-700">
              <th className="text-left py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">#</th>
              <th className="text-left py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">Cliente</th>
              <th className="text-right py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">Ingresos</th>
              <th className="text-center py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">Compras</th>
              <th className="text-right py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">Ticket Prom.</th>
              <th className="text-center py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">Última Compra</th>
            </tr>
          </thead>
          <tbody>
            {topCustomers.map((customer, index) => {
              const percentage = (customer.totalRevenue / maxRevenue) * 100;
              const isRecent = new Date(customer.lastPurchaseDate) > new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);
              
              return (
                <tr
                  key={customer.customerId}
                  className="border-b border-neutral-800 hover:bg-neutral-800 transition-colors"
                >
                  <td className="py-4 px-2">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-neutral-700 text-neutral-300 text-xs font-bold">
                        {index + 1}
                      </span>
                      {index < 3 && (
                        <span className="text-lg">
                          {index === 0 ? "🥇" : index === 1 ? "🥈" : "🥉"}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="py-4 px-2">
                    <div>
                      <p className="text-neutral-100 font-medium">{customer.customerName}</p>
                      <div className="relative w-full h-1.5 bg-neutral-700 rounded-full overflow-hidden mt-1">
                        <div
                          className="absolute top-0 left-0 h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full"
                          style={{ width: `${percentage}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 px-2 text-right">
                    <p className="text-neutral-100 font-bold">{formatCurrency(customer.totalRevenue)}</p>
                  </td>
                  <td className="py-4 px-2 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-neutral-800 text-neutral-300 border border-neutral-700">
                      {customer.purchaseCount}
                    </span>
                  </td>
                  <td className="py-4 px-2 text-right">
                    <p className="text-neutral-300">{formatCurrency(customer.avgTicket)}</p>
                  </td>
                  <td className="py-4 px-2 text-center">
                    <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium ${
                      isRecent
                        ? "bg-green-950 text-green-400 border border-green-800"
                        : "bg-neutral-800 text-neutral-400 border border-neutral-700"
                    }`}>
                      {isRecent && "🟢 "}
                      {formatDate(customer.lastPurchaseDate)}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
