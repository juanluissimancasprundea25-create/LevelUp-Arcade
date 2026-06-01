import { useEffect } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { HudFrame } from '../components/HudFrame.jsx';
import { useCockpit } from '../scenes/cockpitStore.js';
import { useAuth } from '../lib/auth.jsx';

export default function Dashboard() {
  const setScene = useCockpit((s) => s.setScene);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => { setScene('dashboard'); }, [setScene]);

  function handleLogout() {
    logout();
    navigate('/', { replace: true });
  }

  return (
    <HudFrame
      title="PUENTE DE MANDO"
      subtitle={`// ${user?.rol || 'SIN ROL'} · ${user?.email || ''}`}
      telemetry={[
        { label: 'SECTOR',  value: 'COMMAND' },
        { label: 'SESSION', value: 'ACTIVE' },
        { label: 'IA',      value: 'ONLINE' },
      ]}
    >
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.3 }}
        className="hud-panel max-w-3xl w-[92%] p-10 text-center"
      >
        <p className="hud-label">// SISTEMA OPERATIVO</p>
        <h2 className="mt-2 font-display text-3xl md:text-4xl text-white tracking-wider">
          Bienvenido, {user?.nombre || user?.email?.split('@')[0] || 'piloto'}
        </h2>
        <p className="mt-4 text-slate-300/80 text-sm">
          Has aterrizado en el puente de mando. Las consolas de productos,
          clientes, pedidos e IA se desbloquean en las siguientes fases.
        </p>

        <div className="mt-8 grid grid-cols-2 md:grid-cols-4 gap-3">
          {['PRODUCTOS', 'CLIENTES', 'PEDIDOS', 'IA'].map((k) => (
            <div key={k} className="border border-cockpit-line py-3 text-[11px] tracking-[0.3em] text-slate-400">
              {k}<br/><span className="text-cockpit-cyan/70">PRÓXIMAMENTE</span>
            </div>
          ))}
        </div>

        <button onClick={handleLogout} className="hud-btn hud-btn--cyan mt-10">
          ⏻  Cerrar sesión
        </button>
      </motion.div>
    </HudFrame>
  );
}
