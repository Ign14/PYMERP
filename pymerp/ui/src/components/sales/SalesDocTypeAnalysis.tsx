import { useMemo } from "react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

type DocTypeStat = {
  type: string;
  count: number;
  total: number;
};

type SalesDocTypeAnalysisProps = {
  startDate?: string;
  endDate?: string;
};

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString("es-CL")}`;
}

// Datos de demostración
function generateDocTypeStats(): DocTypeStat[] {
  return [
    { type: "Factura Electrónica", count: 185, total: 42500000 },
    { type: "Boleta Electrónica", count: 312, total: 18900000 },
    { type: "Nota de Crédito", count: 28, total: 3200000 },
    { type: "Nota de Débito", count: 15, total: 1500000 },
    { type: "Guía de Despacho", count: 94, total: 0 },
  ];
}

export default function SalesDocTypeAnalysis({ startDate, endDate }: SalesDocTypeAnalysisProps) {
  const docTypeStats = useMemo(() => generateDocTypeStats(), []);

  const chartData = docTypeStats.map((stat) => ({
    name: stat.type,
    cantidad: stat.count,
    monto: stat.total / 1000000, // Convertir a millones para mejor visualización
  }));

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">📋 Análisis de Tipos de Documento</h2>
        <p className="text-sm text-neutral-400">Desglose por tipo de documento tributario</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Gráfico de barras */}
        <div className="lg:col-span-2 bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <div style={{ height: 300 }} role="img" aria-label="Cantidad de documentos por tipo">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} layout="vertical" margin={{ top: 10, right: 10, left: 120, bottom: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
                <XAxis type="number" stroke="#a3a3a3" tick={{ fill: "#a3a3a3", fontSize: 12 }} />
                <YAxis dataKey="name" type="category" stroke="#a3a3a3" tick={{ fill: "#a3a3a3", fontSize: 12 }} width={110} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#171717",
                    border: "1px solid #404040",
                    borderRadius: "0.5rem",
                    color: "#f5f5f5",
                  }}
                  formatter={(value: number, name: string) => {
                    if (name === "cantidad") return [value, "Cantidad"];
                    if (name === "monto") return [`${value.toFixed(1)}M`, "Monto (millones)"];
                    return [value, name];
                  }}
                />
                <Bar dataKey="cantidad" fill="#22d3ee" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Lista detallada */}
        <div className="space-y-3">
          {docTypeStats.map((stat) => (
            <div
              key={stat.type}
              className="bg-neutral-800 border border-neutral-700 rounded-lg p-4"
            >
              <h3 className="text-neutral-100 font-medium mb-2">{stat.type}</h3>
              <div className="space-y-1">
                <div className="flex justify-between items-center">
                  <span className="text-xs text-neutral-400">Cantidad</span>
                  <span className="text-sm font-semibold text-neutral-100">{stat.count.toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-neutral-400">Monto total</span>
                  <span className="text-sm font-semibold text-neutral-100">
                    {stat.total > 0 ? formatCurrency(stat.total) : "N/A"}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
