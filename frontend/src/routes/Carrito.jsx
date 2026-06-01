import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ShoppingCart, Minus, Plus, X, ArrowLeft, Trash2, ImageOff, CreditCard,
} from 'lucide-react';
import { useCarrito } from '../lib/carrito.js';
import { useAuth } from '../lib/auth.jsx';
import { useToast } from '../components/admin/Toast.jsx';
import { HeaderTienda } from '../components/tienda/HeaderTienda.jsx';

/**
 * Carrito de la compra. URL: /cockpit/carrito
 *
 * Diferencias respecto a la versión de Fase 7a:
 *  - "Tramitar pedido" navega ahora a /cockpit/checkout (real)
 *  - Si el usuario no está logueado, redirige al login con ?vuelve=/cockpit/checkout
 *    para que después del login lo lleve directo a tramitar.
 */
export default function Carrito() {
  const { items, totalEuros, totalUnidades, cambiarCantidad, quitar, vaciar } = useCarrito();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const rolNormalizado = String(user?.rol || '').replace(/^ROLE_/, '').toUpperCase();
  const esCliente = rolNormalizado === 'CLIENTE';

  function tramitar() {
    if (items.length === 0) return;
    if (!user) {
      toast.info('Inicia sesión o regístrate para tramitar el pedido');
      navigate('/cockpit/login?vuelve=/cockpit/checkout');
      return;
    }
    if (!esCliente) {
      toast.warn('Solo los clientes pueden tramitar pedidos. Cierra sesión y entra con una cuenta de cliente.');
      return;
    }
    navigate('/cockpit/checkout');
  }

  function vaciarCarrito() {
    if (!confirm('¿Vaciar el carrito por completo?')) return;
    vaciar();
    toast.info('Carrito vaciado');
  }

  return (
    <div className="min-h-screen flex flex-col">
      <HeaderTienda />

      <main className="flex-1 max-w-5xl mx-auto w-full px-4 sm:px-6 py-6">
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
            Seguir comprando
          </Link>
        </motion.div>

        <motion.header
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-5"
        >
          <p className="hud-label text-cockpit-cyan/70">// CARRITO</p>
          <h1 className="font-display text-3xl tracking-wider text-white mt-1 flex items-center gap-3">
            <ShoppingCart className="text-cockpit-neon" size={26} />
            Tu compra
          </h1>
          <p className="hud-readout mt-1">
            {totalUnidades} {totalUnidades === 1 ? 'artículo' : 'artículos'} en el carrito
          </p>
        </motion.header>

        {items.length === 0 ? (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="hud-panel p-10 text-center"
          >
            <ShoppingCart size={48} className="mx-auto mb-3 opacity-30 text-slate-500" />
            <p className="hud-label text-slate-500 mb-1">// CARRITO VACÍO</p>
            <p className="text-xs text-slate-600 mb-5">
              Añade productos del catálogo para empezar tu compra.
            </p>
            <Link to="/cockpit/tienda" className="hud-btn hud-btn--cyan !px-4 !py-2 inline-flex">
              Ir al catálogo
            </Link>
          </motion.div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
            {/* Lista */}
            <motion.div
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.4 }}
              className="hud-panel overflow-hidden"
            >
              <AnimatePresence>
                {items.map((it) => (
                  <CarritoItem
                    key={it.productoId}
                    item={it}
                    onCambiar={(n) => cambiarCantidad(it.productoId, n)}
                    onQuitar={() => quitar(it.productoId)}
                  />
                ))}
              </AnimatePresence>

              <div className="p-3 border-t border-cockpit-line flex justify-between items-center">
                <button
                  onClick={vaciarCarrito}
                  className="text-[10px] tracking-[0.3em] uppercase text-slate-500 hover:text-cockpit-danger transition-colors flex items-center gap-1.5"
                >
                  <Trash2 size={10} />
                  Vaciar carrito
                </button>
                <Link
                  to="/cockpit/tienda"
                  className="text-[10px] tracking-[0.3em] uppercase text-cockpit-cyan hover:text-white transition-colors"
                >
                  + Añadir más productos
                </Link>
              </div>
            </motion.div>

            {/* Resumen */}
            <motion.aside
              initial={{ opacity: 0, x: 10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.4, delay: 0.1 }}
              className="hud-panel p-5 h-fit sticky top-20"
            >
              <p className="hud-label mb-4">// RESUMEN DEL PEDIDO</p>

              <div className="space-y-2 text-sm pb-4 border-b border-cockpit-line/40">
                <div className="flex justify-between text-slate-400">
                  <span>Subtotal</span>
                  <span className="font-mono">{totalEuros.toFixed(2)} €</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Envío</span>
                  <span className="font-mono">se calcula al pagar</span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>IVA</span>
                  <span className="font-mono text-[10px] italic">incluido</span>
                </div>
              </div>

              <div className="flex justify-between items-end mt-4 mb-5">
                <span className="hud-label">// TOTAL</span>
                <span className="font-display text-2xl text-cockpit-ok">
                  {totalEuros.toFixed(2)} €
                </span>
              </div>

              <button
                onClick={tramitar}
                className="hud-btn !px-4 !py-3 w-full"
              >
                <CreditCard size={14} />
                Tramitar pedido
              </button>

              {!user && (
                <p className="hud-readout text-[10px] text-slate-500 mt-3 text-center">
                  Necesitas <Link to="/cockpit/login" className="text-cockpit-cyan hover:underline">iniciar sesión</Link>
                  {' '}o{' '}
                  <Link to="/cockpit/registro" className="text-cockpit-neon hover:underline">registrarte</Link>
                  {' '}para completar la compra.
                </p>
              )}
            </motion.aside>
          </div>
        )}
      </main>
    </div>
  );
}

