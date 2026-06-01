import { useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { EstadoBadge } from './EstadoBadge.jsx';
import { PedidoTimeline } from './PedidoTimeline.jsx';
import { CreditCard, Truck, Package, X, FileText, MapPin, User, Calendar } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useToast } from './Toast.jsx';

/**
 * Modal con el detalle completo de un pedido:
 *  - Cabecera con cliente, fecha y badge de estado
 *  - Timeline visual del progreso
 *  - Líneas del pedido (productos)
 *  - Total
 *  - Botones de acción según el estado actual y el rol
 *  - Botón "Emitir factura" si está PAGADO/ENVIADO/ENTREGADO
 */
export function PedidoDetalleModal({ open, onClose, pedido, esAdmin, onChanged, onFacturaEmitida }) {
  const [busy, setBusy] = useState(null); // string con la acción en curso
  const toast = useToast();

  if (!pedido) return null;

  async function ejecutar(accion, url, payload) {
    if (busy) return;
    setBusy(accion);
    try {
      const { data } = await api.post(url, payload || {});
      toast.ok(`Pedido ${accion}`);
      onChanged && onChanged(data);
    } catch (e) {
      toast.err(e.response?.data?.mensaje || `No se pudo ${accion}`);
    } finally {
      setBusy(null);
    }
  }

  async function emitirFactura() {
    if (busy) return;
    setBusy('factura');
    try {
      const { data } = await api.post(`/facturas/desde-pedido/${pedido.id}`);
      toast.ok(`Factura ${data.numeroFactura} emitida`);
      onFacturaEmitida && onFacturaEmitida(data);
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo emitir la factura');
    } finally {
      setBusy(null);
    }
  }

  const fechaFmt = pedido.fechaPedido
    ? new Date(pedido.fechaPedido).toLocaleString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      })
    : '—';

  const total = Number(pedido.total || 0);
  const facturable = ['PAGADO', 'ENVIADO', 'ENTREGADO'].includes(pedido.estado);

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title={`Pedido #${pedido.id}`}
      subtitle={`DETALLE · ${pedido.estado}`}
      size="lg"
    >
      <div className="space-y-5">
        {/* Cabecera con datos */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <InfoBlock icon={User} label="// CLIENTE" value={
            <>
              <div className="text-white">{pedido.clienteNombreCompleto || '—'}</div>
              <div className="hud-readout text-cockpit-cyan/80 mt-0.5">{pedido.clienteEmail}</div>
            </>
          } />
          <InfoBlock icon={Calendar} label="// FECHA" value={fechaFmt} />
          <InfoBlock icon={CreditCard} label="// MÉTODO DE PAGO" value={
            pedido.metodoPago
              ? <span className="text-white tracking-wider">{pedido.metodoPago}</span>
              : <span className="text-slate-600">pendiente</span>
          } />
        </div>

        {pedido.direccionEnvio && (
          <InfoBlock icon={MapPin} label="// DIRECCIÓN DE ENVÍO" value={pedido.direccionEnvio} />
        )}

        {/* Estado actual */}
        <div className="flex items-center justify-between hud-panel p-3 !bg-black/30">
          <p className="hud-label">// ESTADO ACTUAL</p>
          <EstadoBadge estado={pedido.estado} size="lg" />
        </div>

        {/* Timeline */}
        <PedidoTimeline estado={pedido.estado} />

        {/* Líneas del pedido */}
        <div className="hud-panel p-4">
          <p className="hud-label mb-3">// LÍNEAS · {pedido.lineas?.length || 0} productos</p>
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
                      <div className="text-white">{l.productoNombre}</div>
                      <div className="hud-readout text-slate-500">{l.productoSku}</div>
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
                    {total.toFixed(2)} €
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Acciones */}
        {esAdmin && pedido.estado !== 'CANCELADO' && pedido.estado !== 'ENTREGADO' && (
          <div className="hud-panel p-4 !bg-black/40">
            <p className="hud-label mb-3">// ACCIONES DE GESTIÓN</p>
            <div className="flex flex-wrap gap-2">
              {pedido.estado === 'PENDIENTE' && (
                <button
                  onClick={() => ejecutar('marcado como pagado', `/pedidos/${pedido.id}/pagar`, { metodoPago: 'TARJETA' })}
                  disabled={!!busy}
                  className="hud-btn hud-btn--cyan !px-4 !py-2"
                >
                  <CreditCard size={14} /> Marcar pagado
                </button>
              )}
              {pedido.estado === 'PAGADO' && (
                <button
                  onClick={() => ejecutar('enviado', `/pedidos/${pedido.id}/enviar`)}
                  disabled={!!busy}
                  className="hud-btn !px-4 !py-2"
                >
                  <Truck size={14} /> Marcar enviado
                </button>
              )}
              {pedido.estado === 'ENVIADO' && (
                <button
                  onClick={() => ejecutar('entregado', `/pedidos/${pedido.id}/entregar`)}
                  disabled={!!busy}
                  className="hud-btn !px-4 !py-2"
                  style={{ borderColor: 'rgba(52,211,153,0.6)', color: '#34d399' }}
                >
                  <Package size={14} /> Marcar entregado
                </button>
              )}
              {pedido.estado === 'PENDIENTE' && (
                <button
                  onClick={() => ejecutar('cancelado', `/pedidos/${pedido.id}/cancelar`)}
                  disabled={!!busy}
                  className="hud-btn !px-4 !py-2"
                  style={{ borderColor: 'rgba(239,68,68,0.5)', color: '#ef4444' }}
                >
                  <X size={14} /> Cancelar
                </button>
              )}
              {facturable && (
                <button
                  onClick={emitirFactura}
                  disabled={!!busy}
                  className="hud-btn hud-btn--cyan !px-4 !py-2 ml-auto"
                >
                  <FileText size={14} />
                  {busy === 'factura' ? 'Emitiendo…' : 'Emitir factura'}
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </HudModal>
  );
}

function InfoBlock({ icon: Icon, label, value }) {
  return (
    <div className="hud-panel p-3 !bg-black/30">
      <div className="flex items-start gap-2">
        <Icon size={14} className="text-cockpit-cyan/70 shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <p className="hud-label">{label}</p>
          <div className="mt-1 text-sm">{value}</div>
        </div>
      </div>
    </div>
  );
}
