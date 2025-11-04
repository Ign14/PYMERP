import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

const LOGIN_URL = 'http://localhost:5173/login'

export default function SessionAccessCard() {
  const navigate = useNavigate()
  const { session, logout } = useAuth()
  const [copyMessage, setCopyMessage] = useState<string | null>(null)

  const modules = session?.modules ?? []

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(LOGIN_URL)
      setCopyMessage('Enlace copiado al portapapeles.')
    } catch {
      setCopyMessage('No se pudo copiar el enlace. Copialo manualmente.')
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleGotoLogin = () => {
    navigate('/login')
  }

  return (
    <div className="card">
      <div className="card-header">
        <h2>Acceso a la cuenta</h2>
      </div>
      <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {session ? (
          <>
            <p>
              Sesion iniciada como <strong>{session.email}</strong>
              {session.name ? ` (${session.name})` : ''}.
            </p>
            <p className="muted small">Compania activa: {session.companyId}</p>
            {modules.length > 0 ? (
              <div>
                <h3 className="small">Modulos habilitados</h3>
                <p className="muted small">{modules.join(', ')}</p>
              </div>
            ) : null}
          </>
        ) : (
          <p>No hay una sesion activa en este momento.</p>
        )}

        <section style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <h3>Enlace para nuevos usuarios</h3>
          <p className="muted small">
            Comparte este enlace con los usuarios que crees desde esta pagina para que inicien
            sesion con la contrasena entregada: <span className="mono">{LOGIN_URL}</span>
          </p>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <button className="btn ghost" type="button" onClick={handleCopyLink}>
              Copiar enlace de acceso
            </button>
            <button className="btn ghost" type="button" onClick={handleGotoLogin}>
              Abrir pantalla de login
            </button>
          </div>
          {copyMessage ? (
            <p className="muted small" aria-live="polite">
              {copyMessage}
            </p>
          ) : null}
        </section>

        <section style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <h3>Sesion actual</h3>
          <p className="muted small">
            Cierra la sesion para probar nuevas credenciales o cambia de usuario sin abandonar la
            seccion de configuracion.
          </p>
          <button className="btn danger" type="button" onClick={handleLogout}>
            Cerrar sesion
          </button>
        </section>
      </div>
    </div>
  )
}
