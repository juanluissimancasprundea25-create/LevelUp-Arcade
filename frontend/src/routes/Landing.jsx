import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { LogIn, LogOut, Cpu, ShoppingBag, UserPlus } from 'lucide-react';
import { HudFrame } from '../components/HudFrame.jsx';
import { useAuth } from '../lib/auth.jsx';
import { useCockpit } from '../scenes/cockpitStore.js';

/**
 * Landing page del cockpit. Comportamiento según el rol:
 *  - No autenticado: botones "Acceder" / "Registrarse" / "Ir a la tienda"
 *  - CLIENTE: botones "Ir a la tienda" / "Mi cuenta" + cerrar sesión
 *  - ADMIN o EMPLEADO: botones "Ir al puente (panel)" / "Ver tienda" + cerrar sesión
 */
export default function Landing() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const setScene = useCockpit((s) => s.setScene);

  // Pose de cámara landing al entrar
  useEffect(() => {
    setScene('landing');
  }, [setScene]);

  const rolNormalizado = String(user?.rol || '').replace(/^ROLE_/, '').toUpperCase();
  const esCliente = rolNormalizado === 'CLIENTE';
  const esStaff   = rolNormalizado === 'ADMIN' || rolNormalizado === 'EMPLEADO';

  function cerrarSesion() {
    logout();
    navigate('/cockpit');
  }

  return (
    <HudFrame>
      <div className="absolute inset-0 grid place-items-center pointer-events-none">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
          className="text-center pointer-events-auto max-w-2xl px-6"
        >
          <p className="hud-label text-cockpit-cyan/80 mb-3">// LEVELUP ARCADE · COCKPIT</p>
          <h1 className="font-display text-5xl md:text-6xl tracking-wider text-white">
            LEVEL<span className="text-cockpit-neon">UP</span>
          </h1>
          <p className="text-slate-300 mt-4 text-sm md:text-base">
            {!user
              ? 'Tienda de videojuegos, merchandising y coleccionables. Sistema de gestión integrado para el equipo y experiencia de cliente premium.'
              : esCliente
                ? `Bienvenido, ${user.nombre || 'piloto'}. Tu próxima aventura te espera.`
                : `Conectado como ${user.nombre || 'operador'}. Acceso autorizado al puente de mando.`
            }
          </p>

          <div className="mt-8 flex flex-wrap gap-3 justify-center">
            {!user ? (
              <>
                <Link to="/cockpit/tienda" className="hud-btn !px-5 !py-3">
                  <ShoppingBag size={14} />
                  Ir a la tienda
                </Link>
                <Link to="/cockpit/login" className="hud-btn hud-btn--cyan !px-5 !py-3">
                  <LogIn size={14} />
                  Acceder
                </Link>
                <Link to="/cockpit/registro" className="hud-btn hud-btn--cyan !px-5 !py-3">
                  <UserPlus size={14} />
                  Registrarse
                </Link>
              </>
            ) : esStaff ? (
              <>
                <Link to="/cockpit/admin" className="hud-btn !px-5 !py-3">
                  <Cpu size={14} />
                  Ir al puente
                </Link>
                <Link to="/cockpit/tienda" className="hud-btn hud-btn--cyan !px-5 !py-3">
                  <ShoppingBag size={14} />
                  Ver tienda
                </Link>
                <button onClick={cerrarSesion} className="hud-btn hud-btn--cyan !px-5 !py-3">
                  <LogOut size={14} />
                  Cerrar sesión
                </button>
              </>
            ) : (
              <>
                <Link to="/cockpit/tienda" className="hud-btn !px-5 !py-3">
                  <ShoppingBag size={14} />
                  Explorar tienda
                </Link>
                <Link to="/cockpit/mi-cuenta" className="hud-btn hud-btn--cyan !px-5 !py-3">
                  <Cpu size={14} />
                  Mi cuenta
                </Link>
                <button onClick={cerrarSesion} className="hud-btn hud-btn--cyan !px-5 !py-3">
                  <LogOut size={14} />
                  Cerrar sesión
                </button>
              </>
            )}
          </div>
        </motion.div>
      </div>
    </HudFrame>
  );
}
