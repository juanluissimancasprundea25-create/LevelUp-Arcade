import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, Search, X, FileText, Download, Receipt } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { api, getToken } from '../../lib/api.js';
import { useToast } from '../../components/admin/Toast.jsx';

export default function Facturas() {
  const [busqueda, setBusqueda] = useState('');
  const [descargando, setDescargando] = useState(null);
  const toast = useToast();

  // Pedimos página grande para evitar paginar de momento.
  // Endpoint devuelve Page<FacturaResponse> con { content, totalElements... }
  const facturas = useFetch('/facturas', { params: { size: 500, sort: 'fechaEmision,desc' } });

  // Page<>: usar data.content
  const listado = facturas.data?.content || facturas.data || [];

  const filtradas = useMemo(() => {
    let arr = listado;
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(f =>
        (f.numeroFactura || '').toLowerCase().includes(q) ||
        (f.clienteNombre || '').toLowerCase().includes(q) ||
        String(f.pedidoId).includes(q)
      );
    }
    return arr;
  }, [listado, busqueda]);

  const totalFacturado = useMemo(() => {
    return filtradas.reduce((s, f) => s + Number(f.total || 0), 0);
  }, [filtradas]);

  async function descargarPdf(factura) {
    if (descargando) return;
    setDescargando(factura.id);
    try {
      // axios con responseType blob
      const res = await api.get(`/facturas/${factura.id}/pdf`, { responseType: 'blob' });
      // Crear URL temporal y forzar descarga
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `factura-${factura.numeroFactura || factura.id}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toast.ok(`PDF descargado · ${factura.numeroFactura}`);
    } catch (e) {
      toast.err('No se pudo descargar el PDF');
    } finally {
      setDescargando(null);
    }
  }

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
            Facturas
          </h1>
          <p className="hud-readout mt-1">
            {facturas.loading
              ? 'Sincronizando…'
              : `${filtradas.length} facturas · ${totalFacturado.toFixed(2)} € facturados`}
          </p>
        </div>
        <button
          onClick={() => facturas.refresh()}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
          disabled={facturas.loading}
        >
          <RefreshCw size={14} className={facturas.loading ? 'animate-spin' : ''} />
          <span className="hidden sm:inline">Refrescar</span>
        </button>
      </motion.header>

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
            placeholder="Buscar por nº factura, cliente o ID de pedido…"
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
        <p className="hud-readout text-slate-500 mt-2 text-[10px]">
          ℹ Para emitir una factura, abre el pedido correspondiente y pulsa "Emitir factura".
        </p>
      </motion.div>

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
                <th className="hud-label text-left p-3">Nº Factura</th>
                <th className="hud-label text-left p-3">Cliente</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Fecha emisión</th>
                <th className="hud-label text-center p-3 hidden lg:table-cell">Pedido</th>
                <th className="hud-label text-right p-3">Total</th>
                <th className="hud-label text-right p-3">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {facturas.loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={6}>
                      <div className="h-10 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtradas.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-10 text-center">
                    <Receipt size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {listado.length === 0
                        ? '// NO HAY FACTURAS EMITIDAS · EMITE LA PRIMERA DESDE UN PEDIDO'
                        : '// NINGÚN RESULTADO'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtradas.map((f) => (
                  <FacturaRow
                    key={f.id}
                    factura={f}
                    descargando={descargando === f.id}
                    onDescargar={() => descargarPdf(f)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </motion.div>
    </div>
  );
}

function FacturaRow({ factura: f, descargando, onDescargar }) {
  const fecha = f.fechaEmision
    ? new Date(f.fechaEmision).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric'
      })
    : '—';

  return (
    <tr className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors">
      <td className="p-3">
        <div className="flex items-center gap-2">
          <FileText size={14} className="text-cockpit-cyan" />
          <span className="font-mono text-sm text-white">{f.numeroFactura}</span>
        </div>
      </td>
      <td className="p-3 text-sm text-slate-200 truncate">{f.clienteNombre || '—'}</td>
      <td className="p-3 hidden md:table-cell text-xs text-slate-400 hud-readout">{fecha}</td>
      <td className="p-3 hidden lg:table-cell text-center">
        <span className="font-mono text-cockpit-neon">#{f.pedidoId}</span>
      </td>
      <td className="p-3 text-right font-mono text-cockpit-ok">
        {Number(f.total || 0).toFixed(2)} €
      </td>
      <td className="p-3 text-right whitespace-nowrap">
        <button
          onClick={onDescargar}
          disabled={descargando}
          className="p-1.5 mr-1 text-cockpit-cyan hover:text-white hover:bg-cockpit-cyan/10 transition-colors disabled:opacity-50"
          title="Descargar PDF"
        >
          {descargando
            ? <RefreshCw size={15} className="animate-spin" />
            : <Download size={15} />}
        </button>
      </td>
    </tr>
  );
}
