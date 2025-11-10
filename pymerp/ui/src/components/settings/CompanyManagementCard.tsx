import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  listCompaniesWithDetails,
  deleteCompanyWithDetails,
  type CompanyResponse,
} from '../../services/client'
import CompanyManagementDialog from '../dialogs/CompanyManagementDialog'

export default function CompanyManagementCard() {
  const queryClient = useQueryClient()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingCompany, setEditingCompany] = useState<CompanyResponse | null>(null)

  const { data: companies = [], isLoading } = useQuery({
    queryKey: ['companies'],
    queryFn: listCompaniesWithDetails,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCompanyWithDetails(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'], exact: false })
    },
  })

  const handleCreate = () => {
    setEditingCompany(null)
    setDialogOpen(true)
  }

  const handleEdit = (company: CompanyResponse) => {
    setEditingCompany(company)
    setDialogOpen(true)
  }

  const handleDelete = (company: CompanyResponse) => {
    if (
      confirm(
        `¿Estás seguro de eliminar la empresa "${company.fantasyName || company.businessName}"?\n\nEsto también eliminará todas sus ubicaciones padre.`
      )
    ) {
      deleteMutation.mutate(company.id)
    }
  }

  return (
    <>
      <div className="bg-white p-6 rounded-lg shadow">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">Gestión de Empresas</h2>
          <button
            onClick={handleCreate}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            + Nueva Empresa
          </button>
        </div>

        {isLoading ? (
          <p className="text-gray-500">Cargando empresas...</p>
        ) : companies.length === 0 ? (
          <p className="text-gray-500">No hay empresas registradas.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-2">Nombre/Razón Social</th>
                  <th className="text-left p-2">RUT</th>
                  <th className="text-left p-2">Ubicaciones Padre</th>
                  <th className="text-right p-2">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {companies.map(company => (
                  <tr key={company.id} className="border-b hover:bg-gray-50">
                    <td className="p-2">
                      <div>
                        <div className="font-medium">
                          {company.fantasyName || company.businessName}
                        </div>
                        {company.fantasyName && (
                          <div className="text-xs text-gray-500">{company.businessName}</div>
                        )}
                      </div>
                    </td>
                    <td className="p-2">{company.rut}</td>
                    <td className="p-2">
                      {company.parentLocations && company.parentLocations.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {company.parentLocations.map(loc => (
                            <span
                              key={loc.id}
                              className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs"
                            >
                              {loc.code} - {loc.name}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-gray-400">Sin ubicaciones</span>
                      )}
                    </td>
                    <td className="p-2 text-right">
                      <button
                        onClick={() => handleEdit(company)}
                        className="px-3 py-1 text-blue-600 hover:bg-blue-50 rounded mr-2"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() => handleDelete(company)}
                        className="px-3 py-1 text-red-600 hover:bg-red-50 rounded"
                        disabled={deleteMutation.isPending}
                      >
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <CompanyManagementDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        editingCompany={editingCompany}
      />
    </>
  )
}
