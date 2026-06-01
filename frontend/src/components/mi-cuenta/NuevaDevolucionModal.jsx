import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  HudModal,
} from '../admin/HudModal.jsx';
import { Package, Minus, Plus, AlertCircle, RotateCcw, Loader2, Info } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useToast } from '../admin/Toast.jsx';

/**
 * Modal para solicitar una nueva devolución.
 *
 *  - Paso 1: Selección de pedido. Solo se pueden devolver pedidos en estado
 *    ENTREGADO (se filtran del listado /pedidos/mios).
 *  - Paso 2: Selección de líneas a devolver con cantidad (no puede exceder
 *    la cantidad original). Motivo obligatorio.
 *
 *  Al enviar → POST /api/devoluciones con { pedidoId, motivo, lineas: [{ lineaPedidoId, cantidad }] }
 *
 * Si se le pasa `pedidoIdPreseleccionado`, salta directamente al paso 2.
 */
export function NuevaDevolucionModal({ open, onClose, pedidoIdPreseleccionado, onCreada }) {
  const [paso, setPaso] = useState(1);
  const [pedidos, setPedidos] = useState([]);
  const [pedidoSel, setPedidoSel] = useState(null);
  const [lineasSel, setLineasSel] = useState({}); // { lineaPedidoId: cantidad }
  const [motivo, setMotivo] = useState('');
  const [loadingPedidos, setLoadingPedidos] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const toast = useToast();

  // Cargar pedidos elegibles cuando se abre
  useEffect(() => {
    if (!open) return;
    let cancelado = false;
    async function cargar() {
      setLoadingPedidos(true);
      try {
        const { data } = await api.get('/pedidos/mios');
        if (cancelado) return;
        // Solo entregados son devolvibles
        const entregados = (data || []).filter(p => p.estado === 'ENTREGADO');
        setPedidos(entregados);

        // Si viene preseleccionado, saltar al paso 2
        if (pedidoIdPreseleccionado) {
          const preSel = entregados.find(p => p.id === Number(pedidoIdPreseleccionado));
          if (preSel) {
            seleccionarPedido(preSel);
          } else {
            toast.warn('Ese pedido no está disponible para devolución');
          }
        }
      } catch (_) {
        toast.err('No se pudieron cargar los pedidos');
      } finally {
        if (!cancelado) setLoadingPedidos(false);
      }
    }
    cargar();
    return () => { cancelado = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, pedidoIdPreseleccionado]);

  // Reset al cerrar
  useEffect(() => {
    if (!open) {
      setPaso(1);
      setPedidoSel(null);
      setLineasSel({});
      setMotivo('');
    }
  }, [open]);

  function seleccionarPedido(p) {
    setPedidoSel(p);
    // Pre-rellenar lineasSel con 0 unidades para cada línea
    const inicial = {};
    (p.lineas || []).forEach(l => { inicial[l.id] = 0; });
    setLineasSel(inicial);
    setPaso(2);
  }

  function cambiarCantidad(linea, nueva) {
    const max = linea.cantidad;
    const valida = Math.max(0, Math.min(max, nueva));
    setLineasSel(s => ({ ...s, [linea.id]: valida }));
  }

  // Total estimado del importe a devolver
  const importeEstimado = useMemo(() => {
    if (!pedidoSel) return 0;
    return (pedidoSel.lineas || []).reduce((s, l) => {
      const cant = lineasSel[l.id] || 0;
      return s + cant * Number(l.precioUnitario || 0);
    }, 0);
  }, [pedidoSel, lineasSel]);

  // Número de unidades totales seleccionadas
  const totalUnidades = useMemo(() => {
    return Object.values(lineasSel).reduce((s, c) => s + (c || 0), 0);
  }, [lineasSel]);

  async function enviar() {
    if (enviando || !pedidoSel) return;
    if (!motivo.trim()) {
      toast.warn('Indica el motivo de la devolución');
      return;
    }
    const lineasPayload = Object.entries(lineasSel)
      .filter(([_, cant]) => cant > 0)
      .map(([lineaId, cant]) => ({
        lineaPedidoId: Number(lineaId),
        cantidad: cant,
      }));

    if (lineasPayload.length === 0) {
      toast.warn('Selecciona al menos un producto a devolver');
      return;
    }

    setEnviando(true);
    try {
      const { data } = await api.post('/devoluciones', {
        pedidoId: pedidoSel.id,
        motivo: motivo.trim(),
        lineas: lineasPayload,
      });
      toast.ok('Solicitud de devolución enviada');
      onCreada && onCreada(data);
      onClose();
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo crear la devolución');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title="Solicitar devolución"
      subtitle={paso === 1 ? 'PASO 1 · ELIGE PEDIDO' : 'PASO 2 · PRODUCTOS Y MOTIVO'}
      size="lg"
    >
      {paso === 1 ? (
        <Paso1Pedidos
          pedidos={pedidos}
          loading={loadingPedidos}
          onSeleccionar={seleccionarPedido}
        />
      ) : (
        <Paso2Lineas
          pedido={pedidoSel}
          lineasSel={lineasSel}
          motivo={motivo}
          totalUnidades={totalUnidades}
          importeEstimado={importeEstimado}
          enviando={enviando}
          onCambiarCantidad={cambiarCantidad}
          onMotivo={setMotivo}
          onAtras={() => {
            // Solo permitir volver si no había preselección
            if (!pedidoIdPreseleccionado) setPaso(1);
            else onClose();
          }}
          onEnviar={enviar}
          puedeAtras={!pedidoIdPreseleccionado}
        />
      )}
    </HudModal>
  );
}

/* ---------- Paso 1: lista de pedidos elegibles ---------- */
function Paso1Pedidos({ pedidos, loading, onSeleccionar }) {
  if (loading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-16 bg-white/5 animate-pulse rounded" />
        ))}
      </div>
    );
  }
  if (pedidos.length === 0) {
    return (
      <div className="text-center py-8">
        <Package size={42} className="mx-auto mb-3 opacity-30 text-slate-500" />
        <p className="hud-label text-slate-500 mb-2">// NO TIENES PEDIDOS DEVOLVIBLES</p>
        <p className="text-xs text-slate-600 max-w-sm mx-auto">
          Solo se pueden devolver pedidos en estado <strong className="text-cockpit-ok">Entregado</strong>.
          Si tu pedido aún no ha llegado, espera a recibirlo.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-xs text-slate-400">
        Selecciona el pedido del que quieres devolver productos. Solo aparecen
        los pedidos ya entregados.
      </p>
      <div className="space-y-2">
        {pedidos.map(p => (
          <button
            key={p.id}
            onClick={() => onSeleccionar(p)}
            className="w-full flex items-center gap-3 p-3 border border-cockpit-line
                       hover:border-cockpit-neon/60 hover:bg-white/[0.025] transition-all text-left"
          >
            <span className="font-mono text-cockpit-neon shrink-0">#{p.id}</span>
            <div className="flex-1 min-w-0">
              <p className="text-sm text-white truncate">
                {p.lineas?.length || 0} {p.lineas?.length === 1 ? 'producto' : 'productos'}
              </p>
              <p className="hud-readout text-[10px] text-slate-500">
                {new Date(p.fechaPedido).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })}
              </p>
            </div>
            <span className="font-mono text-sm text-cockpit-cyan shrink-0">
              {Number(p.total || 0).toFixed(2)} €
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}

/* ---------- Paso 2: seleccionar líneas y motivo ---------- */
function Paso2Lineas({
  pedido, lineasSel, motivo, totalUnidades, importeEstimado, enviando,
  onCambiarCantidad, onMotivo, onAtras, onEnviar, puedeAtras,
}) {
  return (
    <div className="space-y-5">
      {/* Cabecera pedido */}
      <div className="hud-panel p-3 !bg-black/30 flex items-center justify-between">
        <div>
          <p className="hud-label">// PEDIDO ORIGEN</p>
          <p className="font-mono text-cockpit-neon mt-0.5">
            #{pedido.id} ·{' '}
            <span className="text-slate-400 text-xs">
              {new Date(pedido.fechaPedido).toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })}
            </span>
          </p>
        </div>
        {puedeAtras && (
          <button
            onClick={onAtras}
            className="text-[10px] tracking-[0.3em] uppercase text-cockpit-cyan hover:text-white"
            disabled={enviando}
          >
            ← Cambiar pedido
          </button>
        )}
      </div>

      {/* Líneas seleccionables */}
      <div>
        <p className="hud-label mb-2">// ELIGE QUÉ DEVOLVER · {totalUnidades} unidades</p>
        <div className="space-y-2">
          {(pedido.lineas || []).map(l => (
            <LineaSelector
              key={l.id}
              linea={l}
              cantidad={lineasSel[l.id] || 0}
              onCambiar={(n) => onCambiarCantidad(l, n)}
              disabled={enviando}
            />
          ))}
        </div>
      </div>

      {/* Motivo */}
      <div>
        <label className="block">
          <span className="hud-label">Motivo de la devolución *</span>
          <textarea
            value={motivo}
            onChange={(e) => onMotivo(e.target.value)}
            placeholder="Producto defectuoso, no era lo que esperaba, talla equivocada…"
            rows={3}
            className="hud-input resize-none font-sans mt-1.5"
            maxLength={1000}
            disabled={enviando}
          />
          <p className="text-[10px] text-slate-500 mt-1">
            {motivo.length}/1000 caracteres
          </p>
        </label>
      </div>

      {/* Resumen */}
      <div className="hud-panel p-3 !bg-black/30 flex items-center justify-between">
        <div className="flex gap-2 items-start">
          <Info size={14} className="text-cockpit-cyan shrink-0 mt-0.5" />
          <p className="text-[11px] text-slate-400 leading-relaxed">
            La solicitud quedará en estado <strong className="text-cockpit-amber">Solicitada</strong>.
            Un administrador la revisará y te notificará si se aprueba o rechaza.
          </p>
        </div>
        <div className="text-right shrink-0">
          <p className="hud-label">// A DEVOLVER</p>
          <p className="font-display text-lg text-cockpit-ok mt-0.5">
            {importeEstimado.toFixed(2)} €
          </p>
        </div>
      </div>

      {/* Acciones */}
      <div className="flex gap-3 justify-end">
        <button
          onClick={onAtras}
          className="hud-btn hud-btn--cyan !px-4 !py-2"
          disabled={enviando}
        >
          {puedeAtras ? 'Atrás' : 'Cancelar'}
        </button>
        <button
          onClick={onEnviar}
          disabled={enviando || totalUnidades === 0 || !motivo.trim()}
          className="hud-btn !px-5 !py-2"
        >
          {enviando
            ? <><Loader2 size={14} className="animate-spin" /> Enviando…</>
            : <><RotateCcw size={14} /> Enviar solicitud</>
          }
        </button>
      </div>
    </div>
  );
}

