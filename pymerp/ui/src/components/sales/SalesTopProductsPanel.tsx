import { useMemo } from "react";

type ProductStat = {
  productId: string;
  productName: string;
  sku: string;
  totalRevenue: number;
  totalQuantity: number;
  salesCount: number;
};

type SalesTopProductsPanelProps = {
  startDate?: string;
  endDate?: string;
};

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString("es-CL")}`;
}

// Datos de demostración para top productos
function generateTopProducts(): ProductStat[] {
  return [
    { productId: "P001", productName: "Laptop Dell Inspiron 15", sku: "DELL-INS-15", totalRevenue: 8500000, totalQuantity: 12, salesCount: 12 },
    { productId: "P002", productName: "Monitor Samsung 27\"", sku: "SAM-MON-27", totalRevenue: 6200000, totalQuantity: 28, salesCount: 28 },
    { productId: "P003", productName: "Teclado Mecánico Logitech", sku: "LOG-KB-MEC", totalRevenue: 4800000, totalQuantity: 65, salesCount: 65 },
    { productId: "P004", productName: "Mouse Inalámbrico HP", sku: "HP-MOUSE-WL", totalRevenue: 3500000, totalQuantity: 120, salesCount: 85 },
    { productId: "P005", productName: "Webcam HD Logitech C920", sku: "LOG-CAM-920", totalRevenue: 2900000, totalQuantity: 42, salesCount: 42 },
    { productId: "P006", productName: "Auriculares Sony WH-1000XM4", sku: "SONY-AUR-XM4", totalRevenue: 2600000, totalQuantity: 18, salesCount: 18 },
    { productId: "P007", productName: "SSD Kingston 1TB", sku: "KING-SSD-1TB", totalRevenue: 2100000, totalQuantity: 38, salesCount: 38 },
    { productId: "P008", productName: "Impresora HP LaserJet Pro", sku: "HP-PRINT-LJ", totalRevenue: 1800000, totalQuantity: 8, salesCount: 8 },
    { productId: "P009", productName: "Router WiFi 6 TP-Link", sku: "TP-ROUTER-W6", totalRevenue: 1500000, totalQuantity: 24, salesCount: 24 },
    { productId: "P010", productName: "Cable HDMI 2.1 Premium", sku: "HDMI-21-PREM", totalRevenue: 950000, totalQuantity: 95, salesCount: 75 },
  ];
}

export default function SalesTopProductsPanel({ startDate, endDate }: SalesTopProductsPanelProps) {
  const topProducts = useMemo(() => generateTopProducts(), []);
  const maxRevenue = topProducts.length > 0 ? topProducts[0].totalRevenue : 1;

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">🏆 Top 10 Productos más vendidos</h2>
        <p className="text-sm text-neutral-400">Ranking por ingresos totales generados</p>
      </header>

      <div className="space-y-3">
        {topProducts.map((product, index) => {
          const percentage = (product.totalRevenue / maxRevenue) * 100;
          return (
            <div
              key={product.productId}
              className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 hover:border-neutral-600 transition-colors"
            >
              <div className="flex items-start justify-between mb-2">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-neutral-700 text-neutral-300 text-xs font-bold">
                      {index + 1}
                    </span>
                    <h3 className="text-neutral-100 font-medium">{product.productName}</h3>
                  </div>
                  <p className="text-xs text-neutral-400">SKU: {product.sku}</p>
                </div>
                <div className="text-right">
                  <p className="text-lg font-bold text-neutral-100">{formatCurrency(product.totalRevenue)}</p>
                  <p className="text-xs text-neutral-400">{product.totalQuantity.toLocaleString()} unidades</p>
                </div>
              </div>
              
              {/* Barra de progreso */}
              <div className="relative w-full h-2 bg-neutral-700 rounded-full overflow-hidden">
                <div
                  className="absolute top-0 left-0 h-full bg-gradient-to-r from-cyan-500 to-blue-500 rounded-full transition-all"
                  style={{ width: `${percentage}%` }}
                ></div>
              </div>
              
              <div className="flex justify-between items-center mt-2">
                <span className="text-xs text-neutral-400">{product.salesCount} venta{product.salesCount > 1 ? "s" : ""}</span>
                <span className="text-xs text-neutral-300">{percentage.toFixed(1)}%</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
