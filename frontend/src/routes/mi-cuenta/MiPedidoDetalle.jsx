import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft, Calendar, MapPin, CreditCard, Loader2,
  AlertCircle, Banknote, Wallet, X as XIcon,
} from 'lucide-react';
import { api } from '../../lib/api.js';
import { useToast } from '../../components/admin/Toast.jsx';
import { EstadoBadge } from '../../components/admin/EstadoBadge.jsx';
import { PedidoTimeline } from '../../components/admin/PedidoTimeline.jsx';

/**
 * Detalle de UN pedido para el cliente. URL: /cockpit/mi-cuenta/pedidos/:id
 *
 *  - Reutiliza PedidoTimeline y EstadoBadge del admin
 *  - Acciones disponibles para el cliente:
 *    - Si PENDIENTE: pagar (selector método) o cancelar
 *    - Otros estados: solo lectura
 *  - Si fue entregado, muestra opción de "solicitar devolución" (Fase 7c)
 */
const METODOS = [
  { v: 'TARJETA', l: 'Tarjeta', icon: CreditCard },
  { v: 'PAYPAL', l: 'PayPal', icon: Wallet },
  { v: 'TRANSFERENCIA', l: 'Transferencia', icon: Banknote },
];

export default function MiPedidoDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [pedido, setPedido] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [accion, setAccion] = useState(null);
  const [metodoPago, setMetodoPago] = useState('TARJETA');
  const [mostrarPago, setMostrarPago] = useState(false);

  async function cargar() {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get(`/pedidos/${id}`);
      setPedido(data);
    } catch (e) {
      setError(e.response?.status === 404
        ? 'Pedido no encontrado'
        : e.response?.status === 403
          ? 'No tienes permiso para ver este pedido'
          : 'No se pudo cargar el pedido');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function pagar() {
    setAccion('pagar');
    try {
      const { data } = await api.post(`/pedidos/${id}/pagar`, { metodoPago });
      setPedido(data);
      setMostrarPago(false);
      toast.ok(`Pedido pagado con ${metodoPago}`);
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo procesar el pago');
    } finally {
      setAccion(null);
    }
  }

  async function cancelar() {
    if (!confirm('¿Cancelar este pedido? La acción no se puede deshacer.')) return;
    setAccion('cancelar');
    try {
      const { data } = await api.post(`/pedidos/${id}/cancelar`);
      setPedido(data);
      toast.ok('Pedido cancelado');
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo cancelar');
    } finally {
      setAccion(null);
    }
  }

  if (loading) {
    return (
      <div className="space-y-3">
        <div className="h-8 w-48 bg-white/5 animate-pulse rounded" />
        <div className="hud-panel p-5 space-y-2">
          <div className="h-4 bg-white/5 animate-pulse rounded" />
          <div className="h-4 w-2/3 bg-white/5 animate-pulse rounded" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="hud-panel p-10 text-center">
        <AlertCircle size={42} className="mx-auto mb-3 opacity-50 text-cockpit-danger" />
        <p className="text-slate-300 mb-5">{error}</p>
        <Link to=".." relative="path" className="hud-btn hud-btn--cyan !px-4 !py-2 inline-flex">
          Volver a mis pedidos
        </Link>
      </div>
    );
  }

  if (!pedido) return null;

  const fechaFmt = pedido.fechaPedido
    ? new Date(pedido.fechaPedido).toLocaleString('es-ES', {
        day: '2-digit', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      })
    : '—';

  return (
    <div className="space-y-5">
      {/* Volver + cabecera */}
      <div>
        <Link
          to=".."
          relative="path"
          className="inline-flex items-center gap-1.5 text-xs tracking-widest uppercase text-cockpit-cyan hover:text-white transition-colors mb-3"
        >
          <ArrowLeft size={12} />
          Volver a mis pedidos
        </Link>

        <div className="flex items-end justify-between gap-3 flex-wrap">
          <div>
            <p className="hud-label text-cockpit-cyan/70">// PEDIDO</p>
            <h2 className="font-display text-2xl tracking-wide text-white mt-0.5">
              #{pedido.id}
            </h2>
          </div>
          <EstadoBadge estado={pedido.estado} size="lg" />
        </div>
      </div>

      {/* Datos básicos */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <InfoBlock icon={Calendar} label="// FECHA" value={fechaFmt} />
        <InfoBlock icon={CreditCard} label="// MÉTODO DE PAGO" value={
          pedido.metodoPago
            ? <span className="text-white tracking-wider">{pedido.metodoPago}</span>
            : <span className="text-slate-600 italic">sin pagar</span>
        } />
        <InfoBlock icon={MapPin} label="// ENVÍO" value={
          pedido.direccionEnvio || <span className="text-slate-600 italic">no especificada</span>
        } />
      </div>

      {/* Timeline (componente reutilizado del admin) */}
      <PedidoTimeline estado={pedido.estado} />

      {/* Líneas */}
      <div className="hud-panel p-4">
        <p className="hud-label mb-3">
          // PRODUCTOS · {pedido.lineas?.length || 0} {pedido.lineas?.length === 1 ? 'artículo' : 'artículos'}
        </p>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-cockpit-line">
                <th className="hud-label text-left py-2">Producto</th>
                <th className="hud-label text-right py-2 w-20">Cant.</th>
                <th className="hud-label text-right py-2 w-24">Precio</th>
                <th className="hud-label text-right py-2 w-24">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {(pedido.lineas || []).map((l) => (
                <tr key={l.id} className="border-b border-cockpit-line/30">
                  <td className="py-2">
                    <Link
                      to={`/cockpit/tienda/producto/${l.productoId}`}
                      className="text-white hover:text-cockpit-neon transition-colors"
                    >
                      {l.productoNombre}
                    </Link>
                    <div className="hud-readout text-slate-500 text-[10px]">{l.productoSku}</div>
                  </td>
                  <td className="py-2 text-right font-mono text-slate-200">{l.cantidad}</td>
                  <td className="py-2 text-right font-mono text-slate-400">
                    {Number(l.precioUnitario).toFixed(2)} €
                  </td>
                  <td className="py-2 text-right font-mono text-cockpit-cyan">
                    {Number(l.subtotal).toFixed(2)} €
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={3} className="py-3 text-right hud-label">// TOTAL</td>
                <td className="py-3 text-right font-display text-xl text-cockpit-ok">
                  {Number(pedido.total || 0).toFixed(2)} €
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      {/* Acciones disponibles para cliente */}
      {pedido.estado === 'PENDIENTE' && !mostrarPago && (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          className="hud-panel p-4 !bg-black/40"
        >
          <p className="hud-label mb-3">// ACCIONES DISPONIBLES</p>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setMostrarPago(true)}
              disabled={!!accion}
              className="hud-btn !px-4 !py-2"
            >
              <CreditCard size={14} /> Pagar ahora
            </button>
            <button
              onClick={cancelar}
              disabled={!!accion}
              className="hud-btn !px-4 !py-2"
              style={{ borderColor: 'rgba(239,68,68,0.5)', color: '#ef4444' }}
            >
              {accion === 'cancelar'
                ? <><Loader2 size={14} className="animate-spin" /> Cancelando…</>
                : <><XIcon size={14} /> Cancelar pedido</>}
            </button>
          </div>
        </motion.div>
      )}

      {/* Selector método de pago inline */}
      {mostrarPago && (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          className="hud-panel p-4 !bg-black/40"
          style={{ borderColor: 'rgba(168,85,247,0.5)' }}
        >
          <p className="hud-label text-cockpit-neon mb-3">// SELECCIONA MÉTODO DE PAGO</p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 mb-3">
            {METODOS.map(m => (
              <button
                key={m.v}
                onClick={() => setMetodoPago(m.v)}
                className="hud-panel p-3 !bg-black/30 text-left transition-all"
                style={metodoPago === m.v ? {
                  borderColor: 'rgba(168,85,247,0.6)',
                  boxShadow: '0 0 14px rgba(168,85,247,0.18)',
                } : {}}
                disabled={!!accion}
              >
                <div className="flex items-center gap-2">
                  <m.icon size={14} className={metodoPago === m.v ? 'text-cockpit-neon' : 'text-slate-500'} />
                  <span className={`text-sm ${metodoPago === m.v ? 'text-white' : 'text-slate-400'}`}>
                    {m.l}
                  </span>
                </div>
              </button>
            ))}
          </div>
          <div className="flex gap-2 justify-end">
            <button
              onClick={() => setMostrarPago(false)}
              disabled={!!accion}
              className="hud-btn hud-btn--cyan !px-4 !py-2"
            >
              Atrás
            </button>
            <button
              onClick={pagar}
              disabled={!!accion}
              className="hud-btn !px-4 !py-2"
            >
              {accion === 'pagar'
                ? <><Loader2 size={14} className="animate-spin" /> Pagando…</>
                : <>Confirmar pago de {Number(pedido.total || 0).toFixed(2)} €</>}
            </button>
          </div>
        </motion.div>
      )}

      {/* Si está entregado, ofrecer devolución (en Fase 7c se enchufa) */}
      {pedido.estado === 'ENTREGADO' && (
        <div className="hud-panel p-4 !bg-black/30">
          <p className="hud-label mb-2">// ¿NECESITAS DEVOLVER ALGO?</p>
          <p className="text-xs text-slate-400 mb-3">
            Tienes 14 días desde la entrega para solicitar una devolución completa o parcial.
          </p>
          <Link
            to={`/cockpit/mi-cuenta/devoluciones?pedido=${pedido.id}`}
            className="hud-btn hud-btn--cyan !px-4 !py-2 inline-flex"
          >
            Solicitar devolución
          </Link>
        </div>
      )}
    </div>
  );
}

function InfoBlock({ icon: Icon, label, value }) {
  return (
    <div className="hud-panel p-3 !bg-black/30">
      <div className="flex items-start gap-2">
        <Icon size={14} className="text-cockpit-cyan/70 shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <p className="hud-label">{label}</p>
          <div className="mt-1 text-sm break-words">{value}</div>
        </div>
      </div>
    </div>
  );
}
