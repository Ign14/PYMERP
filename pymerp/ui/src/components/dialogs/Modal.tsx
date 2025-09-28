import { ReactNode, RefObject, useEffect, useId, useRef } from "react";

interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  initialFocusRef?: RefObject<HTMLElement>;
  className?: string;
}

export default function Modal({
  open,
  title,
  onClose,
  children,
  initialFocusRef,
  className,
}: ModalProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const headingId = useId();

  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== "Tab") {
        return;
      }
      const focusable = getFocusableElements(containerRef.current);
      if (focusable.length === 0) {
        event.preventDefault();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement as HTMLElement | null;

      if (event.shiftKey) {
        if (active === first || !containerRef.current?.contains(active)) {
          event.preventDefault();
          last.focus();
        }
      } else if (active === last) {
        event.preventDefault();
        first.focus();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  useEffect(() => {
    if (!open) return;
    const focusTarget =
      initialFocusRef?.current ??
      containerRef.current?.querySelector<HTMLElement>("[data-autofocus]") ??
      getFocusableElements(containerRef.current)[0];
    if (focusTarget) {
      requestAnimationFrame(() => focusTarget.focus());
    }
  }, [open, initialFocusRef]);

  if (!open) return null;

  return (
    <div className="modal-backdrop" role="presentation">
      <div
        className={className ? `modal ${className}` : "modal"}
        role="dialog"
        aria-modal="true"
        aria-labelledby={headingId}
        ref={containerRef}
      >
        <header className="modal-header">
          <h2 id={headingId}>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Cerrar">
            ×
          </button>
        </header>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}

function getFocusableElements(container: HTMLElement | null): HTMLElement[] {
  if (!container) return [];
  const selectors = [
    "button",
    "[href]",
    "input",
    "select",
    "textarea",
    "[tabindex]:not([tabindex='-1'])",
  ];
  return Array.from(container.querySelectorAll<HTMLElement>(selectors.join(","))).filter(
    (element) => !element.hasAttribute("disabled") && !element.getAttribute("aria-hidden"),
  );
}
