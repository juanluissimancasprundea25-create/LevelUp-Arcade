import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard, Package, FileText, RotateCcw, MessageSquare,
  UserCog, LogOut,
} from 'lucide-react';
import { HeaderTienda } from '../../components/tienda/HeaderTienda.jsx';
import { useAuth } from '../../lib/auth.jsx';

/**
 * Layout del área "Mi cuenta" del cliente. URL base: /cockpit/mi-cuenta
 *
 *  - Cabecera de tienda arriba (compartida con catálogo y carrito)
 *  - Sidebar lateral izquierdo con 6 secciones
 *  - Contenido en <Outlet /> para sub-rutas
 *
 * En móvil el sidebar se convierte en barra superior horizontal scrolleable.
 */
const SECCIONES = [
  { to: '',                 label: 'Resumen',     icon: LayoutDashboard, end: true },
  { to: 'pedidos',          label: 'Mis pedidos', icon: Package },
  { to: 'facturas',         label: 'Facturas',    icon: FileText },
  { to: 'devoluciones',     label: 'Devoluciones', icon: RotateCcw },
  { to: 'chat',             label: 'Mensajes',    icon: MessageSquare },
  { to: 'perfil',           label: 'Mi perfil',   icon: UserCog },
];

export function MiCuentaLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <div className="min-h-screen flex flex-col">
      <HeaderTienda />

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 py-6">
        <motion.header
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-5"
        >
          <p className="hud-label text-cockpit-cyan/70">// PILOTO · {user?.email}</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Hola, {user?.nombre || 'piloto'}
          </h1>
        </motion.header>

        <div className="grid grid-cols-1 md:grid-cols-[220px_1fr] gap-5">
          {/* Sidebar / nav */}
          <motion.nav
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4 }}
            className="hud-panel p-2 md:p-3 h-fit md:sticky md:top-20"
          >
            {/* En móvil: barra horizontal scrolleable */}
            <div className="flex md:flex-col gap-1 overflow-x-auto md:overflow-visible">
              {SECCIONES.map((s) => {
                const Icon = s.icon;
                return (
                  <NavLink
                    key={s.to}
                    to={s.to}
                    end={s.end}
                    className={({ isActive }) => [
                      'flex items-center gap-2 px-3 py-2 text-xs tracking-widest uppercase',
                      'transition-colors whitespace-nowrap md:whitespace-normal',
                      isActive
                        ? 'bg-cockpit-neon/15 text-white border-l-2 md:border-l-2 border-l-cockpit-neon md:border-cockpit-neon/40'
                        : 'text-slate-400 hover:text-white hover:bg-white/[0.03]',
                    ].join(' ')}
                  >
                    <Icon size={14} className="shrink-0" />
                    <span>{s.label}</span>
                  </NavLink>
                );
              })}

              <button
                onClick={logout}
                className="flex items-center gap-2 px-3 py-2 text-xs tracking-widest uppercase
                           text-slate-500 hover:text-cockpit-danger transition-colors mt-2
                           md:border-t md:border-cockpit-line/40 md:pt-3"
              >
                <LogOut size={14} className="shrink-0" />
                <span>Cerrar sesión</span>
              </button>
            </div>
          </motion.nav>

          {/* Contenido */}
          <motion.section
            key={location.pathname}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
          >
            <Outlet />
          </motion.section>
        </div>
      </main>
    </div>
  );
}
