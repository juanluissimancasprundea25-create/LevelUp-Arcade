import { motion } from 'framer-motion';
import { AlertTriangle, Package } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * Lista de productos en alerta de stock bajo. Espera `productos` ya
 * filtrados (los que tienen bajoStock=true). Muestra top 6.
 *
 * Cada item enlaza a `/cockpit/admin/productos?stockId={id}` para que
 * el listado de productos abra automaticamente el modal de ajuste de
 * stock de ese producto.
 */
export function StockAlertList({ productos, loading }) {
  const items = productos ? productos.slice(0, 6) : [];

  return (
    <motion.div
      initial={{ opacity: 0, x: 14 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.5, delay: 0.3 }}
      className="hud-panel p-5"
      style={{
        boxShadow:
          productos && productos.length > 0
            ? '0 0 0 1px rgba(251,191,36,0.3) inset, 0 0 28px rgba(251,191,36,0.18)'
            : undefined,
      }}
    >
      <div className="flex items-center justify-between mb-3">
        <p className="hud-label flex items-center gap-2">
          {productos && productos.length > 0 && (
            <AlertTriangle size={12} className="text-cockpit-amber animate-pulse" />
          )}
          // ALERTA DE STOCK
        </p>
        <span className="hud-readout">
          {loading ? '...' : `${productos?.length || 0} items`}
        </span>
      </div>

      {loading ? (
        <div className="space-y-2">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-10 bg-white/5 animate-pulse rounded" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <div className="py-8 text-center text-slate-500 text-xs tracking-widest">
          <Package size={28} className="mx-auto mb-2 opacity-30" />
          STOCK NOMINAL
        </div>
      ) : (
        <ul className="space-y-1.5">
          {items.map((p) => (
            <li key={p.id}>
              <Link
                to={`/cockpit/admin/productos?stockId=${p.id}`}
                className="flex items-center gap-3 px-3 py-2 border-l-2 border-cockpit-amber/60
                           bg-cockpit-amber/5 hover:bg-cockpit-amber/15 transition-colors group"
                title="Ajustar stock de este producto"
              >
                <span className="flex-1 min-w-0">
                  <span className="block text-xs text-white truncate">{p.nombre}</span>
                  <span className="block hud-readout opacity-70 truncate">{p.sku}</span>
                </span>
                <span className="text-right shrink-0">
                  <span className="block font-mono text-cockpit-amber text-sm">
                    {p.stock}
                    <span className="text-slate-500 text-[10px]"> / {p.stockMinimo}</span>
                  </span>
                  <span className="block hud-label text-[8px]">stock / min</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {productos && productos.length > 6 && (
        <Link
          to="/cockpit/admin/productos?bajoStock=true"
          className="block mt-3 text-center text-[10px] tracking-[0.3em] uppercase
                     text-cockpit-amber hover:text-white transition-colors"
        >
          Ver {productos.length - 6} mas
        </Link>
      )}
    </motion.div>
  );
}