/* ---------- Item del carrito ---------- */
function CarritoItem({ item, onCambiar, onQuitar }) {
  const [imgError, setImgErrorLocal] = useState(false);
  const subtotal = (item.precio || 0) * (item.cantidad || 0);

  return (
    <motion.div
      initial={{ opacity: 0, height: 0 }}
      animate={{ opacity: 1, height: 'auto' }}
      exit={{ opacity: 0, height: 0, marginBottom: 0 }}
      transition={{ duration: 0.25 }}
      className="border-b border-cockpit-line/30 p-3 flex items-center gap-3"
    >
      <Link
        to={`/cockpit/tienda/producto/${item.productoId}`}
        className="w-16 h-16 shrink-0 border border-cockpit-line/40 bg-black/40 grid place-items-center overflow-hidden"
      >
        {item.imagenUrl && !imgError ? (
          <img
            src={item.imagenUrl}
            alt={item.nombre}
            onError={() => setImgErrorLocal(true)}
            className="w-full h-full object-cover"
          />
        ) : (
          <ImageOff size={16} className="text-slate-700" />
        )}
      </Link>

      <div className="flex-1 min-w-0">
        <Link
          to={`/cockpit/tienda/producto/${item.productoId}`}
          className="text-sm text-white hover:text-cockpit-neon transition-colors truncate block"
        >
          {item.nombre}
        </Link>
        <p className="hud-readout text-slate-500 text-[10px]">{item.sku}</p>
        <p className="text-xs text-slate-400 mt-1">
          {Number(item.precio).toFixed(2)} € / unidad
        </p>
      </div>

      <div className="flex items-center border border-cockpit-line shrink-0">
        <button
          onClick={() => onCambiar(item.cantidad - 1)}
          className="px-2 py-1.5 text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
        >
          <Minus size={12} />
        </button>
        <span className="w-8 text-center font-mono text-white text-sm">
          {item.cantidad}
        </span>
        <button
          onClick={() => onCambiar(item.cantidad + 1)}
          className="px-2 py-1.5 text-cockpit-cyan hover:text-white hover:bg-white/5 transition-colors"
        >
          <Plus size={12} />
        </button>
      </div>

      <div className="hidden sm:block w-24 text-right shrink-0">
        <span className="font-mono text-cockpit-cyan">{subtotal.toFixed(2)} €</span>
      </div>

      <button
        onClick={onQuitar}
        className="p-2 text-slate-500 hover:text-cockpit-danger hover:bg-cockpit-danger/10 transition-colors shrink-0"
        title="Quitar"
      >
        <X size={14} />
      </button>
    </motion.div>
  );
}
