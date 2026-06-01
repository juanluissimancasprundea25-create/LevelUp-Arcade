import { useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { Copy, Check, AlertTriangle, Eye, EyeOff } from 'lucide-react';
import { useToast } from './Toast.jsx';

/**
 * Modal especial que se muestra una sola vez tras crear un cliente.
 * Enseña la password temporal en plano y permite copiarla. El admin
 * debe comunicársela al cliente por canal seguro.
 */
export function PasswordTemporalModal({ open, onClose, cliente, password }) {
  const [revealed, setRevealed] = useState(false);
  const [copied, setCopied] = useState(false);
  const toast = useToast();

  async function copiar() {
    try {
      await navigator.clipboard.writeText(password);
      setCopied(true);
      toast.ok('Contraseña copiada al portapapeles');
      setTimeout(() => setCopied(false), 2000);
    } catch (_) {
      toast.err('No se pudo copiar — selecciónala manualmente');
    }
  }

  if (!cliente) return null;

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title="Cliente creado"
      subtitle="CREDENCIAL TEMPORAL"
      size="md"
      closeOnBackdrop={false}
    >
      <div className="text-center mb-5">
        <div className="inline-block p-3 border border-cockpit-ok/60 mb-3">
          <Check size={28} className="text-cockpit-ok" />
        </div>
        <h3 className="font-display text-xl text-white">
          {cliente.nombreCompleto || cliente.nombre}
        </h3>
        <p className="hud-readout mt-1 opacity-80">{cliente.email}</p>
      </div>

      {/* Aviso */}
      <div
        className="border border-cockpit-amber/60 bg-cockpit-amber/5 p-4 mb-5"
        style={{ boxShadow: '0 0 20px rgba(251,191,36,0.15)' }}
      >
        <div className="flex gap-3">
          <AlertTriangle size={18} className="text-cockpit-amber shrink-0 mt-0.5" />
          <div className="text-xs text-slate-300 leading-relaxed">
            Esta es la contraseña <strong className="text-cockpit-amber">temporal</strong> del cliente.
            Solo aparece <strong className="text-cockpit-amber">una vez</strong> — la base de datos solo
            guarda el hash. Cópiala y comunícasela por un canal seguro. El cliente debería cambiarla
            en su primer login.
          </div>
        </div>
      </div>

      {/* Password box */}
      <div className="hud-panel p-4 mb-5 !bg-black/60">
        <div className="hud-label mb-2">// CONTRASEÑA TEMPORAL</div>
        <div className="flex items-center gap-2">
          <code className="flex-1 font-mono text-lg text-cockpit-cyan break-all select-all py-2 px-3 bg-black/40 border border-cockpit-line">
            {revealed ? password : '•'.repeat(password?.length || 12)}
          </code>
          <button
            onClick={() => setRevealed(!revealed)}
            className="hud-btn hud-btn--cyan !px-3 !py-2"
            title={revealed ? 'Ocultar' : 'Mostrar'}
          >
            {revealed ? <EyeOff size={14} /> : <Eye size={14} />}
          </button>
          <button
            onClick={copiar}
            className="hud-btn !px-3 !py-2"
            title="Copiar al portapapeles"
          >
            {copied ? <Check size={14} /> : <Copy size={14} />}
          </button>
        </div>
      </div>

      <div className="flex justify-end">
        <button onClick={onClose} className="hud-btn !px-5 !py-2">
          He guardado la contraseña
        </button>
      </div>
    </HudModal>
  );
}
