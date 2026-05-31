import { Check, Clock, Truck, Package, X, CreditCard } from 'lucide-react';

/**
 * Timeline horizontal del progreso de un pedido.
 * Estados: PENDIENTE → PAGADO → ENVIADO → ENTREGADO. CANCELADO es paralelo.
 */
const PASOS = [
  { key: 'PENDIENTE', label: 'Pendiente',  icon: Clock },
  { key: 'PAGADO',    label: 'Pagado',     icon: CreditCard },
  { key: 'ENVIADO',   label: 'Enviado',    icon: Truck },
  { key: 'ENTREGADO', label: 'Entregado',  icon: Package },
];

const ORDEN = { PENDIENTE: 0, PAGADO: 1, ENVIADO: 2, ENTREGADO: 3 };

export function PedidoTimeline({ estado }) {
  if (estado === 'CANCELADO') {
    return (
      <div className="hud-panel p-4 flex items-center gap-3 border-cockpit-danger/40"
           style={{ boxShadow: '0 0 16px rgba(239,68,68,0.18)' }}>
        <div className="p-2 border border-cockpit-danger/60">
          <X size={18} className="text-cockpit-danger" />
        </div>
        <div>
          <p className="hud-label text-cockpit-danger">// PEDIDO CANCELADO</p>
          <p className="text-xs text-slate-400 mt-0.5">
            El flujo fue interrumpido. Esta operación es definitiva.
          </p>
        </div>
      </div>
    );
  }

  const idxActual = ORDEN[estado] ?? -1;

  return (
    <div className="hud-panel p-4">
      <p className="hud-label mb-4">// PROGRESO DEL PEDIDO</p>
      <div className="flex items-center">
        {PASOS.map((paso, i) => {
          const Icon = paso.icon;
          const completado = i <= idxActual;
          const actual = i === idxActual;

          return (
            <div key={paso.key} className="flex-1 flex items-center last:flex-none">
              {/* Nodo */}
              <div className="flex flex-col items-center gap-1.5 relative">
                <div
                  className="w-9 h-9 grid place-items-center border-2 transition-all"
                  style={{
                    borderColor: completado ? '#a855f7' : 'rgba(120,80,220,0.25)',
                    background: completado ? 'rgba(168,85,247,0.15)' : 'transparent',
                    boxShadow: actual ? '0 0 18px rgba(168,85,247,0.6)' : 'none',
                  }}
                >
                  {completado && i < idxActual ? (
                    <Check size={14} className="text-cockpit-neon" />
                  ) : (
                    <Icon
                      size={14}
                      className={completado ? 'text-cockpit-neon' : 'text-slate-600'}
                    />
                  )}
                </div>
                <span
                  className={[
                    'text-[10px] tracking-widest uppercase font-display',
                    completado ? 'text-cockpit-neon' : 'text-slate-600',
                  ].join(' ')}
                >
                  {paso.label}
                </span>
              </div>

              {/* Conector */}
              {i < PASOS.length - 1 && (
                <div className="flex-1 h-0.5 mx-2 mb-5 relative overflow-hidden">
                  <div className="absolute inset-0 bg-cockpit-line" />
                  <div
                    className="absolute inset-y-0 left-0 transition-all duration-500"
                    style={{
                      width: i < idxActual ? '100%' : '0%',
                      background: '#a855f7',
                      boxShadow: '0 0 8px #a855f7',
                    }}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
