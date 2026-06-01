import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, Search, X, Eye, ShoppingCart } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { useAuth } from '../../lib/auth.jsx';
import { EstadoBadge } from '../../components/admin/EstadoBadge.jsx';
import { PedidoDetalleModal } from '../../components/admin/PedidoDetalleModal.jsx';

const ESTADOS = ['PENDIENTE', 'PAGADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO'];

export default function Pedidos() {
  const { user } = useAuth();
  const esAdmin = user?.rol === 'ADMIN';

  const [busqueda, setBusqueda] = useState('');
  const [estadoSel, setEstadoSel] = useState('');
  const [verPedido, setVerPedido] = useState(null);

  const pedidos = useFetch('/pedidos');

  // Conteos por estado para los chips
  const conteos = useMemo(() => {
    if (!pedidos.data) return {};
    return pedidos.data.reduce((acc, p) => {
      acc[p.estado] = (acc[p.estado] || 0) + 1;
      return acc;
    }, {});
  }, [pedidos.data]);

  const filtrados = useMemo(() => {
    if (!pedidos.data) return [];
    let arr = pedidos.data;
    if (estadoSel) arr = arr.filter(p => p.estado === estadoSel);
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(p =>
        String(p.id).includes(q) ||
        (p.clienteEmail || '').toLowerCase().includes(q) ||
        (p.clienteNombreCompleto || '').toLowerCase().includes(q)
      );
    }
    return arr.sort((a, b) => new Date(b.fechaPedido) - new Date(a.fechaPedido));
  }, [pedidos.data, busqueda, estadoSel]);

  const total = useMemo(() => {
    return filtrados.reduce((s, p) => s + Number(p.total || 0), 0);
  }, [filtrados]);

  return (
    <div className="pb-8">
      <motion.header
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex items-end justify-between mb-5 pt-2 gap-4 flex-wrap"
      >
        <div>
          <p className="hud-label text-cockpit-cyan/70">// MÓDULO</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Pedidos
          </h1>
          <p className="hud-readout mt-1">
            {pedidos.loading
              ? 'Sincronizando…'
              : `${filtrados.length} pedidos · ${total.toFixed(2)} € en total`}
          </p>
        </div>
        <button
          onClick={() => pedidos.refresh()}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
          disabled={pedidos.loading}
        >
          <RefreshCw size={14} className={pedidos.loading ? 'animate-spin' : ''} />
          <span className="hidden sm:inline">Refrescar</span>
        </button>
      </motion.header>

      {/* Chips de estado */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.05 }}
        className="hud-panel p-3 mb-4"
      >
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
              className={[
                'transition-all',
                estadoSel === e ? '' : 'opacity-60 hover:opacity-100',
              ].join(' ')}
            >
              <EstadoBadge estado={e} size="md" dot={false} />
              <span className="ml-1 text-[10px] text-slate-500 font-mono">
                ({conteos[e] || 0})
              </span>
            </button>
          ))}
        </div>
      </motion.div>

      {/* Buscador */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="hud-panel p-4 mb-5"
      >
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por ID, cliente o email…"
            className="hud-input !pl-9"
          />
          {busqueda && (
            <button
              onClick={() => setBusqueda('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
              aria-label="Limpiar"
            >
              <X size={14} />
            </button>
          )}
        </div>
      </motion.div>

      {/* Tabla */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.15 }}
        className="hud-panel overflow-hidden"
      >
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-cockpit-line">
                <th className="hud-label text-left p-3 w-20">#ID</th>
                <th className="hud-label text-left p-3">Cliente</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Fecha</th>
                <th className="hud-label text-center p-3 hidden lg:table-cell">Líneas</th>
                <th className="hud-label text-right p-3">Total</th>
                <th className="hud-label text-center p-3">Estado</th>
                <th className="hud-label text-right p-3 w-16"></th>
              </tr>
            </thead>
            <tbody>
              {pedidos.loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={7}>
                      <div className="h-10 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtrados.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-10 text-center">
                    <ShoppingCart size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {pedidos.data?.length === 0
                        ? '// NO HAY PEDIDOS EN EL SISTEMA'
                        : '// NINGÚN RESULTADO CON ESOS FILTROS'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtrados.map((p) => (
                  <PedidoRow
                    key={p.id}
                    pedido={p}
                    onVer={() => setVerPedido(p)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      <PedidoDetalleModal
        open={!!verPedido}
        pedido={verPedido}
        esAdmin={esAdmin}
        onClose={() => setVerPedido(null)}
        onChanged={(actualizado) => {
          setVerPedido(actualizado);
          pedidos.refresh();
        }}
        onFacturaEmitida={() => {
          // No cerramos por si quiere seguir con el pedido
        }}
      />
    </div>
  );
}

function PedidoRow({ pedido: p, onVer }) {
  const fecha = p.fechaPedido
    ? new Date(p.fechaPedido).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric'
      })
    : '—';

  return (
    <tr
      onClick={onVer}
      className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors cursor-pointer"
    >
      <td className="p-3 font-mono text-cockpit-neon">#{p.id}</td>
      <td className="p-3">
        <div className="text-sm text-white truncate">{p.clienteNombreCompleto || '—'}</div>
        <div className="hud-readout text-slate-400 truncate text-xs">{p.clienteEmail}</div>
      </td>
      <td className="p-3 hidden md:table-cell text-xs text-slate-400 hud-readout">{fecha}</td>
      <td className="p-3 hidden lg:table-cell text-center font-mono text-slate-300">
        {p.lineas?.length || 0}
      </td>
      <td className="p-3 text-right font-mono text-cockpit-cyan">
        {Number(p.total || 0).toFixed(2)} €
      </td>
      <td className="p-3 text-center">
        <EstadoBadge estado={p.estado} size="sm" />
      </td>
      <td className="p-3 text-right">
        <button
          onClick={(e) => { e.stopPropagation(); onVer(); }}
          className="p-1.5 text-cockpit-neon hover:text-white hover:bg-cockpit-neon/10 transition-colors"
          title="Ver detalle"
        >
          <Eye size={15} />
        </button>
      </td>
    </tr>
  );
}
