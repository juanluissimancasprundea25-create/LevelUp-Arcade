import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, User as UserIcon, LogIn, LogOut, Home } from 'lucide-react';
import { useAuth } from '../../lib/auth.jsx';
import { useCarrito } from '../../lib/carrito.js';

/**
 * Cabecera fija de la tienda pública. Muestra:
 *  - Logo (vuelve a /cockpit/tienda)
 *  - Acceso al carrito con badge de unidades
 *  - Si NO logueado: botón "Acceder"
 *  - Si logueado como CLIENTE: nombre + acceso a "Mi cuenta" + logout
 *  - Si logueado como ADMIN/EMPLEADO: acceso al panel cockpit
 */
export function HeaderTienda() {
  const { user, logout } = useAuth();
  const { totalUnidades } = useCarrito();
  const navigate = useNavigate();

  const rolNormalizado = String(user?.rol || '').replace(/^ROLE_/, '').toUpperCase();
  const esCliente = rolNormalizado === 'CLIENTE';
  const esStaff   = rolNormalizado === 'ADMIN' || rolNormalizado === 'EMPLEADO';

  function cerrarSesion() {
    logout();
    navigate('/cockpit/tienda');
  }

  return (
    <header
      className="sticky top-0 z-30 backdrop-blur-md border-b border-cockpit-line"
      style={{
        background: 'rgba(7, 8, 16, 0.7)',
      }}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex items-center gap-4">
        {/* Logo */}
        <Link to="/cockpit/tienda" className="flex items-center gap-2 group shrink-0">
          <div className="font-display tracking-[0.25em] text-sm">
            <span className="text-cockpit-cyan/70">// </span>
            <span className="text-white group-hover:text-cockpit-neon transition-colors">LEVELUP</span>
            <span className="text-cockpit-neon"> ARCADE</span>
          </div>
        </Link>

        {/* Navegación central (desktop) */}
        <nav className="hidden md:flex items-center gap-1 ml-4">
          <Link
            to="/cockpit/tienda"
            className="px-3 py-1.5 text-xs tracking-widest uppercase text-slate-400 hover:text-white transition-colors"
          >
            Catálogo
          </Link>
          {esCliente && (
            <Link
              to="/cockpit/mi-cuenta"
              className="px-3 py-1.5 text-xs tracking-widest uppercase text-slate-400 hover:text-white transition-colors"
            >
              Mi cuenta
            </Link>
          )}
        </nav>

        <div className="flex-1" />

        {/* Carrito */}
        <Link
          to="/cockpit/carrito"
          className="relative p-2 hover:text-cockpit-neon transition-colors"
          title="Carrito"
        >
          <ShoppingCart size={18} className="text-cockpit-cyan" />
          {totalUnidades > 0 && (
            <span
              className="absolute -top-1 -right-1 min-w-[16px] h-4 px-1 text-[9px] font-mono font-bold grid place-items-center text-cockpit-bg"
              style={{
                background: '#a855f7',
                boxShadow: '0 0 8px rgba(168,85,247,0.7)',
              }}
            >
              {totalUnidades > 99 ? '99+' : totalUnidades}
            </span>
          )}
        </Link>

        {/* User actions */}
        {!user ? (
          <Link
            to="/cockpit/login"
            className="hud-btn hud-btn--cyan !px-3 !py-1.5 !text-[10px]"
          >
            <LogIn size={12} />
            <span className="hidden sm:inline">Acceder</span>
          </Link>
        ) : esStaff ? (
          <Link
            to="/cockpit/admin"
            className="hud-btn !px-3 !py-1.5 !text-[10px]"
          >
            <Home size={12} />
            <span className="hidden sm:inline">Panel</span>
          </Link>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/cockpit/mi-cuenta"
              className="hidden sm:flex items-center gap-2 px-3 py-1.5 border border-cockpit-line hover:border-cockpit-neon/60 transition-colors"
              title="Mi cuenta"
            >
              <UserIcon size={12} className="text-cockpit-neon" />
              <span className="text-xs text-white">{user.nombre || 'Mi cuenta'}</span>
            </Link>
            <button
              onClick={cerrarSesion}
              className="p-2 text-slate-400 hover:text-cockpit-danger transition-colors"
              title="Cerrar sesión"
            >
              <LogOut size={14} />
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
