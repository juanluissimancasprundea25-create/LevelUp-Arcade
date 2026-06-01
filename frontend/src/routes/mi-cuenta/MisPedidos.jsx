import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { RefreshCw, Search, X, ShoppingCart, Eye } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { EstadoBadge } from '../../components/admin/EstadoBadge.jsx';

const ESTADOS = ['PENDIENTE', 'PAGADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO'];

export default function MisPedidos() {
  const [busqueda, setBusqueda] = useState('');
  const [estadoSel, setEstadoSel] = useState('');

  const pedidos = useFetch('/pedidos/mios');

  const conteos = useMemo(() => {
    return (pedidos.data || []).reduce((acc, p) => {
      acc[p.estado] = (acc[p.estado] || 0) + 1;
      return acc;
    }, {});
  }, [pedidos.data]);

  const filtrados = useMemo(() => {
    let arr = pedidos.data || [];
    if (estadoSel) arr = arr.filter(p => p.estado === estadoSel);
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(p =>
        String(p.id).includes(q) ||
        (p.lineas || []).some(l =>
          (l.productoNombre || '').toLowerCase().includes(q)
        )
      );
    }
    return [...arr].sort((a, b) => new Date(b.fechaPedido) - new Date(a.fechaPedido));
  }, [pedidos.data, busqueda, estadoSel]);

  return (
    <div className="space-y-5">
      <header className="flex items-end justify-between gap-3 flex-wrap">
        <div>
          <p className="hud-label text-cockpit-cyan/70">// HISTORIAL</p>
          <h2 className="font-display text-xl tracking-wide text-white mt-0.5">
            Mis pedidos
          </h2>
          <p className="hud-readout mt-1">
            {pedidos.loading ? 'Sincronizando…' : `${filtrados.length} de ${pedidos.data?.length || 0}`}
          </p>
        </div>
        <button
          onClick={() => pedidos.refresh()}
          disabled={pedidos.loading}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
        >
          <RefreshCw size={14} className={pedidos.loading ? 'animate-spin' : ''} />
          <span className="hidden sm:inline">Refrescar</span>
        </button>
      </header>

      {/* Chips de estado */}
      <div className="hud-panel p-3">
        <div className="flex items-center gap-2 flex-wrap">
          <button
            onClick={() => setEstadoSel('')}
            className={[
              'px-3 py-1 text-[10px] tracking-widest uppercase font-display transition-all',
              !estadoSel
                ? 'border border-cockpit-neon text-white bg-cockpit-neon/15'
                : 'border border-cockpit-line text-slate-400 hover:text-white',
            ].join(' ')}
          >
            Todos · {pedidos.data?.length || 0}
          </button>
          {ESTADOS.map((e) => (
            <button
              key={e}
              onClick={() => setEstadoSel(estadoSel === e ? '' : e)}
              className={estadoSel === e ? '' : 'opacity-60 hover:opacity-100 transition-opacity'}
            >
              <EstadoBadge estado={e} size="md" dot={false} />
              <span className="ml-1 text-[10px] text-slate-500 font-mono">
                ({conteos[e] || 0})
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Buscador */}
      <div className="hud-panel p-4">
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nº pedido o producto…"
            className="hud-input !pl-9"
          />
          {busqueda && (
            <button
              onClick={() => setBusqueda('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
            >
              <X size={14} />
            </button>
          )}
        </div>
      </div>

      {/* Tabla */}
      <div className="hud-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-cockpit-line">
                <th className="hud-label text-left p-3 w-20">#</th>
                <th className="hud-label text-left p-3">Productos</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Fecha</th>
                <th className="hud-label text-right p-3">Total</th>
                <th className="hud-label text-center p-3">Estado</th>
                <th className="hud-label text-right p-3 w-16"></th>
              </tr>
            </thead>
            <tbody>
              {pedidos.loading ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={6}>
                      <div className="h-10 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtrados.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-10 text-center">
                    <ShoppingCart size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500 mb-1">
                      {pedidos.data?.length === 0
                        ? '// TODAVÍA NO HAS HECHO PEDIDOS'
                        : '// NINGÚN PEDIDO COINCIDE'}
                    </p>
                    {pedidos.data?.length === 0 && (
                      <Link to="/cockpit/tienda" className="text-xs text-cockpit-cyan hover:text-white">
                        Ir al catálogo →
                      </Link>
                    )}
                  </td>
                </tr>
              ) : (
                filtrados.map((p) => (
                  <PedidoMioRow key={p.id} pedido={p} />
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function PedidoMioRow({ pedido: p }) {
  const fecha = p.fechaPedido
    ? new Date(p.fechaPedido).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric'
      })
    : '—';
  const productosResumen = (p.lineas || []).slice(0, 2)
    .map(l => `${l.cantidad} × ${l.productoNombre}`)
    .join(', ');
  const mas = Math.max(0, (p.lineas?.length || 0) - 2);

  return (
    <motion.tr
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors"
    >
      <td className="p-3 font-mono text-cockpit-neon">#{p.id}</td>
      <td className="p-3">
        <div className="text-sm text-white truncate max-w-xs">
          {productosResumen}
          {mas > 0 && <span className="text-slate-500"> +{mas} más</span>}
        </div>
      </td>
      <td className="p-3 hidden md:table-cell text-xs text-slate-400 hud-readout">{fecha}</td>
      <td className="p-3 text-right font-mono text-cockpit-cyan">
        {Number(p.total || 0).toFixed(2)} €
      </td>
      <td className="p-3 text-center">
        <EstadoBadge estado={p.estado} size="sm" />
      </td>
      <td className="p-3 text-right">
        <Link
          to={`${p.id}`}
          className="inline-block p-1.5 text-cockpit-neon hover:text-white hover:bg-cockpit-neon/10 transition-colors"
          title="Ver detalle"
        >
          <Eye size={15} />
        </Link>
      </td>
    </motion.tr>
  );
}
