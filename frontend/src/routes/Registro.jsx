import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  UserPlus, Mail, Lock, User as UserIcon, IdCard,
  Phone, MapPin, ArrowLeft, Loader2, Eye, EyeOff,
} from 'lucide-react';
import { api } from '../lib/api.js';
import { useToast } from '../components/admin/Toast.jsx';
import { HeaderTienda } from '../components/tienda/HeaderTienda.jsx';

/**
 * Registro público de nuevo cliente. URL: /cockpit/registro
 *
 *  - Llama a POST /api/publico/registro (sin auth)
 *  - Tras éxito, el backend devuelve un JWT y el cliente queda logueado
 *  - Redirige a /cockpit/tienda (o a la URL "vuelve" si venía de algún sitio)
 */
const empty = {
  email: '', password: '', password2: '',
  nombre: '', apellidos: '',
  nif: '', telefono: '',
  direccion: '', ciudad: '', codigoPostal: '', pais: 'España',
};

export default function Registro() {
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [verPass, setVerPass] = useState(false);
  const [aceptaTerminos, setAcepta] = useState(false);
  const toast = useToast();
  const navigate = useNavigate();

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  function validar() {
    const e = {};
    if (!form.email.trim()) e.email = 'El email es obligatorio';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) e.email = 'Email no válido';

    if (!form.password) e.password = 'La contraseña es obligatoria';
    else if (form.password.length < 8) e.password = 'Mínimo 8 caracteres';

    if (form.password !== form.password2) e.password2 = 'Las contraseñas no coinciden';

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

  async function onSubmit(ev) {
    ev.preventDefault();
    if (busy) return;
    if (!aceptaTerminos) {
      toast.warn('Debes aceptar los términos para continuar');
      return;
    }
    if (!validar()) return;

    setBusy(true);
    const clean = (s) => (s && s.trim()) || null;
    const payload = {
      email:        form.email.trim().toLowerCase(),
      password:     form.password,
      nombre:       form.nombre.trim(),
      apellidos:    clean(form.apellidos),
      nif:          clean(form.nif),
      telefono:     clean(form.telefono),
      direccion:    clean(form.direccion),
      ciudad:       clean(form.ciudad),
      codigoPostal: clean(form.codigoPostal),
      pais:         clean(form.pais),
    };

    try {
      const { data } = await api.post('/publico/registro', payload);
      // Guardamos el token en sessionStorage usando la misma clave que el resto
      // de la app (lvlup_token). Hacemos un recargado completo para que el
      // AuthProvider releve el token al inicializar.
      try {
        sessionStorage.setItem('lvlup_token', data.token);
      } catch (_) { /* sin storage */ }
      toast.ok(`¡Bienvenido, ${data.nombre}!`);
      // Redirección: si venía con vuelve, ir allí; sino al carrito si hay items, sino a la tienda
      const vuelve = new URLSearchParams(window.location.search).get('vuelve');
      // Pequeño delay para que se vea el toast antes del reload
      setTimeout(() => {
  // Respeta el base path de Vite en dev (/app/) y producción (/app/ vía Spring)
  const base = import.meta.env.BASE_URL || '/';
  const destino = vuelve || '/cockpit/tienda';
  window.location.href = base.replace(/\/$/, '') + destino;
}, 600);
    } catch (err) {
      const msg = err.response?.data?.mensaje || err.response?.data?.message || 'No se pudo completar el registro';
      toast.err(msg);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <HeaderTienda />

      <main className="flex-1 max-w-3xl mx-auto w-full px-4 sm:px-6 py-6">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mb-4"
        >
          <Link
            to="/cockpit/tienda"
            className="inline-flex items-center gap-1.5 text-xs tracking-widest uppercase text-cockpit-cyan hover:text-white transition-colors"
          >
            <ArrowLeft size={12} />
            Volver a la tienda
          </Link>
        </motion.div>

        <motion.header
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-5"
        >
          <p className="hud-label text-cockpit-cyan/70">// REGISTRO</p>
          <h1 className="font-display text-3xl tracking-wider text-white mt-1 flex items-center gap-3">
            <UserPlus className="text-cockpit-neon" size={26} />
            Crea tu cuenta
          </h1>
          <p className="text-sm text-slate-400 mt-2">
            Sólo necesitas un email y una contraseña. Los datos comerciales son opcionales y
            puedes completarlos después en tu perfil.
          </p>
        </motion.header>

        <motion.form
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.05 }}
          onSubmit={onSubmit}
          className="hud-panel p-5 space-y-5"
        >
          {/* Sección credenciales */}
          <div>
            <p className="hud-label mb-2">// CREDENCIALES</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <Campo
                label="Email *"
                icon={Mail}
                type="email"
                value={form.email}
                onChange={(v) => set('email', v)}
                error={errors.email}
                disabled={busy}
                colSpan="md:col-span-2"
              />
              <Campo
                label="Contraseña *"
                icon={Lock}
                type={verPass ? 'text' : 'password'}
                value={form.password}
                onChange={(v) => set('password', v)}
                error={errors.password}
                disabled={busy}
                hint="Mínimo 8 caracteres"
                rightSlot={(
                  <button
                    type="button"
                    onClick={() => setVerPass(v => !v)}
                    className="text-slate-500 hover:text-white"
                    tabIndex={-1}
                  >
                    {verPass ? <EyeOff size={12} /> : <Eye size={12} />}
                  </button>
                )}
              />
              <Campo
                label="Confirmar contraseña *"
                icon={Lock}
                type={verPass ? 'text' : 'password'}
                value={form.password2}
                onChange={(v) => set('password2', v)}
                error={errors.password2}
                disabled={busy}
              />
            </div>
          </div>

          {/* Sección identidad */}
          <div>
            <p className="hud-label mb-2">// IDENTIDAD</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <Campo
                label="Nombre *"
                icon={UserIcon}
                value={form.nombre}
                onChange={(v) => set('nombre', v)}
                error={errors.nombre}
                disabled={busy}
              />
              <Campo
                label="Apellidos"
                icon={UserIcon}
                value={form.apellidos}
                onChange={(v) => set('apellidos', v)}
                disabled={busy}
              />
              <Campo
                label="NIF"
                icon={IdCard}
                value={form.nif}
                onChange={(v) => set('nif', v.toUpperCase())}
                error={errors.nif}
                placeholder="12345678A"
                disabled={busy}
              />
              <Campo
                label="Teléfono"
                icon={Phone}
                value={form.telefono}
                onChange={(v) => set('telefono', v)}
                error={errors.telefono}
                placeholder="+34 600 000 000"
                disabled={busy}
              />
            </div>
          </div>

          {/* Sección dirección */}
          <div>
            <p className="hud-label mb-2">// DIRECCIÓN DE ENVÍO (OPCIONAL)</p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <Campo
                label="Calle"
                icon={MapPin}
                value={form.direccion}
                onChange={(v) => set('direccion', v)}
                disabled={busy}
                colSpan="md:col-span-3"
              />
              <Campo
                label="Ciudad"
                value={form.ciudad}
                onChange={(v) => set('ciudad', v)}
                disabled={busy}
              />
              <Campo
                label="C.P."
                value={form.codigoPostal}
                onChange={(v) => set('codigoPostal', v)}
                error={errors.codigoPostal}
                placeholder="28013"
                disabled={busy}
              />
              <Campo
                label="País"
                value={form.pais}
                onChange={(v) => set('pais', v)}
                disabled={busy}
              />
            </div>
          </div>

          {/* Términos */}
          <label className="flex items-start gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={aceptaTerminos}
              onChange={(e) => setAcepta(e.target.checked)}
              className="accent-cockpit-neon w-4 h-4 mt-0.5"
              disabled={busy}
            />
            <span className="text-xs text-slate-400 leading-relaxed">
              Acepto los términos y condiciones de uso y la política de privacidad de
              LevelUp Arcade. Mis datos serán tratados conforme al RGPD.
            </span>
          </label>

          <div className="pt-3 border-t border-cockpit-line flex flex-col sm:flex-row sm:items-center gap-3 sm:justify-between">
            <p className="text-xs text-slate-500">
              ¿Ya tienes cuenta?{' '}
              <Link to="/cockpit/login" className="text-cockpit-cyan hover:text-white">
                Inicia sesión
              </Link>
            </p>
            <button
              type="submit"
              className="hud-btn !px-5 !py-2.5"
              disabled={busy || !aceptaTerminos}
            >
              {busy
                ? <><Loader2 size={14} className="animate-spin" /> Creando cuenta…</>
                : <><UserPlus size={14} /> Crear mi cuenta</>
              }
            </button>
          </div>
        </motion.form>
      </main>
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
      {error && <p className="text-xs text-cockpit-danger mt-1">{error}</p>}
      {!error && hint && <p className="text-[10px] text-slate-500 mt-1">{hint}</p>}
    </label>
  );
}
