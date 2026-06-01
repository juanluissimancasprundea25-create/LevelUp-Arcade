import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  UserCog, Save, Lock, Mail, IdCard, Phone, MapPin, User as UserIcon,
  Eye, EyeOff, Loader2, ShieldCheck, AlertCircle,
} from 'lucide-react';
import { api } from '../../lib/api.js';
import { useToast } from '../../components/admin/Toast.jsx';

/**
 * Mi perfil — URL: /cockpit/mi-cuenta/perfil
 *
 * Dos pestañas:
 *  - "Datos personales": editar nombre, apellidos, NIF, teléfono, dirección
 *    El email no se puede cambiar. El NIF solo si está vacío (integridad fiscal).
 *  - "Seguridad": cambiar contraseña (requiere actual)
 */
export default function MiPerfil() {
  const [tab, setTab] = useState('datos');

  return (
    <div className="space-y-5">
      <header>
        <p className="hud-label text-cockpit-cyan/70">// CUENTA</p>
        <h2 className="font-display text-xl tracking-wide text-white mt-0.5 flex items-center gap-2">
          <UserCog className="text-cockpit-neon" size={20} />
          Mi perfil
        </h2>
      </header>

      {/* Tabs */}
      <div className="hud-panel p-1 inline-flex">
        <TabButton activo={tab === 'datos'} onClick={() => setTab('datos')}>
          <UserIcon size={12} /> Datos personales
        </TabButton>
        <TabButton activo={tab === 'seguridad'} onClick={() => setTab('seguridad')}>
          <Lock size={12} /> Seguridad
        </TabButton>
      </div>

      <AnimatePresence mode="wait">
        {tab === 'datos' ? (
          <motion.div
            key="datos"
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.2 }}
          >
            <TabDatos />
          </motion.div>
        ) : (
          <motion.div
            key="seg"
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.2 }}
          >
            <TabSeguridad />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ---------- TAB BUTTON ---------- */
function TabButton({ activo, onClick, children }) {
  return (
    <button
      onClick={onClick}
      className={[
        'px-4 py-2 text-xs tracking-widest uppercase font-display transition-all',
        'flex items-center gap-2',
        activo
          ? 'bg-cockpit-neon/15 text-white border border-cockpit-neon/40'
          : 'text-slate-400 hover:text-white border border-transparent',
      ].join(' ')}
    >
      {children}
    </button>
  );
}

/* ====================== TAB 1: DATOS PERSONALES ====================== */
const emptyDatos = {
  email: '', nombre: '', apellidos: '',
  nif: '', telefono: '',
  direccion: '', ciudad: '', codigoPostal: '', pais: '',
};

