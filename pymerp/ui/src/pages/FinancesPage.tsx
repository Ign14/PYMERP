import PageHeader from '../components/layout/PageHeader'
import FinanceSummaryCards from '../components/finances/FinanceSummaryCards'
import AccountsReceivablePanel from '../components/finances/AccountsReceivablePanel'
import AccountsPayablePanel from '../components/finances/AccountsPayablePanel'
import CashflowChart from '../components/finances/CashflowChart'

export default function FinancesPage() {
  return (
    <div className="p-6 space-y-6 bg-neutral-950">
      <PageHeader
        title="Finanzas"
        description="Gestión integral de cuentas por cobrar, cuentas por pagar y proyecciones de flujo de caja."
        actions={null}
      />

      <FinanceSummaryCards />

      <div className="grid grid-cols-1 gap-6">
        <CashflowChart />
        <AccountsReceivablePanel />
        <AccountsPayablePanel />
      </div>
    </div>
  )
}
