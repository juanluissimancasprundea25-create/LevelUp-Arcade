import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Package, Euro, Clock, TrendingUp, ShoppingBag, ArrowRight,
} from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { EstadoBadge } from '../../components/admin/EstadoBadge.jsx';

/**
 * Dashboard de bienvenida del área "Mi cuenta".
 *
 *  - KPIs: total pedidos, pedidos pendientes, gastado total, último pedido
 *  - Lista de los 3 últimos pedidos como acceso rápido
 *  - Bloque "explora la tienda" con CTA
 */
export default function MiCuentaHome() {
  const pedidos = useFetch('/pedidos/mios');

  const stats = useMemo(() => {
    const lista = pedidos.data || [];
    const totalPedidos = lista.length;
    const pendientes = lista.filter(p => p.estado === 'PENDIENTE').length;
    const enCurso    = lista.filter(p =>
      p.estado === 'PAGADO' || p.estado === 'ENVIADO'
    ).length;
    const gastado = lista
      .filter(p => p.estado !== 'CANCELADO')
      .reduce((s, p) => s + Number(p.total || 0), 0);
    const ultimos = [...lista]
      .sort((a, b) => new Date(b.fechaPedido) - new Date(a.fechaPedido))
      .slice(0, 3);
    return { totalPedidos, pendientes, enCurso, gastado, ultimos };
  }, [pedidos.data]);

  return (
    <div className="space-y-5">
      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCliente
          label="Pedidos totales"
          value={pedidos.loading ? '—' : stats.totalPedidos}
          icon={Package}
          color="#a855f7"
        />
        <KpiCliente
          label="Pendientes pago"
          value={pedidos.loading ? '—' : stats.pendientes}
          icon={Clock}
          color="#fbbf24"
          highlight={stats.pendientes > 0}
        />
        <KpiCliente
          label="En camino"
          value={pedidos.loading ? '—' : stats.enCurso}
          icon={TrendingUp}
          color="#22d3ee"
        />
        <KpiCliente
          label="Gastado"
          value={pedidos.loading ? '—' : `${stats.gastado.toFixed(2)} €`}
          icon={Euro}
          color="#34d399"
        />
      </div>

      {/* Últimos pedidos */}
      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="hud-panel p-5"
      >
        <div className="flex items-center justify-between mb-4">
          <div>
            <p className="hud-label">// ÚLTIMOS PEDIDOS</p>
            <h2 className="font-display text-lg text-white tracking-wide mt-0.5">
              Tu actividad reciente
            </h2>
          </div>
          {stats.totalPedidos > 0 && (
            <Link
              to="pedidos"
              className="text-[10px] tracking-[0.3em] uppercase text-cockpit-cyan hover:text-white inline-flex items-center gap-1"
            >
              Ver todos
              <ArrowRight size={11} />
            </Link>
          )}
        </div>

        {pedidos.loading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-12 bg-white/5 animate-pulse rounded" />
            ))}
          </div>
        ) : stats.ultimos.length === 0 ? (
          <div className="text-center py-8">
            <ShoppingBag size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
            <p className="hud-label text-slate-500 mb-1">// AÚN NO HAS HECHO PEDIDOS</p>
            <p className="text-xs text-slate-600 mb-4">
              Explora el catálogo y haz tu primera compra
            </p>
            <Link to="/cockpit/tienda" className="hud-btn hud-btn--cyan !px-4 !py-2 inline-flex">
              Ir a la tienda
            </Link>
          </div>
        ) : (
          <div className="space-y-2">
            {stats.ultimos.map(p => (
              <UltimoPedidoFila key={p.id} pedido={p} />
            ))}
          </div>
        )}
      </motion.div>

      {/* CTA explora */}
      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.15 }}
        className="hud-panel p-5 relative overflow-hidden"
        style={{
          background:
            'linear-gradient(135deg, rgba(168,85,247,0.10), rgba(34,211,238,0.05))',
          borderColor: 'rgba(168,85,247,0.35)',
        }}
      >
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="hud-label text-cockpit-neon">// NUEVOS LANZAMIENTOS</p>
            <h3 className="font-display text-xl text-white mt-1">
              ¿Listo para tu próxima aventura?
            </h3>
            <p className="text-xs text-slate-300 mt-2 max-w-md">
              Explora el catálogo, busca por categorías y aprovecha el stock disponible.
            </p>
          </div>
          <Link to="/cockpit/tienda" className="hud-btn !px-5 !py-3">
            <ShoppingBag size={14} />
            Explorar catálogo
          </Link>
        </div>
      </motion.div>
    </div>
  );
}

/* ---------- KPI ---------- */
function KpiCliente({ label, value, icon: Icon, color, highlight }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      className="hud-panel p-3"
      style={highlight ? {
        borderColor: color + '99',
        boxShadow: `0 0 16px ${color}33`,
      } : {}}
    >
      <div className="flex items-center justify-between">
        <p className="hud-label text-[9px]">{label}</p>
        <Icon size={14} style={{ color }} />
      </div>
      <p className="font-display text-2xl mt-1" style={{ color }}>
        {value}
      </p>
    </motion.div>
  );
}

/* ---------- Fila pedido ---------- */
function UltimoPedidoFila({ pedido: p }) {
  const fecha = p.fechaPedido
    ? new Date(p.fechaPedido).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric'
      })
    : '—';
  return (
    <Link
      to={`pedidos/${p.id}`}
      className="flex items-center gap-3 p-3 border border-cockpit-line/40 hover:border-cockpit-neon/40
                 hover:bg-white/[0.02] transition-all"
    >
      <span className="font-mono text-cockpit-neon text-sm shrink-0">#{p.id}</span>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-slate-300 truncate">
          {p.lineas?.length || 0} {p.lineas?.length === 1 ? 'producto' : 'productos'}
        </p>
        <p className="hud-readout text-[10px] text-slate-500">{fecha}</p>
      </div>
      <span className="font-mono text-sm text-cockpit-cyan shrink-0">
        {Number(p.total || 0).toFixed(2)} €
      </span>
      <EstadoBadge estado={p.estado} size="sm" />
    </Link>
  );
}
