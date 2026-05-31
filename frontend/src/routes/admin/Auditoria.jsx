import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  RefreshCw, X, Activity, ChevronLeft, ChevronRight,
  Database, Zap, Filter, User as UserIcon,
} from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useToast } from '../../components/admin/Toast.jsx';

/**
 * Visualización del log de auditoría.
 *
 *  - Tabla paginada (20 por página por defecto)
 *  - Filtros: acción, entidad, usuario (autocomplete por nombre/email)
 *  - Acciones tienen color según tipo (CREATE verde, UPDATE cyan,
 *    DELETE rojo, LOGIN violeta, etc.)
 *
 * Para mostrar nombres en lugar de IDs, carga /api/usuarios/lookup
 * y construye un mapa id -> usuario que se usa para resolver tanto la
 * columna "Usuario" como el filtro de búsqueda.
 */

const COLOR_POR_ACCION = (accion) => {
  if (!accion) return '#94a3b8';
  const a = accion.toUpperCase();
  if (a.includes('CREATE') || a.includes('CREAR'))    return '#34d399';
  if (a.includes('UPDATE') || a.includes('ACTUALIZ')) return '#22d3ee';
  if (a.includes('DELETE') || a.includes('BORR') || a.includes('ELIMIN')) return '#ef4444';
  if (a.includes('LOGIN') || a.includes('AUTH'))      return '#a855f7';
  if (a.includes('CANCEL'))                            return '#fbbf24';
  return '#94a3b8';
};

const COLOR_POR_ROL = {
  ADMIN:    '#a855f7',
  EMPLEADO: '#22d3ee',
  CLIENTE:  '#34d399',
};

