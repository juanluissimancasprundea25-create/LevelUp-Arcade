import { HudModal } from '../admin/HudModal.jsx';
import { EstadoBadge } from '../admin/EstadoBadge.jsx';
import { Calendar, Package, MessageSquare, AlertTriangle, Check } from 'lucide-react';

/**
 * Detalle de una devolución para el cliente. Solo lectura.
 *
 *  - Muestra estado, fecha, pedido origen, motivo del cliente
 *  - Si fue rechazada, muestra observación del admin destacada en rojo
 *  - Lista las líneas con cantidades y precios
 *  - Importe total
 */
export function MiDevolucionDetalleModal({ open, onClose, devolucion }) {
  if (!devolucion) return null;

  const fechaFmt = devolucion.fechaSolicitud
    ? new Date(devolucion.fechaSolicitud).toLocaleString('es-ES', {
        day: '2-digit', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      })
    : '—';

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title={`Devolución #${devolucion.id}`}
      subtitle={`MI SOLICITUD · ${devolucion.estado}`}
      size="lg"
    >
      <div className="space-y-5">
        {/* Cabecera estado */}
        <div className="flex items-center justify-between hud-panel p-3 !bg-black/30">
          <p className="hud-label">// ESTADO ACTUAL</p>
          <EstadoBadge estado={devolucion.estado} size="lg" />
        </div>

        {/* Mensaje contextual según estado */}
        <MensajeEstado estado={devolucion.estado} />

        {/* Datos */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <InfoBlock icon={Calendar} label="// FECHA SOLICITUD" value={fechaFmt} />
          <InfoBlock icon={Package} label="// PEDIDO ORIGEN" value={
            <span className="font-mono text-cockpit-neon">#{devolucion.pedidoId}</span>
          } />
        </div>

        {/* Motivo */}
        <div className="hud-panel p-4">
          <p className="hud-label mb-2">// TU MOTIVO</p>
          <p className="text-sm text-slate-200 italic leading-relaxed">
            "{devolucion.motivo || 'sin motivo indicado'}"
          </p>
        </div>

        {/* Observaciones admin (si rechazada o aprobada con nota) */}
        {devolucion.observacionesAdmin && (
          <div
            className="hud-panel p-4"
            style={{
              borderColor: devolucion.estado === 'RECHAZADA'
                ? 'rgba(239,68,68,0.4)'
                : 'rgba(34,211,238,0.4)',
              boxShadow: devolucion.estado === 'RECHAZADA'
                ? '0 0 14px rgba(239,68,68,0.15)'
                : '0 0 14px rgba(34,211,238,0.15)',
            }}
          >
            <p
              className="hud-label mb-2 flex items-center gap-2"
              style={{
                color: devolucion.estado === 'RECHAZADA' ? '#ef4444' : '#22d3ee'
              }}
            >
              <MessageSquare size={11} />
              // MENSAJE DEL EQUIPO
            </p>
            <p className="text-sm text-slate-200">
              {devolucion.observacionesAdmin}
            </p>
          </div>
        )}

        {/* Líneas */}
        <div className="hud-panel p-4">
          <p className="hud-label mb-3">
            // PRODUCTOS A DEVOLVER · {devolucion.lineas?.length || 0}
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
                  <td colSpan={3} className="py-3 text-right hud-label">
                    // {devolucion.estado === 'COMPLETADA' ? 'IMPORTE DEVUELTO' : 'IMPORTE SOLICITADO'}
                  </td>
                  <td className="py-3 text-right font-display text-xl text-cockpit-ok">
                    {Number(devolucion.importeDevuelto || 0).toFixed(2)} €
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      </div>
    </HudModal>
  );
}

function MensajeEstado({ estado }) {
  const cfg = {
    SOLICITADA: {
      color: '#fbbf24',
      icon: AlertTriangle,
      texto: 'Tu solicitud está en cola para revisión. Un administrador la procesará en las próximas 24-48 horas.',
    },
    APROBADA: {
      color: '#22d3ee',
      icon: Check,
      texto: 'Tu devolución ha sido aprobada. El equipo está procesando el reembolso — recibirás el dinero en breve.',
    },
    RECHAZADA: {
      color: '#ef4444',
      icon: AlertTriangle,
      texto: 'Tu solicitud ha sido rechazada. Lee el mensaje del equipo abajo para saber el motivo.',
    },
    COMPLETADA: {
      color: '#34d399',
      icon: Check,
      texto: 'Devolución completada. El importe ha sido reembolsado correctamente.',
    },
  };

  const c = cfg[estado];
  if (!c) return null;

  const Icon = c.icon;
  return (
    <div
      className="p-3 border flex gap-2"
      style={{
        borderColor: c.color + '66',
        background: c.color + '08',
      }}
    >
      <Icon size={14} className="shrink-0 mt-0.5" style={{ color: c.color }} />
      <p className="text-xs text-slate-300 leading-relaxed">
        {c.texto}
      </p>
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
