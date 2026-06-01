import { useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { EstadoBadge } from './EstadoBadge.jsx';
import { Check, X as XIcon, PackageCheck, MessageSquare, Calendar, User } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useToast } from './Toast.jsx';

/**
 * Modal con el detalle de una devolución y acciones admin:
 *   SOLICITADA → APROBADA → COMPLETADA
 *   SOLICITADA → RECHAZADA
 */
export function DevolucionDetalleModal({ open, onClose, devolucion, onChanged }) {
  const [busy, setBusy] = useState(null);
  const [mostrarRechazo, setMostrarRechazo] = useState(false);
  const [motivoRechazo, setMotivoRechazo] = useState('');
  const toast = useToast();

  if (!devolucion) return null;

  async function ejecutar(accion, url, payload) {
    if (busy) return;
    setBusy(accion);
    try {
      const { data } = await api.post(url, payload || {});
      toast.ok(`Devolución ${accion}`);
      onChanged && onChanged(data);
      setMostrarRechazo(false);
      setMotivoRechazo('');
    } catch (e) {
      toast.err(e.response?.data?.mensaje || `No se pudo ${accion}`);
    } finally {
      setBusy(null);
    }
  }

  function confirmarRechazo() {
    if (!motivoRechazo.trim()) {
      toast.warn('El motivo de rechazo es obligatorio');
      return;
    }
    ejecutar('rechazada', `/devoluciones/${devolucion.id}/rechazar`, {
      motivoRechazo: motivoRechazo.trim(),
    });
  }

  const fechaFmt = devolucion.fechaSolicitud
    ? new Date(devolucion.fechaSolicitud).toLocaleString('es-ES', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      })
    : '—';

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title={`Devolución #${devolucion.id}`}
      subtitle={`DETALLE · ${devolucion.estado}`}
      size="lg"
    >
      <div className="space-y-5">
        {/* Cabecera */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <InfoBlock icon={User} label="// CLIENTE ID" value={
            <span className="font-mono text-cockpit-cyan">#{devolucion.clienteId}</span>
          } />
          <InfoBlock icon={Calendar} label="// FECHA SOLICITUD" value={fechaFmt} />
          <InfoBlock icon={MessageSquare} label="// PEDIDO ORIGEN" value={
            <span className="font-mono text-cockpit-neon">#{devolucion.pedidoId}</span>
          } />
        </div>

        {/* Estado */}
        <div className="flex items-center justify-between hud-panel p-3 !bg-black/30">
          <p className="hud-label">// ESTADO ACTUAL</p>
          <EstadoBadge estado={devolucion.estado} size="lg" />
        </div>

        {/* Motivo del cliente */}
        <div className="hud-panel p-4">
          <p className="hud-label mb-2">// MOTIVO DEL CLIENTE</p>
          <p className="text-sm text-slate-200 italic leading-relaxed">
            "{devolucion.motivo || 'sin motivo'}"
          </p>
        </div>

        {/* Observaciones admin (si hay) */}
        {devolucion.observacionesAdmin && (
          <div
            className="hud-panel p-4"
            style={{
              borderColor: 'rgba(239,68,68,0.4)',
              boxShadow: '0 0 14px rgba(239,68,68,0.15)',
            }}
          >
            <p className="hud-label text-cockpit-danger mb-2">// OBSERVACIONES ADMIN</p>
            <p className="text-sm text-slate-200">
              {devolucion.observacionesAdmin}
            </p>
          </div>
        )}

        {/* Líneas */}
        <div className="hud-panel p-4">
          <p className="hud-label mb-3">
            // LÍNEAS DEVUELTAS · {devolucion.lineas?.length || 0} productos
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
                {(devolucion.lineas || []).map((l) => (
                  <tr key={l.id} className="border-b border-cockpit-line/30">
                    <td className="py-2 text-white">{l.productoNombre}</td>
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
                  <td colSpan={3} className="py-3 text-right hud-label">// IMPORTE A DEVOLVER</td>
                  <td className="py-3 text-right font-display text-xl text-cockpit-ok">
                    {Number(devolucion.importeDevuelto || 0).toFixed(2)} €
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Acciones */}
        {devolucion.estado === 'SOLICITADA' && !mostrarRechazo && (
          <div className="hud-panel p-4 !bg-black/40">
            <p className="hud-label mb-3">// ACCIONES</p>
            <div className="flex gap-2">
              <button
                onClick={() => ejecutar('aprobada', `/devoluciones/${devolucion.id}/aprobar`)}
                disabled={!!busy}
                className="hud-btn !px-4 !py-2"
                style={{ borderColor: 'rgba(52,211,153,0.6)', color: '#34d399' }}
              >
                <Check size={14} /> Aprobar
              </button>
              <button
                onClick={() => setMostrarRechazo(true)}
                disabled={!!busy}
                className="hud-btn !px-4 !py-2"
                style={{ borderColor: 'rgba(239,68,68,0.5)', color: '#ef4444' }}
              >
                <XIcon size={14} /> Rechazar
              </button>
            </div>
          </div>
        )}

        {/* Formulario inline de rechazo */}
        {mostrarRechazo && (
          <div
            className="hud-panel p-4 !bg-black/40"
            style={{ borderColor: 'rgba(239,68,68,0.5)' }}
          >
            <p className="hud-label text-cockpit-danger mb-2">// MOTIVO DE RECHAZO *</p>
            <textarea
              value={motivoRechazo}
              onChange={(e) => setMotivoRechazo(e.target.value)}
              placeholder="Explica al cliente por qué se rechaza la devolución (obligatorio)…"
              rows={3}
              className="hud-input resize-none font-sans"
              maxLength={1000}
              autoFocus
            />
            <div className="flex gap-2 justify-end mt-3">
              <button
                onClick={() => { setMostrarRechazo(false); setMotivoRechazo(''); }}
                className="hud-btn hud-btn--cyan !px-4 !py-2"
                disabled={!!busy}
              >
                Atrás
              </button>
              <button
                onClick={confirmarRechazo}
                className="hud-btn !px-4 !py-2"
                style={{ borderColor: 'rgba(239,68,68,0.5)', color: '#ef4444' }}
                disabled={!!busy || !motivoRechazo.trim()}
              >
                {busy ? 'Rechazando…' : 'Confirmar rechazo'}
              </button>
            </div>
          </div>
        )}

        {devolucion.estado === 'APROBADA' && (
          <div className="hud-panel p-4 !bg-black/40">
            <p className="hud-label mb-3">// SIGUIENTE PASO</p>
            <p className="text-xs text-slate-400 mb-3">
              Cuando hayas devuelto el dinero al cliente y recibido los productos, marca la
              devolución como completada para cerrarla.
            </p>
            <button
              onClick={() => ejecutar('completada', `/devoluciones/${devolucion.id}/completar`)}
              disabled={!!busy}
              className="hud-btn !px-4 !py-2"
              style={{ borderColor: 'rgba(52,211,153,0.6)', color: '#34d399' }}
            >
              <PackageCheck size={14} /> Marcar completada
            </button>
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
