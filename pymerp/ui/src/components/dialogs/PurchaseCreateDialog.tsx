import PurchaseCreateForm from '../purchases/PurchaseCreateForm'
import Modal from './Modal'

interface Props {
  open: boolean
  onClose: () => void
  onCreated: () => void
}

export default function PurchaseCreateDialog({ open, onClose, onCreated }: Props) {
  const handleSubmitted = (_purchaseId: string) => {
    onCreated()
    onClose()
  }

  return (
    <Modal open={open} onClose={onClose} title="Registrar Compra">
      <PurchaseCreateForm onSubmitted={handleSubmitted} />
    </Modal>
  )
}
