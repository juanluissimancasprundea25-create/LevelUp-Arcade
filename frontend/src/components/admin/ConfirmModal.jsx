import { HudModal } from './HudModal.jsx';
import { AlertTriangle } from 'lucide-react';

/**
 * Modal de confirmación para acciones destructivas.
 *
 *   <ConfirmModal
 *     open={...} onClose={...} onConfirm={...}
 *     title="¿Eliminar producto?"
 *     message="Esta accion no se puede deshacer."
 *   />
 */
export function ConfirmModal({ open, onClose, onConfirm, title, message, busy }) {
  return (
    <HudModal open={open} onClose={onClose} title={title} subtitle="CONFIRMAR" size="sm">
      <div className="text-center py-2">
        <AlertTriangle size={42} className="mx-auto mb-4 text-cockpit-danger opacity-80" />
        <p className="text-slate-300 text-sm">{message}</p>
      </div>
      <div className="mt-6 flex gap-3 justify-end">
        <button onClick={onClose} className="hud-btn hud-btn--cyan !px-4 !py-2" disabled={busy}>
          Cancelar
        </button>
        <button
          onClick={onConfirm}
          disabled={busy}
          className="hud-btn !px-4 !py-2"
          style={{
            color: '#ef4444',
            borderColor: 'rgba(239,68,68,0.6)',
            boxShadow: '0 0 16px rgba(239,68,68,0.25), 0 0 0 1px rgba(239,68,68,0.15) inset',
          }}
        >
          {busy ? 'Eliminando…' : 'Eliminar'}
        </button>
      </div>
    </HudModal>
  );
}
