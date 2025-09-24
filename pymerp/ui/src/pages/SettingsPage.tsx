import PageHeader from "../components/layout/PageHeader";

export default function SettingsPage() {
  return (
    <div className="page-section">
      <PageHeader
        title="Configuración"
        description="Administra datos de la empresa, usuarios y preferencias."
      />

      <div className="card settings-grid">
        <section>
          <h3>Compañía</h3>
          <p className="muted small">Razón social, dirección, horarios, imagen de marca.</p>
          <button className="btn ghost">Editar</button>
        </section>
        <section>
          <h3>Usuarios y roles</h3>
          <p className="muted small">Invitaciones, permisos por módulo, bitácora de accesos.</p>
          <button className="btn ghost">Gestionar</button>
        </section>
        <section>
          <h3>Integraciones</h3>
          <p className="muted small">Facturación, correo, almacenamiento, pasarelas de pago.</p>
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
