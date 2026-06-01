import { useEffect, useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { api } from '../../lib/api.js';
import { useToast } from './Toast.jsx';

/**
 * Modal de creación / edición de cliente.
 *
 *  - Sin `cliente` → CREAR (POST /clientes)
 *    El backend genera la password y devuelve { cliente, passwordTemporal }.
 *    Notifica al padre con onCreated({ cliente, passwordTemporal })
 *    para que muestre el modal especial.
 *
 *  - Con `cliente` → EDITAR (PUT /clientes/{id})
 *    NO se permite cambiar el email (input deshabilitado).
 *    Llama a onSaved(cliente).
 */
const empty = {
  email: '', nombre: '', apellidos: '',
  nif: '', telefono: '',
  direccion: '', ciudad: '', codigoPostal: '', pais: 'España',
  activo: true,
};

export function ClienteFormModal({ open, onClose, cliente, onSaved, onCreated }) {
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const esEdicion = !!cliente?.id;

  useEffect(() => {
    if (!open) return;
    if (cliente) {
      setForm({
        email:        cliente.email || '',
        nombre:       cliente.nombre || '',
        apellidos:    cliente.apellidos || '',
        nif:          cliente.nif || '',
        telefono:     cliente.telefono || '',
        direccion:    cliente.direccion || '',
        ciudad:       cliente.ciudad || '',
        codigoPostal: cliente.codigoPostal || '',
        pais:         cliente.pais || 'España',
        activo:       cliente.activo ?? true,
      });
    } else {
      setForm(empty);
    }
    setErrors({});
  }, [open, cliente]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  function validar() {
    const e = {};
    if (!esEdicion) {
      if (!form.email.trim()) e.email = 'El email es obligatorio';
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) e.email = 'Email no válido';
    }
    if (!form.nombre.trim()) e.nombre = 'El nombre es obligatorio';

    if (form.nif && !/^[0-9]{8}[A-Za-z]$|^[XYZ][0-9]{7}[A-Za-z]$/.test(form.nif.trim())) {
      e.nif = 'NIF inválido (ej: 12345678A)';
    }
    if (form.telefono && !/^[+]?[0-9 ]{6,20}$/.test(form.telefono.trim())) {
      e.telefono = 'Teléfono inválido';
    }
    if (form.codigoPostal && !/^[0-9]{4,10}$/.test(form.codigoPostal.trim())) {
      e.codigoPostal = 'Código postal: solo dígitos (4-10)';
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(ev) {
    ev.preventDefault();
    if (busy || !validar()) return;

    setBusy(true);
    // Limpiar strings vacíos → null para campos opcionales
    const clean = (s) => (s && s.trim()) || null;
    const payload = {
      nombre:       form.nombre.trim(),
      apellidos:    clean(form.apellidos),
      nif:          clean(form.nif),
      telefono:     clean(form.telefono),
      direccion:    clean(form.direccion),
      ciudad:       clean(form.ciudad),
      codigoPostal: clean(form.codigoPostal),
      pais:         clean(form.pais),
    };
    if (esEdicion) {
      payload.activo = !!form.activo;
    } else {
      payload.email = form.email.trim().toLowerCase();
    }

    try {
      if (esEdicion) {
        const { data } = await api.put(`/clientes/${cliente.id}`, payload);
        toast.ok('Cliente actualizado');
        onSaved && onSaved(data);
        onClose();
      } else {
        const { data } = await api.post('/clientes', payload);
        // data = { cliente, passwordTemporal }
        toast.ok('Cliente creado');
        onClose();
        onCreated && onCreated(data);
      }
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
      title={esEdicion ? `Editar · ${cliente?.nombreCompleto || cliente?.email}` : 'Nuevo cliente'}
      subtitle={esEdicion ? 'EDITAR' : 'ALTA'}
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Sección 1: Identidad */}
        <div>
          <p className="hud-label mb-2">// IDENTIDAD</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label className="block md:col-span-2">
              <span className="hud-label">Email *</span>
              <input
                type="email"
                value={form.email}
                onChange={(e) => set('email', e.target.value)}
                placeholder="cliente@dominio.com"
                className="hud-input mt-1.5"
                disabled={busy || esEdicion}
                title={esEdicion ? 'El email no se puede cambiar aquí' : ''}
              />
              {errors.email && <p className="text-xs text-cockpit-danger mt-1">{errors.email}</p>}
              {esEdicion && <p className="text-[10px] text-slate-500 mt-1">El email no se puede modificar.</p>}
            </label>

            <label className="block">
              <span className="hud-label">Nombre *</span>
              <input
                type="text"
                value={form.nombre}
                onChange={(e) => set('nombre', e.target.value)}
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={100}
              />
              {errors.nombre && <p className="text-xs text-cockpit-danger mt-1">{errors.nombre}</p>}
            </label>

            <label className="block">
              <span className="hud-label">Apellidos</span>
              <input
                type="text"
                value={form.apellidos}
                onChange={(e) => set('apellidos', e.target.value)}
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={150}
              />
            </label>
          </div>
        </div>

        {/* Sección 2: Contacto fiscal */}
        <div>
          <p className="hud-label mb-2">// DATOS FISCALES Y CONTACTO</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label className="block">
              <span className="hud-label">NIF</span>
              <input
                type="text"
                value={form.nif}
                onChange={(e) => set('nif', e.target.value.toUpperCase())}
                placeholder="12345678A"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={20}
              />
              {errors.nif && <p className="text-xs text-cockpit-danger mt-1">{errors.nif}</p>}
            </label>

            <label className="block">
              <span className="hud-label">Teléfono</span>
              <input
                type="text"
                value={form.telefono}
                onChange={(e) => set('telefono', e.target.value)}
                placeholder="+34 600 000 000"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={20}
              />
              {errors.telefono && <p className="text-xs text-cockpit-danger mt-1">{errors.telefono}</p>}
            </label>
          </div>
        </div>

        {/* Sección 3: Dirección */}
        <div>
          <p className="hud-label mb-2">// DIRECCIÓN</p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <label className="block md:col-span-3">
              <span className="hud-label">Calle</span>
              <input
                type="text"
                value={form.direccion}
                onChange={(e) => set('direccion', e.target.value)}
                placeholder="C/ Mayor 12, 3ºB"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={255}
              />
            </label>

            <label className="block">
              <span className="hud-label">Ciudad</span>
              <input
                type="text"
                value={form.ciudad}
                onChange={(e) => set('ciudad', e.target.value)}
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={100}
              />
            </label>

            <label className="block">
              <span className="hud-label">Código postal</span>
              <input
                type="text"
                value={form.codigoPostal}
                onChange={(e) => set('codigoPostal', e.target.value)}
                placeholder="28013"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={10}
              />
              {errors.codigoPostal && <p className="text-xs text-cockpit-danger mt-1">{errors.codigoPostal}</p>}
            </label>

            <label className="block">
              <span className="hud-label">País</span>
              <input
                type="text"
                value={form.pais}
                onChange={(e) => set('pais', e.target.value)}
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={100}
              />
            </label>
          </div>
        </div>

        {/* Estado (solo edición) */}
        {esEdicion && (
          <div>
            <p className="hud-label mb-2">// ESTADO</p>
            <div className="flex gap-2">
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
              >
                Inactivo
              </button>
            </div>
          </div>
        )}

        {/* Aviso de password automática */}
        {!esEdicion && (
          <div className="text-xs text-cockpit-cyan/80 px-3 py-2 border border-cockpit-cyan/30 bg-cockpit-cyan/5">
            ℹ El sistema generará una contraseña temporal automáticamente.
            Tras crear el cliente, te la mostrará una sola vez para que se la comuniques.
          </div>
        )}

        <div className="flex gap-3 justify-end pt-3 border-t border-cockpit-line">
          <button type="button" onClick={onClose} className="hud-btn hud-btn--cyan !px-4 !py-2" disabled={busy}>
            Cancelar
          </button>
          <button type="submit" className="hud-btn !px-5 !py-2" disabled={busy}>
            {busy ? 'Guardando…' : (esEdicion ? 'Guardar cambios' : 'Crear cliente')}
          </button>
        </div>
      </form>
    </HudModal>
  );
}
