import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, Search, X, Eye, RotateCcw } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { EstadoBadge } from '../../components/admin/EstadoBadge.jsx';
import { DevolucionDetalleModal } from '../../components/admin/DevolucionDetalleModal.jsx';

const ESTADOS = ['SOLICITADA', 'APROBADA', 'RECHAZADA', 'COMPLETADA'];

export default function Devoluciones() {
  const [busqueda, setBusqueda] = useState('');
  const [estadoSel, setEstadoSel] = useState('');
  const [verDevolucion, setVerDevolucion] = useState(null);

  // Endpoint devuelve Page<>. Pedimos página grande.
  const devoluciones = useFetch('/devoluciones', {
    params: { size: 500, sort: 'fechaSolicitud,desc' },
  });

  const listado = devoluciones.data?.content || devoluciones.data || [];

  const conteos = useMemo(() => {
    return listado.reduce((acc, d) => {
      acc[d.estado] = (acc[d.estado] || 0) + 1;
      return acc;
    }, {});
  }, [listado]);

  const filtradas = useMemo(() => {
    let arr = listado;
    if (estadoSel) arr = arr.filter(d => d.estado === estadoSel);
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(d =>
        String(d.id).includes(q) ||
        String(d.pedidoId).includes(q) ||
        String(d.clienteId).includes(q) ||
        (d.motivo || '').toLowerCase().includes(q)
      );
    }
    return arr;
  }, [listado, busqueda, estadoSel]);

  const importeTotal = useMemo(() => {
    return filtradas
      .filter(d => d.estado === 'APROBADA' || d.estado === 'COMPLETADA')
      .reduce((s, d) => s + Number(d.importeDevuelto || 0), 0);
  }, [filtradas]);

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
            Devoluciones
          </h1>
          <p className="hud-readout mt-1">
            {devoluciones.loading
              ? 'Sincronizando…'
              : `${filtradas.length} solicitudes · ${importeTotal.toFixed(2)} € aprobados/completados`}
          </p>
        </div>
        <button
          onClick={() => devoluciones.refresh()}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
          disabled={devoluciones.loading}
        >
          <RefreshCw size={14} className={devoluciones.loading ? 'animate-spin' : ''} />
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
            Todas · {listado.length}
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
            placeholder="Buscar por ID, pedido, cliente o motivo…"
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
                <th className="hud-label text-left p-3 hidden md:table-cell">Pedido</th>
                <th className="hud-label text-left p-3">Motivo</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Fecha</th>
                <th className="hud-label text-right p-3">Importe</th>
                <th className="hud-label text-center p-3">Estado</th>
                <th className="hud-label text-right p-3 w-16"></th>
              </tr>
            </thead>
            <tbody>
              {devoluciones.loading ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={7}>
                      <div className="h-10 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtradas.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-10 text-center">
                    <RotateCcw size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {listado.length === 0
                        ? '// NO HAY DEVOLUCIONES SOLICITADAS'
                        : '// NINGÚN RESULTADO'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtradas.map((d) => (
                  <DevolucionRow
                    key={d.id}
                    devolucion={d}
                    onVer={() => setVerDevolucion(d)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      <DevolucionDetalleModal
        open={!!verDevolucion}
        devolucion={verDevolucion}
        onClose={() => setVerDevolucion(null)}
        onChanged={(actualizada) => {
          setVerDevolucion(actualizada);
          devoluciones.refresh();
        }}
      />
    </div>
  );
}

function DevolucionRow({ devolucion: d, onVer }) {
  const fecha = d.fechaSolicitud
    ? new Date(d.fechaSolicitud).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric'
      })
    : '—';

  return (
    <tr
      onClick={onVer}
      className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors cursor-pointer"
    >
      <td className="p-3 font-mono text-cockpit-neon">#{d.id}</td>
      <td className="p-3 hidden md:table-cell">
        <span className="font-mono text-cockpit-cyan">#{d.pedidoId}</span>
      </td>
      <td className="p-3">
        <div className="text-sm text-slate-200 truncate max-w-xs" title={d.motivo}>
          {d.motivo || <span className="text-slate-600">sin motivo</span>}
        </div>
      </td>
      <td className="p-3 hidden md:table-cell text-xs text-slate-400 hud-readout">{fecha}</td>
      <td className="p-3 text-right font-mono text-cockpit-ok">
        {Number(d.importeDevuelto || 0).toFixed(2)} €
      </td>
      <td className="p-3 text-center">
        <EstadoBadge estado={d.estado} size="sm" />
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