/* ---------- Selector por línea ---------- */
function LineaSelector({ linea, cantidad, onCambiar, disabled }) {
  const activa = cantidad > 0;
  return (
    <div
      className="hud-panel p-3 !bg-black/20 flex items-center gap-3 transition-all"
      style={activa ? {
        borderColor: 'rgba(168,85,247,0.5)',
        boxShadow: '0 0 14px rgba(168,85,247,0.12)',
      } : {}}
    >
      <div className="flex-1 min-w-0">
        <p className={`text-sm ${activa ? 'text-white' : 'text-slate-300'} truncate`}>
          {linea.productoNombre}
        </p>
        <p className="hud-readout text-[10px] text-slate-500">
          {linea.productoSku} · {Number(linea.precioUnitario).toFixed(2)} €/u
        </p>
      </div>

      <div className="flex items-center border border-cockpit-line shrink-0">
        <button
          onClick={() => onCambiar(cantidad - 1)}
          className="px-2 py-1.5 text-slate-400 hover:text-white hover:bg-white/5 transition-colors disabled:opacity-30"
          disabled={disabled || cantidad <= 0}
        >
          <Minus size={12} />
        </button>
        <span className="w-10 text-center font-mono text-white text-sm">
          {cantidad}<span className="text-slate-600">/{linea.cantidad}</span>
        </span>
        <button
          onClick={() => onCambiar(cantidad + 1)}
          className="px-2 py-1.5 text-cockpit-cyan hover:text-white hover:bg-white/5 transition-colors disabled:opacity-30"
          disabled={disabled || cantidad >= linea.cantidad}
        >
          <Plus size={12} />
        </button>
      </div>
    </div>
  );
}
