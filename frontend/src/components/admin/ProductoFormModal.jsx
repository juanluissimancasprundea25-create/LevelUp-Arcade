import { useEffect, useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { Sparkles, Tag, Loader2 } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useToast } from './Toast.jsx';
import { useAuth } from '../../lib/auth.jsx';

/**
 * Modal de creación / edición de producto.
 *
 *  - Si `producto` viene null → modo CREAR (POST /productos)
 *  - Si `producto` trae un id → modo EDITAR (PUT /productos/{id})
 *
 * Incluye botones de IA (solo ADMIN):
 *  - Generar descripción → POST /llm/descripcion-producto
 *  - Sugerir categoría   → POST /llm/sugerir-categoria
 *
 * Tras guardar, llama a onSaved(producto) y cierra.
 */

const empty = {
  sku: '',
  nombre: '',
  descripcion: '',
  precio: '',
  stock: 0,
  stockMinimo: 0,
  categoriaId: '',
  proveedorId: '',
  imagenUrl: '',
  activo: true,
};

export function ProductoFormModal({ open, onClose, producto, onSaved }) {
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [iaBusy, setIaBusy] = useState(null); // 'descripcion' | 'categoria' | null
  const toast = useToast();
  const { user } = useAuth();
  const esEdicion = !!producto?.id;
  const puedeIA = user?.rol === 'ADMIN';

  const categorias  = useFetch('/categorias');
  const proveedores = useFetch('/proveedores');

  // Cargar producto en edición o reset en creación
  useEffect(() => {
    if (!open) return;
    if (producto) {
      setForm({
        sku:         producto.sku || '',
        nombre:      producto.nombre || '',
        descripcion: producto.descripcion || '',
        precio:      producto.precio ?? '',
        stock:       producto.stock ?? 0,
        stockMinimo: producto.stockMinimo ?? 0,
        categoriaId: producto.categoriaId ?? '',
        proveedorId: producto.proveedorId ?? '',
        imagenUrl:   producto.imagenUrl || '',
        activo:      producto.activo ?? true,
      });
    } else {
      setForm(empty);
    }
    setErrors({});
  }, [open, producto]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  function validar() {
    const e = {};
    if (!/^[A-Z0-9-]{3,50}$/.test(form.sku.trim())) {
      e.sku = 'SKU: solo MAYÚSCULAS, dígitos y guiones (3-50)';
    }
    if (!form.nombre.trim()) e.nombre = 'El nombre es obligatorio';
    if (form.precio === '' || Number(form.precio) < 0) e.precio = 'Precio inválido';
    if (form.stock === '' || Number(form.stock) < 0) e.stock = 'Stock inválido';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(ev) {
    ev.preventDefault();
    if (busy) return;
    if (!validar()) return;

    setBusy(true);
    const payload = {
      sku:         form.sku.trim().toUpperCase(),
      nombre:      form.nombre.trim(),
      descripcion: form.descripcion.trim() || null,
      precio:      Number(form.precio),
      stock:       Number(form.stock),
      stockMinimo: Number(form.stockMinimo) || 0,
      categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
      proveedorId: form.proveedorId ? Number(form.proveedorId) : null,
      imagenUrl:   form.imagenUrl.trim() || null,
    };
    if (esEdicion) payload.activo = !!form.activo;

    try {
      const { data } = esEdicion
        ? await api.put(`/productos/${producto.id}`, payload)
        : await api.post('/productos', payload);
      toast.ok(esEdicion ? 'Producto actualizado' : 'Producto creado');
      onSaved && onSaved(data);
      onClose();
    } catch (err) {
      const msg = err.response?.data?.mensaje
                || err.response?.data?.message
                || 'No se pudo guardar';
      toast.err(msg);
    } finally {
      setBusy(false);
    }
  }

  // === IA ===
  async function generarDescripcion() {
    if (!form.nombre.trim()) {
      toast.warn('Pon primero el nombre del producto');
      return;
    }
    setIaBusy('descripcion');
    try {
      const categoriaNombre = categorias.data?.find(c => c.id === Number(form.categoriaId))?.nombre || null;
      const { data } = await api.post('/llm/descripcion-producto', {
        nombreProducto: form.nombre.trim(),
        categoria: categoriaNombre,
      });
      set('descripcion', data.resultado);
      toast.ok(`Descripción generada (${data.modelo})`);
    } catch (e) {
      const msg = e.response?.data?.message
               || e.response?.data?.mensaje
               || e.response?.data?.error
               || 'La IA no respondió';
      toast.err(msg);
    } finally {
      setIaBusy(null);
    }
  }

  async function sugerirCategoria() {
    if (!form.nombre.trim()) {
      toast.warn('Pon primero el nombre del producto');
      return;
    }
    setIaBusy('categoria');
    try {
      const { data } = await api.post('/llm/sugerir-categoria', {
        nombreProducto: form.nombre.trim(),
        descripcion: form.descripcion.trim() || null,
      });
      const sugerida = (categorias.data || []).find(
        c => c.nombre.toLowerCase() === (data.resultado || '').trim().toLowerCase()
      );
      if (sugerida) {
        set('categoriaId', sugerida.id);
        toast.ok(`Categoría sugerida: ${sugerida.nombre}`);
      } else {
        toast.info(`Sugerencia: "${data.resultado}" (no existe, créala antes)`);
      }
    } catch (e) {
      const msg = e.response?.data?.message
               || e.response?.data?.mensaje
               || e.response?.data?.error
               || 'La IA no respondió';
      toast.err(msg);
    } finally {
      setIaBusy(null);
    }
  }

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title={esEdicion ? `Editar · ${producto?.nombre}` : 'Nuevo producto'}
      subtitle={esEdicion ? 'EDITAR' : 'ALTA'}
      size="lg"
    >
      <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* SKU */}
        <label className="block">
          <span className="hud-label">SKU *</span>
          <input
            type="text"
            value={form.sku}
            onChange={(e) => set('sku', e.target.value.toUpperCase())}
            placeholder="JUEGO-001"
            className="hud-input mt-1.5"
            disabled={busy}
          />
          {errors.sku && <p className="text-xs text-cockpit-danger mt-1">{errors.sku}</p>}
        </label>

        {/* Nombre */}
        <label className="block">
          <span className="hud-label">Nombre *</span>
          <input
            type="text"
            value={form.nombre}
            onChange={(e) => set('nombre', e.target.value)}
            placeholder="Super Arcade Adventure"
            className="hud-input mt-1.5"
            disabled={busy}
            maxLength={200}
          />
          {errors.nombre && <p className="text-xs text-cockpit-danger mt-1">{errors.nombre}</p>}
        </label>

        {/* Descripción + IA (ocupa 2 cols) */}
        <label className="block md:col-span-2">
          <div className="flex items-center justify-between">
            <span className="hud-label">Descripción</span>
            {puedeIA && (
              <button
                type="button"
                onClick={generarDescripcion}
                disabled={iaBusy === 'descripcion' || busy}
                className="hud-btn hud-btn--cyan !px-3 !py-1.5 !text-[10px]"
                title="Generar con IA"
              >
                {iaBusy === 'descripcion'
                  ? <Loader2 size={12} className="animate-spin" />
                  : <Sparkles size={12} />}
                IA · Generar
              </button>
            )}
          </div>
          <textarea
            value={form.descripcion}
            onChange={(e) => set('descripcion', e.target.value)}
            placeholder="Describe el producto…"
            rows={4}
            className="hud-input mt-1.5 resize-none font-sans"
            disabled={busy}
            maxLength={5000}
          />
        </label>

        {/* Precio */}
        <label className="block">
          <span className="hud-label">Precio (€) *</span>
          <input
            type="number" step="0.01" min="0"
            value={form.precio}
            onChange={(e) => set('precio', e.target.value)}
            placeholder="29.95"
            className="hud-input mt-1.5"
            disabled={busy}
          />
          {errors.precio && <p className="text-xs text-cockpit-danger mt-1">{errors.precio}</p>}
        </label>

        {/* Estado activo (solo en edición) */}
        {esEdicion ? (
          <label className="block">
            <span className="hud-label">Estado</span>
            <div className="mt-1.5 flex gap-2">
              <button
                type="button"
                onClick={() => set('activo', true)}
                className={`hud-btn !px-4 !py-2 flex-1 ${form.activo ? '' : 'opacity-40'}`}
              >
                Activo
              </button>
              <button
                type="button"
                onClick={() => set('activo', false)}
                className={`hud-btn hud-btn--cyan !px-4 !py-2 flex-1 ${!form.activo ? '' : 'opacity-40'}`}
                style={!form.activo ? {
                  color: '#fbbf24',
                  borderColor: 'rgba(251,191,36,0.6)',
                } : {}}
              >
                Inactivo
              </button>
            </div>
          </label>
        ) : (
          <div /> /* placeholder columna */
        )}

        {/* Stock */}
        <label className="block">
          <span className="hud-label">Stock *</span>
          <input
            type="number" min="0"
            value={form.stock}
            onChange={(e) => set('stock', e.target.value)}
            className="hud-input mt-1.5"
            disabled={busy}
          />
          {errors.stock && <p className="text-xs text-cockpit-danger mt-1">{errors.stock}</p>}
        </label>

        {/* Stock mínimo */}
        <label className="block">
          <span className="hud-label">Stock mínimo</span>
          <input
            type="number" min="0"
            value={form.stockMinimo}
            onChange={(e) => set('stockMinimo', e.target.value)}
            className="hud-input mt-1.5"
            disabled={busy}
          />
        </label>

        {/* Categoría + IA sugerir */}
        <label className="block">
          <div className="flex items-center justify-between">
            <span className="hud-label">Categoría</span>
            {puedeIA && (
              <button
                type="button"
                onClick={sugerirCategoria}
                disabled={iaBusy === 'categoria' || busy || categorias.loading}
                className="hud-btn hud-btn--cyan !px-3 !py-1.5 !text-[10px]"
                title="Sugerir con IA"
              >
                {iaBusy === 'categoria'
                  ? <Loader2 size={12} className="animate-spin" />
                  : <Tag size={12} />}
                IA · Sugerir
              </button>
            )}
          </div>
          <select
            value={form.categoriaId}
            onChange={(e) => set('categoriaId', e.target.value)}
            className="hud-input mt-1.5"
            disabled={busy || categorias.loading}
          >
            <option value="">— sin categoría —</option>
            {(categorias.data || []).map((c) => (
              <option key={c.id} value={c.id}>{c.nombre}</option>
            ))}
          </select>
        </label>

        {/* Proveedor */}
        <label className="block">
          <span className="hud-label">Proveedor</span>
          <select
            value={form.proveedorId}
            onChange={(e) => set('proveedorId', e.target.value)}
            className="hud-input mt-1.5"
            disabled={busy || proveedores.loading}
          >
            <option value="">— sin proveedor —</option>
            {(proveedores.data || []).map((p) => (
              <option key={p.id} value={p.id}>{p.nombreEmpresa || p.nombre}</option>
            ))}
          </select>
        </label>

        {/* URL Imagen (ocupa 2 cols) */}
        <label className="block md:col-span-2">
          <span className="hud-label">URL de imagen</span>
          <input
            type="url"
            value={form.imagenUrl}
            onChange={(e) => set('imagenUrl', e.target.value)}
            placeholder="https://… o /img/algo.jpg"
            className="hud-input mt-1.5"
            disabled={busy}
            maxLength={500}
          />
          {form.imagenUrl && (
            <div className="mt-2">
              <img
                src={form.imagenUrl}
                alt="vista previa"
                className="h-24 w-24 object-cover border border-cockpit-line"
                onError={(e) => { e.currentTarget.style.display = 'none'; }}
                onLoad={(e) => { e.currentTarget.style.display = 'block'; }}
              />
            </div>
          )}
        </label>

        {/* Botones */}
        <div className="md:col-span-2 flex gap-3 justify-end mt-2 pt-4 border-t border-cockpit-line">
          <button type="button" onClick={onClose} className="hud-btn hud-btn--cyan !px-4 !py-2" disabled={busy}>
            Cancelar
          </button>
          <button type="submit" className="hud-btn !px-5 !py-2" disabled={busy}>
            {busy ? 'Guardando…' : (esEdicion ? 'Guardar cambios' : 'Crear producto')}
          </button>
        </div>
      </form>
    </HudModal>
  );
}
