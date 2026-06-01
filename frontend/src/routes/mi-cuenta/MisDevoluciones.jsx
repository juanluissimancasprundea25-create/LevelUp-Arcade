import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Plus, RefreshCw, Search, X, RotateCcw, Eye } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { EstadoBadge } from '../../components/admin/EstadoBadge.jsx';
import { NuevaDevolucionModal } from '../../components/mi-cuenta/NuevaDevolucionModal.jsx';
import { MiDevolucionDetalleModal } from '../../components/mi-cuenta/MiDevolucionDetalleModal.jsx';

const ESTADOS = ['SOLICITADA', 'APROBADA', 'RECHAZADA', 'COMPLETADA'];

/**
 * Mis devoluciones — URL: /cockpit/mi-cuenta/devoluciones
 *
 *  - Botón "Solicitar devolución" abre modal con flujo de 2 pasos
 *  - Lista paginada de devoluciones del cliente (GET /devoluciones/mias)
 *  - Click en una fila abre detalle (solo lectura)
 *
 * Soporta query param ?pedido=N para abrir directo el modal de nueva
 * devolución con ese pedido preseleccionado (link desde detalle pedido).
 */
export default function MisDevoluciones() {
  const [busqueda, setBusqueda] = useState('');
  const [estadoSel, setEstadoSel] = useState('');
  const [mostrarNueva, setMostrarNueva] = useState(false);
  const [verDevolucion, setVerDevolucion] = useState(null);

  const [params, setParams] = useSearchParams();
  const pedidoPreseleccionado = params.get('pedido');

  // Si llegamos con ?pedido=N, abrimos el modal y quitamos el param de la URL
  useEffect(() => {
    if (pedidoPreseleccionado) {
      setMostrarNueva(true);
    }
  }, [pedidoPreseleccionado]);

  const devoluciones = useFetch('/devoluciones/mias', {
    params: { size: 200, sort: 'fechaSolicitud,desc' },
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
        (d.motivo || '').toLowerCase().includes(q)
      );
    }
    return arr;
  }, [listado, busqueda, estadoSel]);

  function cerrarNueva() {
    setMostrarNueva(false);
    if (pedidoPreseleccionado) {
      // Limpiar el query param sin recargar
      params.delete('pedido');
      setParams(params, { replace: true });
    }
  }

  return (
    <div className="space-y-5">
      <header className="flex items-end justify-between gap-3 flex-wrap">
        <div>
          <p className="hud-label text-cockpit-cyan/70">// DEVOLUCIONES</p>
          <h2 className="font-display text-xl tracking-wide text-white mt-0.5">
            Mis devoluciones
          </h2>
          <p className="hud-readout mt-1">
            {devoluciones.loading
              ? 'Sincronizando…'
              : `${filtradas.length} de ${listado.length} solicitudes`}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => devoluciones.refresh()}
            disabled={devoluciones.loading}
            className="hud-btn hud-btn--cyan !px-3 !py-2"
          >
            <RefreshCw size={14} className={devoluciones.loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refrescar</span>
          </button>
          <button
            onClick={() => setMostrarNueva(true)}
            className="hud-btn !px-4 !py-2"
          >
            <Plus size={14} />
            Solicitar devolución
          </button>
        </div>
      </header>

      {/* Aviso */}
      <div className="hud-panel p-3 !bg-black/30 flex gap-2">
        <RotateCcw size={14} className="text-cockpit-cyan shrink-0 mt-0.5" />
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Tienes 14 días desde la entrega del pedido para solicitar una devolución.
          Tras enviar la solicitud, un administrador la revisará y te notificará el resultado.
        </p>
      </div>

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
      </div>

      {/* Buscador */}
      <div className="hud-panel p-4">
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nº, pedido o motivo…"
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
                <th className="hud-label text-left p-3 w-16">#</th>
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
                Array.from({ length: 3 }).map((_, i) => (
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
                    <p className="hud-label text-slate-500 mb-1">
                      {listado.length === 0
                        ? '// NO HAS SOLICITADO NINGUNA DEVOLUCIÓN'
                        : '// NINGUNA COINCIDE CON ESOS FILTROS'}
                    </p>
                    {listado.length === 0 && (
                      <p className="text-xs text-slate-600 mt-2">
                        Si tienes algún problema con un pedido entregado, solicita una devolución arriba.
                      </p>
                    )}
                  </td>
                </tr>
              ) : (
                filtradas.map(d => (
                  <DevolucionMiaRow
                    key={d.id}
                    devolucion={d}
                    onVer={() => setVerDevolucion(d)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal nueva */}
      <NuevaDevolucionModal
        open={mostrarNueva}
        onClose={cerrarNueva}
        pedidoIdPreseleccionado={pedidoPreseleccionado}
        onCreada={() => devoluciones.refresh()}
      />

      {/* Modal detalle */}
      <MiDevolucionDetalleModal
        open={!!verDevolucion}
        devolucion={verDevolucion}
        onClose={() => setVerDevolucion(null)}
      />
    </div>
  );
}

function DevolucionMiaRow({ devolucion: d, onVer }) {
  const fecha = d.fechaSolicitud
    ? new Date(d.fechaSolicitud).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric'
      })
    : '—';

  return (
    <motion.tr
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      onClick={onVer}
      className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors cursor-pointer"
    >
      <td className="p-3 font-mono text-cockpit-neon">#{d.id}</td>
      <td className="p-3 hidden md:table-cell">
        <span className="font-mono text-cockpit-cyan">#{d.pedidoId}</span>
      </td>
      <td className="p-3">
        <div className="text-sm text-slate-200 truncate max-w-xs" title={d.motivo}>
          {d.motivo || <span className="text-slate-600 italic">sin motivo</span>}
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
    </motion.tr>
  );
}
