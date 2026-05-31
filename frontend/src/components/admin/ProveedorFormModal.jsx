import { useEffect, useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { api } from '../../lib/api.js';
import { useToast } from './Toast.jsx';

/**
 * Modal de creación / edición de proveedor.
 *   - Sin `proveedor` → CREAR (POST /proveedores)
 *   - Con `proveedor` → EDITAR (PUT /proveedores/{id})
 */
const empty = {
  nombreEmpresa: '', cif: '',
  emailContacto: '', telefono: '',
  direccion: '', ciudad: '', codigoPostal: '', pais: 'España',
  activo: true,
};

export function ProveedorFormModal({ open, onClose, proveedor, onSaved }) {
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const esEdicion = !!proveedor?.id;

  useEffect(() => {
    if (!open) return;
    if (proveedor) {
      setForm({
        nombreEmpresa: proveedor.nombreEmpresa || '',
        cif:           proveedor.cif || '',
        emailContacto: proveedor.emailContacto || '',
        telefono:      proveedor.telefono || '',
        direccion:     proveedor.direccion || '',
        ciudad:        proveedor.ciudad || '',
        codigoPostal:  proveedor.codigoPostal || '',
        pais:          proveedor.pais || 'España',
        activo:        proveedor.activo ?? true,
      });
    } else {
      setForm(empty);
    }
    setErrors({});
  }, [open, proveedor]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  function validar() {
    const e = {};
    if (!form.nombreEmpresa.trim()) e.nombreEmpresa = 'Obligatorio';
    if (!form.cif.trim()) {
      e.cif = 'El CIF es obligatorio';
    } else if (!/^[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]$/.test(form.cif.trim().toUpperCase())) {
      e.cif = 'CIF inválido (ej: A12345678)';
    }
    if (form.emailContacto && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.emailContacto.trim())) {
      e.emailContacto = 'Email no válido';
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
    const clean = (s) => (s && s.trim()) || null;
    const payload = {
      nombreEmpresa: form.nombreEmpresa.trim(),
      cif:           form.cif.trim().toUpperCase(),
      emailContacto: clean(form.emailContacto),
      telefono:      clean(form.telefono),
      direccion:     clean(form.direccion),
      ciudad:        clean(form.ciudad),
      codigoPostal:  clean(form.codigoPostal),
      pais:          clean(form.pais),
    };
    if (esEdicion) payload.activo = !!form.activo;

    try {
      const { data } = esEdicion
        ? await api.put(`/proveedores/${proveedor.id}`, payload)
        : await api.post('/proveedores', payload);
      toast.ok(esEdicion ? 'Proveedor actualizado' : 'Proveedor creado');
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
      title={esEdicion ? `Editar · ${proveedor?.nombreEmpresa}` : 'Nuevo proveedor'}
      subtitle={esEdicion ? 'EDITAR' : 'ALTA'}
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Sección: Identidad fiscal */}
        <div>
          <p className="hud-label mb-2">// IDENTIDAD FISCAL</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label className="block">
              <span className="hud-label">Nombre empresa *</span>
              <input
                type="text"
                value={form.nombreEmpresa}
                onChange={(e) => set('nombreEmpresa', e.target.value)}
                placeholder="Distribuciones Galaxia S.L."
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={200}
              />
              {errors.nombreEmpresa && <p className="text-xs text-cockpit-danger mt-1">{errors.nombreEmpresa}</p>}
            </label>

            <label className="block">
              <span className="hud-label">CIF *</span>
              <input
                type="text"
                value={form.cif}
                onChange={(e) => set('cif', e.target.value.toUpperCase())}
                placeholder="A12345678"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={20}
              />
              {errors.cif && <p className="text-xs text-cockpit-danger mt-1">{errors.cif}</p>}
            </label>
          </div>
        </div>

        {/* Sección: Contacto */}
        <div>
          <p className="hud-label mb-2">// CONTACTO</p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label className="block">
              <span className="hud-label">Email de contacto</span>
              <input
                type="email"
                value={form.emailContacto}
                onChange={(e) => set('emailContacto', e.target.value)}
                placeholder="ventas@empresa.com"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={150}
              />
              {errors.emailContacto && <p className="text-xs text-cockpit-danger mt-1">{errors.emailContacto}</p>}
            </label>

            <label className="block">
              <span className="hud-label">Teléfono</span>
              <input
                type="text"
                value={form.telefono}
                onChange={(e) => set('telefono', e.target.value)}
                placeholder="+34 900 000 000"
                className="hud-input mt-1.5"
                disabled={busy}
                maxLength={20}
              />
              {errors.telefono && <p className="text-xs text-cockpit-danger mt-1">{errors.telefono}</p>}
            </label>
          </div>
        </div>

        {/* Sección: Dirección */}
        <div>
          <p className="hud-label mb-2">// DIRECCIÓN</p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <label className="block md:col-span-3">
              <span className="hud-label">Calle</span>
              <input
                type="text"
                value={form.direccion}
                onChange={(e) => set('direccion', e.target.value)}
                placeholder="Polígono industrial Norte, nave 12"
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
                placeholder="28850"
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

        <div className="flex gap-3 justify-end pt-3 border-t border-cockpit-line">
          <button type="button" onClick={onClose} className="hud-btn hud-btn--cyan !px-4 !py-2" disabled={busy}>
            Cancelar
          </button>
          <button type="submit" className="hud-btn !px-5 !py-2" disabled={busy}>
            {busy ? 'Guardando…' : (esEdicion ? 'Guardar cambios' : 'Crear proveedor')}
          </button>
        </div>
      </form>
    </HudModal>
  );
}
