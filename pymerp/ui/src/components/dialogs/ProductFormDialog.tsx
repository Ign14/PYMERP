import { ChangeEvent, FormEvent, useEffect, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import axios from 'axios'
import { Product, ProductFormData, createProduct, updateProduct } from '../../services/client'
import Modal from './Modal'

type Props = {
  open: boolean
  product?: Product | null
  onClose: () => void
  onSaved?: (product: Product) => void
}

type FormState = {
  sku: string
  name: string
  description: string
  category: string
  barcode: string
  imageUrl: string | null
  imageFile: File | null
}

const EMPTY_FORM: FormState = {
  sku: '',
  name: '',
  description: '',
  category: '',
  barcode: '',
  imageUrl: null,
  imageFile: null,
}

export default function ProductFormDialog({ open, product, onClose, onSaved }: Props) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [imageError, setImageError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const previewUrlRef = useRef<string | null>(null)

  useEffect(
    () => () => {
      if (previewUrlRef.current && previewUrlRef.current.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrlRef.current)
      }
    },
    []
  )

  useEffect(() => {
    if (!open) {
      setForm(EMPTY_FORM)
      setImageError(null)
      resetPreview(null)
      return
    }
    if (product) {
      setForm({
        sku: product.sku ?? '',
        name: product.name ?? '',
        description: product.description ?? '',
        category: product.category ?? '',
        barcode: product.barcode ?? '',
        imageUrl: product.imageUrl ?? null,
        imageFile: null,
      })
      setImageError(null)
      resetPreview(product.imageUrl ?? null)
    } else {
      setForm(EMPTY_FORM)
      setImageError(null)
      resetPreview(null)
    }
  }, [open, product])

  const mutation = useMutation({
    mutationFn: (payload: ProductFormData) => {
      if (product) {
        return updateProduct(product.id, payload)
      }
      return createProduct(payload)
    },
    onSuccess: saved => {
      onSaved?.(saved)
      onClose()
    },
  })

  useEffect(() => {
    if (!open) {
      mutation.reset()
    }
  }, [open, mutation])

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    if (file.size > 512 * 1024) {
      setImageError('La imagen debe pesar 500 KB o menos')
      event.target.value = ''
      return
    }
    setImageError(null)
    setForm(prev => ({ ...prev, imageFile: file }))
    const url = URL.createObjectURL(file)
    updatePreview(url)
  }

  const removeImage = () => {
    setImageError(null)
    if (form.imageFile) {
      const previousUrl = form.imageUrl ?? null
      setForm(prev => ({ ...prev, imageFile: null }))
      updatePreview(previousUrl)
    } else {
      setForm(prev => ({ ...prev, imageUrl: null }))
      updatePreview(null)
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const resetPreview = (value: string | null) => {
    updatePreview(value)
  }

  const updatePreview = (value: string | null) => {
    if (previewUrlRef.current && previewUrlRef.current.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrlRef.current)
    }
    previewUrlRef.current = value
    setImagePreview(value)
  }

  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    const sku = form.sku.trim()
    const name = form.name.trim()
    if (!sku) {
      window.alert('El SKU es obligatorio')
      return
    }
    if (!name) {
      window.alert('El nombre es obligatorio')
      return
    }
    const payload: ProductFormData = {
      sku,
      name,
      description: form.description.trim() ? form.description.trim() : undefined,
      category: form.category.trim() ? form.category.trim() : undefined,
      barcode: form.barcode.trim() ? form.barcode.trim() : undefined,
    }
    if (form.imageFile) {
      payload.imageFile = form.imageFile
      payload.imageUrl = null
    } else if (form.imageUrl === null) {
      payload.imageUrl = null
    } else if (typeof form.imageUrl === 'string' && form.imageUrl.trim().length > 0) {
      payload.imageUrl = form.imageUrl.trim()
    }
    mutation.mutate(payload)
  }

  const title = product ? 'Editar producto' : 'Nuevo producto'
  const hasImage = Boolean(imagePreview)

  return (
    <Modal open={open} title={title} onClose={() => !mutation.isPending && onClose()}>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          <span>SKU *</span>
          <input
            className="input"
            value={form.sku}
            onChange={e => setForm(prev => ({ ...prev, sku: e.target.value }))}
            placeholder="SKU-001"
            disabled={mutation.isPending}
            data-autofocus
          />
        </label>
        <label>
          <span>Nombre *</span>
          <input
            className="input"
            value={form.name}
            onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
            placeholder="Producto demo"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Categoria</span>
          <input
            className="input"
            value={form.category}
            onChange={e => setForm(prev => ({ ...prev, category: e.target.value }))}
            placeholder="Bebidas"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Codigo de barras</span>
          <input
            className="input"
            value={form.barcode}
            onChange={e => setForm(prev => ({ ...prev, barcode: e.target.value }))}
            placeholder="7890000000"
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Descripcion</span>
          <textarea
            className="input"
            value={form.description}
            onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
            placeholder="Notas internas"
            rows={3}
            disabled={mutation.isPending}
          />
        </label>
        <label>
          <span>Imagen</span>
          {hasImage ? (
            <div className="image-preview">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={imagePreview ?? undefined} alt="Vista previa del producto" />
            </div>
          ) : (
            <p className="muted small">No has seleccionado una imagen</p>
          )}
          <input
            ref={fileInputRef}
            className="input"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/*"
            onChange={handleImageChange}
            disabled={mutation.isPending}
          />
          <p className="muted small">Formatos aceptados: PNG, JPG o WebP. Tamao mximo 500 KB.</p>
          {(form.imageFile || form.imageUrl) && (
            <div className="inline-actions">
              <button
                className="btn ghost"
                type="button"
                onClick={removeImage}
                disabled={mutation.isPending}
              >
                Quitar imagen
              </button>
            </div>
          )}
          {imageError && <p className="error">{imageError}</p>}
        </label>
        <div className="buttons">
          <button className="btn" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Guardando...' : 'Guardar'}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
          >
            Cancelar
          </button>
        </div>
        {mutation.isError && <p className="error">{resolveErrorMessage(mutation.error)}</p>}
      </form>
    </Modal>
  )
}

function resolveErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const detail = error.response?.data as
      | { detail?: string; message?: string; error?: string }
      | string
      | undefined
    if (typeof detail === 'string' && detail.trim().length > 0) {
      return detail
    }
    if (detail && typeof detail === 'object') {
      if (typeof detail.detail === 'string' && detail.detail.trim().length > 0) {
        return detail.detail
      }
      if (typeof detail.message === 'string' && detail.message.trim().length > 0) {
        return detail.message
      }
      if (typeof detail.error === 'string' && detail.error.trim().length > 0) {
        return detail.error
      }
    }
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return 'No se pudo guardar'
}
