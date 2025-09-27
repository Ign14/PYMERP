import { useEffect, useState } from "react";
import { fetchProductQrBlob, Product } from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  product: Product | null;
  onClose: () => void;
};

export default function ProductQrModal({ open, product, onClose }: Props) {
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cleanup = () => {
    setLoading(false);
    setError(null);
    if (imageUrl && imageUrl.startsWith("blob:")) {
      URL.revokeObjectURL(imageUrl);
    }
    setImageUrl(null);
  };

  useEffect(() => {
    if (!open || !product) {
      cleanup();
      return;
    }
    setLoading(true);
    setError(null);
    fetchProductQrBlob(product.id)
      .then((blob) => {
        const objectUrl = URL.createObjectURL(blob);
        setImageUrl(objectUrl);
      })
      .catch(() => {
        setError("No se pudo cargar el código QR");
      })
      .finally(() => {
        setLoading(false);
      });
    return cleanup;
  }, [open, product?.id]);

  const handlePrint = () => {
    if (!imageUrl || !product) return;
    const printWindow = window.open("", "_blank", "noopener,noreferrer");
    if (!printWindow) return;
    const title = product.sku ? `QR ${product.sku}` : "QR";
    printWindow.document.write(`<!doctype html><html><head><title>${title}</title></head>`);
    printWindow.document.write(
      '<body style="margin:0;display:flex;align-items:center;justify-content:center;height:100vh;background:#fff;">',
    );
    printWindow.document.write(`<img src="${imageUrl}" alt="${title}" style="max-width:90%;max-height:90%;" />`);
    printWindow.document.write("</body></html>");
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();
  };

  const handleDownload = async () => {
    if (!product) return;
    try {
      const blob = await fetchProductQrBlob(product.id, { download: true });
      const objectUrl = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      const extension = blob.type === "image/svg+xml" ? "svg" : "png";
      const baseName = (product.sku || product.id).replace(/[^a-zA-Z0-9-_]+/g, "-");
      anchor.href = objectUrl;
      anchor.download = `${baseName}-qr.${extension}`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(objectUrl), 500);
    } catch (downloadError) {
      setError("No se pudo descargar el código QR");
    }
  };

  const title = product ? `Código QR ${product.sku ?? ""}`.trim() || "Código QR" : "Código QR";

  return (
    <Modal open={open} title={title} onClose={onClose}>
      {loading && <p className="muted">Cargando código QR...</p>}
      {error && <p className="error">{error}</p>}
      {!loading && !error && imageUrl && (
        <div className="qr-preview">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={imageUrl} alt={title} />
        </div>
      )}
      <div className="buttons">
        <button className="btn" type="button" onClick={handlePrint} disabled={!imageUrl || loading}>
          Imprimir
        </button>
        <button className="btn ghost" type="button" onClick={handleDownload} disabled={!product || loading}>
          Descargar
        </button>
        <button className="btn ghost" type="button" onClick={onClose}>
          Cerrar
        </button>
      </div>
    </Modal>
  );
}
