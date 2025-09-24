import PageHeader from "../components/layout/PageHeader";

export default function SettingsPage() {
  return (
    <div className="page-section">
      <PageHeader
        title="Configuraci�n"
        description="Administra datos de la empresa, usuarios y preferencias."
      />

      <div className="card settings-grid">
        <section>
          <h3>Compa��a</h3>
          <p className="muted small">Raz�n social, direcci�n, horarios, imagen de marca.</p>
          <button className="btn ghost">Editar</button>
        </section>
        <section>
          <h3>Usuarios y roles</h3>
          <p className="muted small">Invitaciones, permisos por m�dulo, bit�cora de accesos.</p>
          <button className="btn ghost">Gestionar</button>
        </section>
        <section>
          <h3>Integraciones</h3>
          <p className="muted small">Facturaci�n, correo, almacenamiento, pasarelas de pago.</p>
          <button className="btn ghost">Configurar</button>
        </section>
        <section>
          <h3>Preferencias</h3>
          <p className="muted small">Tema, idioma, formatos, notificaciones.</p>
          <button className="btn ghost">Personalizar</button>
        </section>
      </div>
    </div>
  );
}
