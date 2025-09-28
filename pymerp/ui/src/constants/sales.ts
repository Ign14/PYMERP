export const SALE_DOCUMENT_TYPES = [
  { value: "Factura", label: "Factura" },
  { value: "Boleta", label: "Boleta" },
  { value: "Cotización", label: "Cotización" },
  { value: "Comprobante", label: "Comprobante" },
] as const;

export const SALE_PAYMENT_METHODS = [
  { value: "Efectivo", label: "Efectivo" },
  { value: "Tarjetas", label: "Tarjetas" },
  { value: "Transferencia", label: "Transferencia" },
  { value: "Otros", label: "Otros" },
] as const;
