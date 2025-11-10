import PageHeader from '../components/layout/PageHeader'
import AccountOverviewCard from '../components/settings/AccountOverviewCard'
import SessionAccessCard from '../components/settings/SessionAccessCard'
import UserManagementCard from '../components/settings/UserManagementCard'
import CompanyManagementCard from '../components/settings/CompanyManagementCard'
import { useAuth } from '../context/AuthContext'

export default function SettingsPage() {
  const { session } = useAuth()

  if (!session) {
    return (
      <div className="page-section">
        <PageHeader title="Configuracion" description="Inicia sesion para administrar la cuenta." />
        <p>No hay una sesion activa actualmente.</p>
      </div>
    )
  }

  const layoutStyle = {
    display: 'grid',
    gap: '24px',
    gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
    alignItems: 'stretch',
  } as const

  return (
    <div className="page-section">
      <PageHeader
        title="Configuracion"
        description="Administra los datos de la empresa, los usuarios y el acceso a PYMERP."
      />
      <div style={layoutStyle}>
        <AccountOverviewCard companyId={session.companyId} />
        <SessionAccessCard />
        <UserManagementCard />
      </div>
      <div className="mt-6">
        <CompanyManagementCard />
      </div>
    </div>
  )
}
