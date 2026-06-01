import { NavLink, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  LayoutDashboard, Package, Tags, Users, Truck, ShoppingCart,
  FileText, RotateCcw, MessageSquare, Activity, Bot, Power
} from 'lucide-react';
import { useAuth } from '../../lib/auth.jsx';

/**
 * Sidebar lateral con los módulos del sistema. Cada item es una
 * "consola" iluminable. NavLink resalta el activo. Acceso filtrado
 * por rol (CLIENTE no ve Auditoría ni Devoluciones admin, etc.).
 */
export function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Definición de módulos. `roles` controla quién puede verlos.
  const modulos = [
    { to: '/cockpit/admin',             icon: LayoutDashboard, label: 'Puente',       roles: ['ADMIN','EMPLEADO'], end: true },
    { to: '/cockpit/admin/productos',   icon: Package,        label: 'Productos',    roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/categorias',  icon: Tags,           label: 'Categorías',   roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/clientes',    icon: Users,          label: 'Clientes',     roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/proveedores', icon: Truck,          label: 'Proveedores',  roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/pedidos',     icon: ShoppingCart,   label: 'Pedidos',      roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/facturas',    icon: FileText,       label: 'Facturas',     roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/devoluciones',icon: RotateCcw,      label: 'Devoluciones', roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/chat',        icon: MessageSquare,  label: 'Chat',         roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/ia',          icon: Bot,            label: 'IA',           roles: ['ADMIN','EMPLEADO'] },
    { to: '/cockpit/admin/auditoria',   icon: Activity,       label: 'Auditoría',    roles: ['ADMIN'] },
  ];

  const visibles = modulos.filter(m => m.roles.includes(user?.rol));

  function handleLogout() {
    logout();
    navigate('/cockpit', { replace: true });
  }

  return (
    <motion.aside
      initial={{ x: -60, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
      className="hud-panel fixed top-4 bottom-4 left-4 z-20 w-[68px] xl:w-[230px]
                 flex flex-col py-5 px-2 xl:px-4"
    >
      {/* Logo / título */}
      <div className="px-2 mb-6">
        <div className="hud-label text-cockpit-cyan/80">// SISTEMA</div>
        <div className="hidden xl:block font-display text-base tracking-[0.3em] text-white mt-1">
          LEVELUP
        </div>
      </div>

      {/* Identidad piloto */}
      <div className="px-2 mb-5 pb-4 border-b border-cockpit-line">
        <div className="hud-label">// PILOTO</div>
        <div className="mt-1 text-xs font-mono text-cockpit-neon truncate" title={user?.email}>
          {user?.nombre || user?.email?.split('@')[0]}
        </div>
        <div className="text-[10px] tracking-widest text-cockpit-cyan/70 uppercase mt-0.5">
          {user?.rol}
        </div>
      </div>

      {/* Módulos */}
      <nav className="flex-1 overflow-y-auto space-y-1 px-1">
        {visibles.map((m) => (
          <NavLink
            key={m.to}
            to={m.to}
            end={m.end}
            className={({ isActive }) =>
              [
                'group flex items-center gap-3 px-3 py-2.5',
                'text-[11px] tracking-[0.25em] uppercase font-display',
                'border-l-2 transition-all duration-200',
                isActive
                  ? 'border-cockpit-neon bg-cockpit-neon/15 text-white shadow-[inset_0_0_18px_rgba(168,85,247,0.2)]'
                  : 'border-transparent text-slate-400 hover:text-cockpit-cyan hover:bg-white/5 hover:border-cockpit-cyan/50',
              ].join(' ')
            }
          >
            <m.icon size={16} className="shrink-0" />
            <span className="hidden xl:inline truncate">{m.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Logout */}
      <button
        onClick={handleLogout}
        className="mt-3 mx-1 flex items-center gap-3 px-3 py-2.5
                   text-[11px] tracking-[0.25em] uppercase font-display
                   text-slate-500 hover:text-cockpit-danger
                   border-l-2 border-transparent hover:border-cockpit-danger/60
                   transition-all duration-200"
      >
        <Power size={16} className="shrink-0" />
        <span className="hidden xl:inline">Salir</span>
      </button>
    </motion.aside>
  );
}
