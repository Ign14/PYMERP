import { useMemo, useRef } from "react";
import { Product } from "../../services/client";
import Modal from "./Modal";

type Props = {
  open: boolean;
  product: Product | null;
  imageUrl: string | null;
  onClose: () => void;
};

export default function ProductImageModal({ open, product, imageUrl, onClose }: Props) {
  const closeButtonRef = useRef<HTMLButtonElement | null>(null);
  const title = useMemo(() => {
    if (!product) {
      return "Imagen del producto";
    }
    return `Imagen - ${product.name}`;
  }, [product]);

  const source = imageUrl ?? "";

  return (
    <Modal open={open} title={title} onClose={onClose} initialFocusRef={closeButtonRef}>
      <div className="image-modal__content">
        {source ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={source} alt={title} className="image-modal__preview" />
        ) : (
          <p className="muted">Sin imagen disponible</p>
        )}
      </div>
      <div className="buttons">
        <button ref={closeButtonRef} className="btn" type="button" onClick={onClose}>
          Cerrar
        </button>
      </div>
    </Modal>
  );
}
