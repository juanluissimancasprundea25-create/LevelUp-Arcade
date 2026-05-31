import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw, Search, X, FileText, Download, Receipt } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { api } from '../../lib/api.js';
import { useToast } from '../../components/admin/Toast.jsx';

/**
 * Mis facturas — URL: /cockpit/mi-cuenta/facturas
 *
 *  - Lista las facturas del cliente actual (GET /facturas/mias)
 *  - Descarga PDF como blob (mismo flujo que admin pero filtrado por cliente)
 *  - Sin permiso para "verificar" pública (eso es solo para admin)
 */
export default function MisFacturas() {
  const [busqueda, setBusqueda] = useState('');
  const [descargando, setDescargando] = useState(null);
  const toast = useToast();

  const facturas = useFetch('/facturas/mias', {
    params: { size: 200, sort: 'fechaEmision,desc' },
  });

  const listado = facturas.data?.content || facturas.data || [];

  const filtradas = useMemo(() => {
    let arr = listado;
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(f =>
        (f.numeroFactura || '').toLowerCase().includes(q) ||
        String(f.pedidoId).includes(q)
      );
    }
    return arr;
  }, [listado, busqueda]);

  const totalGastado = useMemo(() => {
    return listado.reduce((s, f) => s + Number(f.total || 0), 0);
  }, [listado]);

  async function descargar(factura) {
    if (descargando) return;
    setDescargando(factura.id);
    try {
      const res = await api.get(`/facturas/${factura.id}/pdf`, { responseType: 'blob' });
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
    <div className="space-y-5">
      <header className="flex items-end justify-between gap-3 flex-wrap">
        <div>
          <p className="hud-label text-cockpit-cyan/70">// FACTURACIÓN</p>
          <h2 className="font-display text-xl tracking-wide text-white mt-0.5">
            Mis facturas
          </h2>
          <p className="hud-readout mt-1">
            {facturas.loading
              ? 'Sincronizando…'
              : `${filtradas.length} facturas · ${totalGastado.toFixed(2)} € facturados`}
          </p>
        </div>
        <button
          onClick={() => facturas.refresh()}
          disabled={facturas.loading}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
        >
          <RefreshCw size={14} className={facturas.loading ? 'animate-spin' : ''} />
          <span className="hidden sm:inline">Refrescar</span>
        </button>
      </header>

      {/* Aviso conservación */}
      <div className="hud-panel p-3 !bg-black/30 flex gap-2">
        <FileText size={14} className="text-cockpit-cyan shrink-0 mt-0.5" />
        <p className="text-[11px] text-slate-400 leading-relaxed">
          Las facturas se emiten en cuanto el pedido pasa a estado <strong className="text-cockpit-cyan">Pagado</strong>.
          Descárgalas y guárdalas por si las necesitas más adelante — quedan disponibles aquí permanentemente.
        </p>
      </div>

      {/* Buscador */}
      <div className="hud-panel p-4">
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nº factura o nº pedido…"
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
                <th className="hud-label text-left p-3">Nº Factura</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Fecha</th>
                <th className="hud-label text-center p-3 hidden lg:table-cell">Pedido</th>
                <th className="hud-label text-right p-3">Total</th>
                <th className="hud-label text-right p-3 w-24">PDF</th>
              </tr>
            </thead>
            <tbody>
              {facturas.loading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={5}>
                      <div className="h-10 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtradas.length === 0 ? (
                <tr>
                  <td colSpan={5} className="p-10 text-center">
                    <Receipt size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {listado.length === 0
                        ? '// TODAVÍA NO TIENES FACTURAS'
                        : '// NINGUNA FACTURA COINCIDE'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtradas.map((f) => (
                  <FacturaRow
                    key={f.id}
                    factura={f}
                    descargando={descargando === f.id}
                    onDescargar={() => descargar(f)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
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
      <td className="p-3 hidden md:table-cell text-xs text-slate-400 hud-readout">{fecha}</td>
      <td className="p-3 hidden lg:table-cell text-center">
        <span className="font-mono text-cockpit-neon">#{f.pedidoId}</span>
      </td>
      <td className="p-3 text-right font-mono text-cockpit-ok">
        {Number(f.total || 0).toFixed(2)} €
      </td>
      <td className="p-3 text-right">
        <button
          onClick={onDescargar}
          disabled={descargando}
          className="hud-btn hud-btn--cyan !px-3 !py-1.5 !text-[10px] disabled:opacity-50"
          title="Descargar PDF"
        >
          {descargando
            ? <RefreshCw size={12} className="animate-spin" />
            : <Download size={12} />}
          PDF
        </button>
      </td>
    </tr>
  );
}
