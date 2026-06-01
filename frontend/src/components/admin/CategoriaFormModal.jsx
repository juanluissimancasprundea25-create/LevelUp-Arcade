import { useEffect, useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { api } from '../../lib/api.js';
import { useToast } from './Toast.jsx';

/**
 * Modal de creación / edición de categoría.
 *   - Sin `categoria` → CREAR (POST /categorias)
 *   - Con `categoria` → EDITAR (PUT /categorias/{id})
 */
const empty = { nombre: '', descripcion: '', activa: true };

export function CategoriaFormModal({ open, onClose, categoria, onSaved }) {
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const esEdicion = !!categoria?.id;

  useEffect(() => {
    if (!open) return;
    if (categoria) {
      setForm({
        nombre: categoria.nombre || '',
        descripcion: categoria.descripcion || '',
        activa: categoria.activa ?? true,
      });
    } else {
      setForm(empty);
    }
    setErrors({});
  }, [open, categoria]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  function validar() {
    const e = {};
    if (!form.nombre.trim()) e.nombre = 'El nombre es obligatorio';
    if (form.nombre.length > 100) e.nombre = 'Máximo 100 caracteres';
    if (form.descripcion.length > 2000) e.descripcion = 'Máximo 2000 caracteres';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(ev) {
    ev.preventDefault();
    if (busy || !validar()) return;
    setBusy(true);
    const payload = {
      nombre: form.nombre.trim(),
      descripcion: form.descripcion.trim() || null,
    };
    if (esEdicion) payload.activa = !!form.activa;

    try {
      const { data } = esEdicion
        ? await api.put(`/categorias/${categoria.id}`, payload)
        : await api.post('/categorias', payload);
      toast.ok(esEdicion ? 'Categoría actualizada' : 'Categoría creada');
      onSaved && onSaved(data);
      onClose();
    } catch (err) {
      toast.err(err.response?.data?.mensaje || err.response?.data?.message || 'No se pudo guardar');
    } finally {
      setBusy(false);
    }
  }

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title={esEdicion ? `Editar · ${categoria?.nombre}` : 'Nueva categoría'}
      subtitle={esEdicion ? 'EDITAR' : 'ALTA'}
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block">
          <span className="hud-label">Nombre *</span>
          <input
            type="text"
            value={form.nombre}
            onChange={(e) => set('nombre', e.target.value)}
            placeholder="Aventura, RPG, Indie…"
            className="hud-input mt-1.5"
            disabled={busy}
            maxLength={100}
            autoFocus
          />
          {errors.nombre && <p className="text-xs text-cockpit-danger mt-1">{errors.nombre}</p>}
        </label>

        <label className="block">
          <span className="hud-label">Descripción</span>
          <textarea
            value={form.descripcion}
            onChange={(e) => set('descripcion', e.target.value)}
            placeholder="Opcional. Sirve para que la IA entienda mejor la categoría."
            rows={4}
            className="hud-input mt-1.5 resize-none font-sans"
            disabled={busy}
            maxLength={2000}
          />
          {errors.descripcion && <p className="text-xs text-cockpit-danger mt-1">{errors.descripcion}</p>}
        </label>

        {esEdicion && (
          <label className="block">
            <span className="hud-label">Estado</span>
            <div className="mt-1.5 flex gap-2">
              <button
                type="button"
                onClick={() => set('activa', true)}
                className={`hud-btn !px-4 !py-2 flex-1 ${form.activa ? '' : 'opacity-40'}`}
              >
                Activa
              </button>
              <button
                type="button"
                onClick={() => set('activa', false)}
                className={`hud-btn hud-btn--cyan !px-4 !py-2 flex-1 ${!form.activa ? '' : 'opacity-40'}`}
              >
                Inactiva
              </button>
            </div>
          </label>
        )}

        <div className="flex gap-3 justify-end pt-3 border-t border-cockpit-line">
          <button type="button" onClick={onClose} className="hud-btn hud-btn--cyan !px-4 !py-2" disabled={busy}>
            Cancelar
          </button>
          <button type="submit" className="hud-btn !px-5 !py-2" disabled={busy}>
            {busy ? 'Guardando…' : (esEdicion ? 'Guardar' : 'Crear')}
          </button>
        </div>
      </form>
    </HudModal>
  );
}