export default function Auditoria() {
  const [logs, setLogs] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalElementos, setTotalElementos] = useState(0);
  const [loading, setLoading] = useState(true);
  const [filtroAccion, setFiltroAccion] = useState('');
  const [filtroEntidad, setFiltroEntidad] = useState('');
  const [filtroUsuarioTexto, setFiltroUsuarioTexto] = useState('');
  const [filtroUsuarioId, setFiltroUsuarioId] = useState(null);
  const toast = useToast();

  const tamPag = 20;

  // Lookup de usuarios para resolver IDs → nombre/rol
  const usuariosLookup = useFetch('/usuarios/lookup');
  const mapaUsuarios = useMemo(() => {
    const m = new Map();
    for (const u of usuariosLookup.data || []) m.set(u.id, u);
    return m;
  }, [usuariosLookup.data]);

  async function cargar() {
    setLoading(true);
    const params = { page: pagina, size: tamPag, sort: 'fecha,desc' };
    if (filtroAccion.trim())   params.accion    = filtroAccion.trim();
    if (filtroEntidad.trim())  params.entidad   = filtroEntidad.trim();
    if (filtroUsuarioId != null) params.usuarioId = filtroUsuarioId;
    try {
      const { data } = await api.get('/auditoria', { params });
      setLogs(data?.content || []);
      setTotalPaginas(data?.totalPages || 0);
      setTotalElementos(data?.totalElements || 0);
    } catch (e) {
      toast.err('No se pudo cargar el log');
    } finally {
      setLoading(false);
    }
  }

  // Recargar al cambiar página o filtros (filtroUsuarioId es lo que importa, no el texto)
  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, filtroAccion, filtroEntidad, filtroUsuarioId]);

  function aplicarFiltroAccion(v)  { setPagina(0); setFiltroAccion(v); }
  function aplicarFiltroEntidad(v) { setPagina(0); setFiltroEntidad(v); }

  /**
   * Cuando el texto del filtro de usuario cambia:
   *  - Si está vacío, quito el filtro.
   *  - Si coincide EXACTAMENTE con un nombre completo o email,
   *    aplico el filtro por usuarioId.
   *  - Si no coincide, mantengo el texto pero no aplico filtro (esperando
   *    que el usuario complete la búsqueda).
   */
  function onTextoUsuarioChange(v) {
    setFiltroUsuarioTexto(v);
    if (!v.trim()) {
      setPagina(0);
      setFiltroUsuarioId(null);
      return;
    }
    const vLow = v.trim().toLowerCase();
    const match = (usuariosLookup.data || []).find(u =>
      u.email?.toLowerCase() === vLow ||
      u.nombreCompleto?.toLowerCase() === vLow ||
      (u.nombre || '').toLowerCase() === vLow
    );
    if (match) {
      setPagina(0);
      setFiltroUsuarioId(match.id);
    } else {
      // No-match aún: limpiar id activo para no mantener uno stale
      setFiltroUsuarioId(null);
    }
  }

  function limpiarFiltros() {
    setPagina(0);
    setFiltroAccion('');
    setFiltroEntidad('');
    setFiltroUsuarioTexto('');
    setFiltroUsuarioId(null);
  }

  const hayFiltros = filtroAccion || filtroEntidad || filtroUsuarioTexto;

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
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1 flex items-center gap-3">
            <Activity className="text-cockpit-cyan" size={26} />
            Auditoría
          </h1>
          <p className="hud-readout mt-1">
            {loading ? 'Sincronizando…' : `${totalElementos} registros · página ${pagina + 1} de ${Math.max(totalPaginas, 1)}`}
          </p>
        </div>
        <button
          onClick={cargar}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
          disabled={loading}
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          <span className="hidden sm:inline">Refrescar</span>
        </button>
      </motion.header>

      {/* Filtros */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="hud-panel p-4 mb-5"
      >
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {/* Acción */}
          <label className="block">
            <span className="hud-label">Acción contiene</span>
            <div className="relative mt-1.5">
              <Zap size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              <input
                type="text"
                value={filtroAccion}
                onChange={(e) => aplicarFiltroAccion(e.target.value)}
                placeholder="CREATE_PRODUCTO, LOGIN…"
                className="hud-input !pl-8"
              />
              {filtroAccion && (
                <button
                  onClick={() => aplicarFiltroAccion('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
                  aria-label="Limpiar"
                >
                  <X size={12} />
                </button>
              )}
            </div>
          </label>

          {/* Entidad */}
          <label className="block">
            <span className="hud-label">Entidad contiene</span>
            <div className="relative mt-1.5">
              <Database size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              <input
                type="text"
                value={filtroEntidad}
                onChange={(e) => aplicarFiltroEntidad(e.target.value)}
                placeholder="Producto, Cliente, Pedido…"
                className="hud-input !pl-8"
              />
              {filtroEntidad && (
                <button
                  onClick={() => aplicarFiltroEntidad('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
                  aria-label="Limpiar"
                >
                  <X size={12} />
                </button>
              )}
            </div>
          </label>

          {/* Usuario con autocomplete por datalist */}
          <label className="block">
            <span className="hud-label">Usuario (nombre o email)</span>
            <div className="relative mt-1.5">
              <UserIcon size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              <input
                type="text"
                list="usuarios-lookup-options"
                value={filtroUsuarioTexto}
                onChange={(e) => onTextoUsuarioChange(e.target.value)}
                placeholder={usuariosLookup.loading ? 'Cargando…' : 'Empieza a escribir…'}
                className="hud-input !pl-8"
                disabled={usuariosLookup.loading}
              />
              {filtroUsuarioTexto && (
                <button
                  onClick={() => onTextoUsuarioChange('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
                  aria-label="Limpiar"
                >
                  <X size={12} />
                </button>
              )}
              <datalist id="usuarios-lookup-options">
                {(usuariosLookup.data || []).map((u) => (
                  <option key={u.id} value={u.nombreCompleto || u.nombre}>
                    {u.email} · {u.rol}
                  </option>
                ))}
              </datalist>
            </div>
            {filtroUsuarioTexto && filtroUsuarioId == null && (
              <p className="text-[10px] text-cockpit-amber mt-1">
                ⚠ Selecciona uno de la lista para aplicar el filtro
              </p>
            )}
          </label>
        </div>

        {hayFiltros && (
          <button
            onClick={limpiarFiltros}
            className="mt-3 text-[10px] tracking-[0.3em] uppercase text-cockpit-cyan hover:text-white"
          >
            <Filter size={10} className="inline mr-1" />
            Quitar todos los filtros
          </button>
        )}
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
                <th className="hud-label text-left p-3 w-44">Fecha</th>
                <th className="hud-label text-left p-3">Acción</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Entidad</th>
                <th className="hud-label text-left p-3 hidden lg:table-cell">Descripción</th>
                <th className="hud-label text-left p-3 w-48">Usuario</th>
                <th className="hud-label text-right p-3 w-28 hidden lg:table-cell">IP</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={6}>
                      <div className="h-8 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-10 text-center">
                    <Activity size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      // SIN REGISTROS PARA ESOS FILTROS
                    </p>
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <LogRow
                    key={log.id}
                    log={log}
                    usuario={log.usuarioId != null ? mapaUsuarios.get(log.usuarioId) : null}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Paginación */}
        {totalPaginas > 1 && (
          <div className="border-t border-cockpit-line p-3 flex items-center justify-between gap-3">
            <p className="hud-readout text-slate-500 text-[10px]">
              Mostrando {logs.length} de {totalElementos}
            </p>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPagina(p => Math.max(0, p - 1))}
                disabled={pagina === 0 || loading}
                className="hud-btn hud-btn--cyan !px-2 !py-1.5 disabled:opacity-40"
              >
                <ChevronLeft size={14} />
              </button>
              <span className="hud-label px-2">
                {pagina + 1} / {totalPaginas}
              </span>
              <button
                onClick={() => setPagina(p => Math.min(totalPaginas - 1, p + 1))}
                disabled={pagina >= totalPaginas - 1 || loading}
                className="hud-btn hud-btn--cyan !px-2 !py-1.5 disabled:opacity-40"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </motion.div>
    </div>
  );
}

/* ---------- Fila ---------- */
function LogRow({ log, usuario }) {
  const color = COLOR_POR_ACCION(log.accion);
  const fecha = log.fecha
    ? new Date(log.fecha).toLocaleString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
      })
    : '—';

  return (
    <tr className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors">
      <td className="p-3">
        <span className="hud-readout text-[10px] text-slate-300">{fecha}</span>
      </td>
      <td className="p-3">
        <span
          className="inline-block px-2 py-0.5 text-[10px] tracking-widest uppercase font-display border"
          style={{
            color,
            borderColor: color + '66',
            background: color + '10',
          }}
        >
          {log.accion}
        </span>
      </td>
      <td className="p-3 hidden md:table-cell">
        {log.entidad ? (
          <div>
            <span className="text-sm text-white">{log.entidad}</span>
            {log.entidadId != null && (
              <span className="hud-readout text-slate-500 text-[10px] ml-1">
                #{log.entidadId}
              </span>
            )}
          </div>
        ) : (
          <span className="text-slate-600 text-xs">—</span>
        )}
      </td>
      <td className="p-3 hidden lg:table-cell">
        <span className="text-xs text-slate-400 line-clamp-2" title={log.descripcion}>
          {log.descripcion || <span className="text-slate-700 italic">sin descripción</span>}
        </span>
      </td>

      {/* Usuario: nombre + rol coloreado */}
      <td className="p-3">
        {usuario ? (
          <UsuarioChip usuario={usuario} />
        ) : log.usuarioId != null ? (
          <span className="hud-readout text-slate-500 text-xs">
            #{log.usuarioId} <span className="italic opacity-70">(eliminado)</span>
          </span>
        ) : (
          <span className="text-slate-700 text-xs italic">anónimo</span>
        )}
      </td>

      <td className="p-3 text-right hidden lg:table-cell">
        <span className="hud-readout text-[10px] text-slate-500">
          {log.ipOrigen || '—'}
        </span>
      </td>
    </tr>
  );
}

function UsuarioChip({ usuario: u }) {
  const inicial = (u.nombre?.[0] || u.email?.[0] || '?').toUpperCase();
  const colorRol = COLOR_POR_ROL[u.rol] || '#94a3b8';

  return (
    <div className="flex items-center gap-2 min-w-0">
      <div
        className="w-7 h-7 grid place-items-center font-display text-xs border shrink-0"
        style={{
          borderColor: colorRol + '66',
          color: colorRol,
          background: colorRol + '10',
        }}
      >
        {inicial}
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-xs text-white truncate" title={u.email}>
          {u.nombreCompleto || u.nombre}
        </div>
        <div
          className="hud-readout text-[9px] truncate"
          style={{ color: colorRol }}
          title={u.email}
        >
          {u.rol}
        </div>
      </div>
    </div>
  );
}