function TabDatos() {
  const [form, setForm] = useState(emptyDatos);
  const [original, setOriginal] = useState(emptyDatos);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [nifBloqueado, setNifBloqueado] = useState(false);
  const toast = useToast();

  // Cargar perfil
  useEffect(() => {
    let cancelado = false;
    async function cargar() {
      try {
        const { data } = await api.get('/mi-perfil');
        if (cancelado) return;
        const datos = {
          email:        data.email || '',
          nombre:       data.nombre || '',
          apellidos:    data.apellidos || '',
          nif:          data.nif || '',
          telefono:     data.telefono || '',
          direccion:    data.direccion || '',
          ciudad:       data.ciudad || '',
          codigoPostal: data.codigoPostal || '',
          pais:         data.pais || '',
        };
        setForm(datos);
        setOriginal(datos);
        setNifBloqueado(!!data.nif);
      } catch (_) {
        toast.err('No se pudo cargar tu perfil');
      } finally {
        if (!cancelado) setLoading(false);
      }
    }
    cargar();
    return () => { cancelado = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  const hayCambios = JSON.stringify(form) !== JSON.stringify(original);

  function validar() {
    const e = {};
    if (!form.nombre.trim()) e.nombre = 'El nombre es obligatorio';
    if (form.nif && !/^[0-9]{8}[A-Za-z]$|^[XYZ][0-9]{7}[A-Za-z]$/.test(form.nif.trim())) {
      e.nif = 'NIF inválido';
    }
    if (form.telefono && !/^[+]?[0-9 ]{6,20}$/.test(form.telefono.trim())) {
      e.telefono = 'Teléfono inválido';
    }
    if (form.codigoPostal && !/^[0-9]{4,10}$/.test(form.codigoPostal.trim())) {
      e.codigoPostal = 'Código postal: solo dígitos';
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function guardar(ev) {
    ev.preventDefault();
    if (guardando || !validar()) return;
    setGuardando(true);
    try {
      const payload = {
        nombre:       form.nombre.trim(),
        apellidos:    form.apellidos.trim(),
        nif:          form.nif.trim().toUpperCase(),
        telefono:     form.telefono.trim(),
        direccion:    form.direccion.trim(),
        ciudad:       form.ciudad.trim(),
        codigoPostal: form.codigoPostal.trim(),
        pais:         form.pais.trim(),
      };
      const { data } = await api.put('/mi-perfil', payload);
      toast.ok('Perfil actualizado');
      const datos = {
        email:        data.email || '',
        nombre:       data.nombre || '',
        apellidos:    data.apellidos || '',
        nif:          data.nif || '',
        telefono:     data.telefono || '',
        direccion:    data.direccion || '',
        ciudad:       data.ciudad || '',
        codigoPostal: data.codigoPostal || '',
        pais:         data.pais || '',
      };
      setForm(datos);
      setOriginal(datos);
      setNifBloqueado(!!data.nif);
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo actualizar');
    } finally {
      setGuardando(false);
    }
  }

  if (loading) {
    return (
      <div className="hud-panel p-5 space-y-3">
        <div className="h-4 w-32 bg-white/5 animate-pulse rounded" />
        <div className="grid grid-cols-2 gap-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-9 bg-white/5 animate-pulse rounded" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={guardar} className="hud-panel p-5 space-y-5">
      {/* Email no editable */}
      <div>
        <p className="hud-label mb-2">// CUENTA</p>
        <Campo
          label="Email (no editable)"
          icon={Mail}
          value={form.email}
          onChange={() => {}}
          disabled
          colSpan="md:col-span-2"
          hint="El email no se puede modificar aquí. Si necesitas cambiarlo, contacta con soporte."
        />
      </div>

      {/* Identidad */}
      <div>
        <p className="hud-label mb-2">// IDENTIDAD</p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <Campo
            label="Nombre *"
            icon={UserIcon}
            value={form.nombre}
            onChange={(v) => set('nombre', v)}
            error={errors.nombre}
            disabled={guardando}
          />
          <Campo
            label="Apellidos"
            icon={UserIcon}
            value={form.apellidos}
            onChange={(v) => set('apellidos', v)}
            disabled={guardando}
          />
          <Campo
            label={nifBloqueado ? 'NIF (no editable)' : 'NIF'}
            icon={IdCard}
            value={form.nif}
            onChange={(v) => set('nif', v.toUpperCase())}
            error={errors.nif}
            disabled={guardando || nifBloqueado}
            placeholder="12345678A"
            hint={nifBloqueado
              ? 'Una vez registrado no se puede cambiar (integridad fiscal)'
              : 'Solo si lo necesitas para facturación'}
          />
          <Campo
            label="Teléfono"
            icon={Phone}
            value={form.telefono}
            onChange={(v) => set('telefono', v)}
            error={errors.telefono}
            placeholder="+34 600 000 000"
            disabled={guardando}
          />
        </div>
      </div>

      {/* Dirección */}
      <div>
        <p className="hud-label mb-2">// DIRECCIÓN DE ENVÍO</p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <Campo
            label="Calle"
            icon={MapPin}
            value={form.direccion}
            onChange={(v) => set('direccion', v)}
            disabled={guardando}
            colSpan="md:col-span-3"
          />
          <Campo
            label="Ciudad"
            value={form.ciudad}
            onChange={(v) => set('ciudad', v)}
            disabled={guardando}
          />
          <Campo
            label="C.P."
            value={form.codigoPostal}
            onChange={(v) => set('codigoPostal', v)}
            error={errors.codigoPostal}
            placeholder="28013"
            disabled={guardando}
          />
          <Campo
            label="País"
            value={form.pais}
            onChange={(v) => set('pais', v)}
            disabled={guardando}
          />
        </div>
      </div>

      {/* Acciones */}
      <div className="flex items-center justify-between pt-3 border-t border-cockpit-line gap-3 flex-wrap">
        <p className="hud-readout text-[10px] text-slate-500">
          {hayCambios ? '// CAMBIOS SIN GUARDAR' : '// SIN CAMBIOS PENDIENTES'}
        </p>
        <button
          type="submit"
          disabled={guardando || !hayCambios}
          className="hud-btn !px-5 !py-2"
        >
          {guardando
            ? <><Loader2 size={14} className="animate-spin" /> Guardando…</>
            : <><Save size={14} /> Guardar cambios</>}
        </button>
      </div>
    </form>
  );
}

/* ====================== TAB 2: SEGURIDAD ====================== */
function TabSeguridad() {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [nueva2, setNueva2] = useState('');
  const [verActual, setVerActual] = useState(false);
  const [verNueva, setVerNueva] = useState(false);
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [exitoso, setExitoso] = useState(false);
  const toast = useToast();

  function validar() {
    const e = {};
    if (!actual) e.actual = 'Indica tu contraseña actual';
    if (!nueva) e.nueva = 'Indica la nueva contraseña';
    else if (nueva.length < 8) e.nueva = 'Mínimo 8 caracteres';
    if (nueva !== nueva2) e.nueva2 = 'Las contraseñas no coinciden';
    if (actual && nueva && actual === nueva) e.nueva = 'Debe ser distinta de la actual';
    setErrores(e);
    return Object.keys(e).length === 0;
  }

  async function cambiar(ev) {
    ev.preventDefault();
    if (guardando || !validar()) return;
    setGuardando(true);
    setExitoso(false);
    try {
      await api.post('/mi-perfil/cambiar-password', {
        passwordActual: actual,
        passwordNueva: nueva,
      });
      toast.ok('Contraseña actualizada correctamente');
      setExitoso(true);
      setActual('');
      setNueva('');
      setNueva2('');
      setTimeout(() => setExitoso(false), 4000);
    } catch (e) {
      const msg = e.response?.data?.mensaje || 'No se pudo cambiar la contraseña';
      if (e.response?.status === 401) {
        setErrores({ actual: msg });
      } else {
        toast.err(msg);
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="space-y-4">
      {/* Aviso seguridad */}
      <div className="hud-panel p-3 !bg-black/30 flex gap-2">
        <ShieldCheck size={14} className="text-cockpit-cyan shrink-0 mt-0.5" />
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Por seguridad necesitamos verificar tu contraseña actual antes de
          cambiarla. La nueva debe tener al menos <strong>8 caracteres</strong>
          {' '}y ser distinta de la anterior.
        </p>
      </div>

      <form onSubmit={cambiar} className="hud-panel p-5 space-y-4">
        <div>
          <p className="hud-label mb-2">// CAMBIAR CONTRASEÑA</p>
        </div>

        <Campo
          label="Contraseña actual *"
          icon={Lock}
          type={verActual ? 'text' : 'password'}
          value={actual}
          onChange={setActual}
          error={errores.actual}
          disabled={guardando}
          rightSlot={(
            <button
              type="button"
              onClick={() => setVerActual(v => !v)}
              className="text-slate-500 hover:text-white"
              tabIndex={-1}
            >
              {verActual ? <EyeOff size={12} /> : <Eye size={12} />}
            </button>
          )}
        />

        <Campo
          label="Nueva contraseña *"
          icon={Lock}
          type={verNueva ? 'text' : 'password'}
          value={nueva}
          onChange={setNueva}
          error={errores.nueva}
          disabled={guardando}
          hint="Mínimo 8 caracteres"
          rightSlot={(
            <button
              type="button"
              onClick={() => setVerNueva(v => !v)}
              className="text-slate-500 hover:text-white"
              tabIndex={-1}
            >
              {verNueva ? <EyeOff size={12} /> : <Eye size={12} />}
            </button>
          )}
        />

        <Campo
          label="Confirmar nueva contraseña *"
          icon={Lock}
          type={verNueva ? 'text' : 'password'}
          value={nueva2}
          onChange={setNueva2}
          error={errores.nueva2}
          disabled={guardando}
        />

        {exitoso && (
          <motion.div
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            className="p-3 border border-cockpit-ok/50 bg-cockpit-ok/5 flex gap-2"
          >
            <ShieldCheck size={14} className="text-cockpit-ok shrink-0 mt-0.5" />
            <p className="text-xs text-slate-200">
              Contraseña actualizada. Úsala la próxima vez que inicies sesión.
            </p>
          </motion.div>
        )}

        <div className="flex justify-end pt-3 border-t border-cockpit-line">
          <button
            type="submit"
            disabled={guardando || !actual || !nueva || !nueva2}
            className="hud-btn !px-5 !py-2"
          >
            {guardando
              ? <><Loader2 size={14} className="animate-spin" /> Cambiando…</>
              : <><Lock size={14} /> Cambiar contraseña</>}
          </button>
        </div>
      </form>
    </div>
  );
}

/* ---------- Campo reutilizable ---------- */
function Campo({ label, icon: Icon, type = 'text', value, onChange, error, disabled, placeholder, hint, rightSlot, colSpan }) {
  return (
    <label className={`block ${colSpan || ''}`}>
      <span className="hud-label">{label}</span>
      <div className="relative mt-1.5">
        {Icon && (
          <Icon size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
        )}
        <input
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          disabled={disabled}
          className={`hud-input ${Icon ? '!pl-8' : ''} ${rightSlot ? '!pr-9' : ''}`}
        />
        {rightSlot && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            {rightSlot}
          </div>
        )}
      </div>
      {error && <p className="text-xs text-cockpit-danger mt-1 flex items-center gap-1"><AlertCircle size={11} /> {error}</p>}
      {!error && hint && <p className="text-[10px] text-slate-500 mt-1">{hint}</p>}
    </label>
  );
}
