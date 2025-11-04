import { useMemo, useState } from "react";
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  SortingState,
  useReactTable,
} from "@tanstack/react-table";
import { Customer, CustomerStats } from "../../services/client";

type Props = {
  customers: Customer[];
  customerStats: Record<string, CustomerStats>;
  onCustomerSelect: (customer: Customer) => void;
  onCustomerEdit: (customer: Customer) => void;
  onCustomerDelete: (customerId: string) => void;
  selectedCustomerId: string | null;
  getHealthStatus: (lastSaleDate?: string) => {
    status: string;
    label: string;
    color: string;
    icon: string;
  };
  calculateRFM?: (stats?: CustomerStats) => {
    recency: number;
    frequency: number;
    monetary: number;
    recencyScore: number;
    frequencyScore: number;
    monetaryScore: number;
    score: string;
    segment: string;
  };
};

export default function CustomersTableView({
  customers,
  customerStats,
  onCustomerSelect,
  onCustomerEdit,
  onCustomerDelete,
  selectedCustomerId,
  getHealthStatus,
  calculateRFM,
}: Props) {
  const [sorting, setSorting] = useState<SortingState>([]);

  const columns = useMemo<ColumnDef<Customer>[]>(
    () => [
      {
        accessorKey: "name",
        header: "Nombre",
        cell: (info) => (
          <div className="flex items-center gap-2">
            <strong className="text-neutral-100">{info.getValue() as string}</strong>
            {customerStats[info.row.original.id] && (
              <span
                className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium border ${
                  getHealthStatus(customerStats[info.row.original.id]?.lastSaleDate || undefined).color
                }`}
              >
                <span>{getHealthStatus(customerStats[info.row.original.id]?.lastSaleDate || undefined).icon}</span>
                <span>{getHealthStatus(customerStats[info.row.original.id]?.lastSaleDate || undefined).label}</span>
              </span>
            )}
          </div>
        ),
      },
      {
        accessorKey: "rut",
        header: "RUT",
        cell: (info) => <span className="mono text-sm text-neutral-300">{info.getValue() as string || "-"}</span>,
      },
      {
        accessorKey: "email",
        header: "Email",
        cell: (info) => <span className="mono text-sm text-neutral-400">{info.getValue() as string || "-"}</span>,
      },
      {
        accessorKey: "phone",
        header: "Teléfono",
        cell: (info) => <span className="mono text-sm text-neutral-400">{info.getValue() as string || "-"}</span>,
      },
      {
        accessorKey: "segment",
        header: "Segmento",
        cell: (info) => <span className="text-sm text-neutral-300">{info.getValue() as string || "-"}</span>,
      },
      {
        id: "totalSales",
        header: "Ventas",
        accessorFn: (row) => customerStats[row.id]?.totalSales || 0,
        cell: (info) => <span className="text-neutral-100 font-semibold">{info.getValue() as number}</span>,
      },
      {
        id: "totalRevenue",
        header: "Ingresos",
        accessorFn: (row) => customerStats[row.id]?.totalRevenue || 0,
        cell: (info) => (
          <span className="text-neutral-100 font-semibold">
            ${(info.getValue() as number).toLocaleString("es-CL")}
          </span>
        ),
      },
      {
        id: "rfm",
        header: "RFM Score",
        accessorFn: (row) => {
          if (!calculateRFM) return "";
          return calculateRFM(customerStats[row.id])?.score || "000";
        },
        cell: ({ row }) => {
          if (!calculateRFM) return <span className="text-neutral-500">-</span>;
          const rfm = calculateRFM(customerStats[row.original.id]);
          return (
            <div className="flex flex-col gap-1">
              <span className="mono text-sm font-bold text-neutral-100">{rfm.score}</span>
              <span className="text-xs text-neutral-400">{rfm.segment}</span>
            </div>
          );
        },
      },
      {
        accessorKey: "createdAt",
        header: "Registrado",
        cell: (info) => (
          <span className="text-sm text-neutral-400">
            {info.getValue() ? new Date(info.getValue() as string).toLocaleDateString("es-CL") : "-"}
          </span>
        ),
      },
      {
        id: "actions",
        header: "Acciones",
        cell: ({ row }) => (
          <div className="flex gap-2">
            <button
              className="btn ghost text-sm"
              onClick={(e) => {
                e.stopPropagation();
                onCustomerEdit(row.original);
              }}
            >
              Editar
            </button>
            <button
              className="btn ghost text-sm"
              onClick={(e) => {
                e.stopPropagation();
                onCustomerDelete(row.original.id);
              }}
            >
              {row.original.active !== false ? "Desactivar" : "Activar"}
            </button>
          </div>
        ),
      },
    ],
    [customerStats, getHealthStatus, onCustomerEdit, onCustomerDelete]
  );

  const table = useReactTable({
    data: customers,
    columns,
    state: {
      sorting,
    },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <div className="overflow-x-auto">
      <table className="table w-full">
        <thead>
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <th
                  key={header.id}
                  className="cursor-pointer select-none hover:bg-neutral-800"
                  onClick={header.column.getToggleSortingHandler()}
                >
                  <div className="flex items-center gap-2">
                    {flexRender(header.column.columnDef.header, header.getContext())}
                    {{
                      asc: " 🔼",
                      desc: " 🔽",
                    }[header.column.getIsSorted() as string] ?? null}
                  </div>
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {table.getRowModel().rows.map((row) => (
            <tr
              key={row.id}
              className={`cursor-pointer hover:bg-neutral-700 ${
                selectedCustomerId === row.original.id ? "bg-neutral-700 ring-2 ring-blue-500" : ""
              }`}
              onClick={() => onCustomerSelect(row.original)}
            >
              {row.getVisibleCells().map((cell) => (
                <td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
