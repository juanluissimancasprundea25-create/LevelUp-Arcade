import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HudFrame } from '../components/HudFrame.jsx';
import { useCockpit } from '../scenes/cockpitStore.js';
import { useAuth } from '../lib/auth.jsx';

export default function Login() {
  const navigate = useNavigate();
  const setScene = useCockpit((s) => s.setScene);
  const warpTo = useCockpit((s) => s.warpTo);
  const { login, error } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { setScene('login'); }, [setScene]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    const ok = await login(email.trim(), password);
    setBusy(false);
    if (ok) {
  await warpTo('dashboard');
  // Esperamos a que /me termine y luego decidimos a donde ir segun el rol.
  // login() ya hizo refresh() del user, asi que leemos directo del token:
  const token = sessionStorage.getItem('lvlup_token');
  let rol = 'CLIENTE';
  try {
    const res = await fetch('/api/auth/me', {
      headers: { Authorization: 'Bearer ' + token }
    });
    const data = await res.json();
    rol = String(data.rol || '').replace(/^ROLE_/, '').toUpperCase();
  } catch (_) {}

  if (rol === 'ADMIN' || rol === 'EMPLEADO') {
    navigate('/cockpit/admin', { replace: true });
  } else {
    // Cliente: por ahora no hay zona cliente en el cockpit, asi que
    // lo mandamos a la landing pero ya logueado. En Fase 7 sustituimos
    // esto por /cockpit/tienda o /cockpit/cuenta.
    navigate('/cockpit', { replace: true });
  }
}
  }

  return (
    <HudFrame
      title="AUTENTICACION"
      subtitle="// Acceso restringido"
      telemetry={[
        { label: 'CANAL', value: 'JWT/256' },
        { label: 'CIFRADO', value: 'BCRYPT' },
      ]}
    >
      <motion.form
        onSubmit={handleSubmit}
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5, delay: 0.2 }}
        className="hud-panel w-[92%] max-w-md p-8 md:p-10"
      >
        <div className="mb-6">
          <p className="hud-label">// PROTOCOLO DE ACCESO</p>
          <h2 className="mt-1 font-display text-2xl tracking-wider text-white">
            Identificate, piloto
          </h2>
        </div>

        <label className="block mb-4">
          <span className="hud-label">Email</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="capitan@levelup.arcade"
            className="hud-input mt-1.5"
            autoComplete="username"
          />
        </label>

        <label className="block mb-6">
          <span className="hud-label">Contrasena</span>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="********"
            className="hud-input mt-1.5"
            autoComplete="current-password"
          />
        </label>

        {error && (
          <div className="mb-4 px-3 py-2 border border-cockpit-danger text-cockpit-danger text-xs tracking-wider bg-cockpit-danger/10">
            {error}
          </div>
        )}

        <button type="submit" disabled={busy} className="hud-btn w-full">
          {busy ? 'VERIFICANDO...' : 'ENGAGE'}
        </button>

        <div className="mt-6 flex items-center justify-center text-[11px] tracking-widest uppercase">
          <button
            type="button"
            onClick={() => navigate('/cockpit')}
            className="text-slate-400 hover:text-cockpit-cyan transition-colors"
          >
            Volver
          </button>
        </div>
      </motion.form>
    </HudFrame>
  );
}