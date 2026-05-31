import { createContext, useCallback, useContext, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { CheckCircle, AlertTriangle, XCircle, Info } from 'lucide-react';

/**
 * Sistema mínimo de toasts con React Context. Sin librerías externas.
 * Uso:
 *   const toast = useToast();
 *   toast.ok('Guardado');
 *   toast.err('Algo fallo');
 */
const ToastCtx = createContext(null);

let nextId = 1;

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);

  const dismiss = useCallback((id) => {
    setItems((arr) => arr.filter((t) => t.id !== id));
  }, []);

  const push = useCallback((type, msg, ttl = 3500) => {
    const id = nextId++;
    setItems((arr) => [...arr, { id, type, msg }]);
    setTimeout(() => dismiss(id), ttl);
  }, [dismiss]);

  const api = {
    ok:   (m) => push('ok', m),
    err:  (m) => push('err', m, 5000),
    warn: (m) => push('warn', m),
    info: (m) => push('info', m),
  };

  return (
    <ToastCtx.Provider value={api}>
      {children}
      <div className="fixed top-4 right-4 z-[100] space-y-2 pointer-events-none">
        <AnimatePresence>
          {items.map((t) => (
            <motion.div
              key={t.id}
              initial={{ opacity: 0, x: 30 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 30 }}
              transition={{ duration: 0.25 }}
              className={[
                'hud-panel px-4 py-3 flex items-center gap-3 max-w-sm pointer-events-auto',
                t.type === 'ok'   && 'border-cockpit-ok/60',
                t.type === 'err'  && 'border-cockpit-danger/60',
                t.type === 'warn' && 'border-cockpit-amber/60',
                t.type === 'info' && 'border-cockpit-cyan/60',
              ].filter(Boolean).join(' ')}
              style={{
                boxShadow:
                  t.type === 'ok'   ? '0 0 24px rgba(52,211,153,0.35)'  :
                  t.type === 'err'  ? '0 0 24px rgba(239,68,68,0.35)'   :
                  t.type === 'warn' ? '0 0 24px rgba(251,191,36,0.35)'  :
                                      '0 0 24px rgba(34,211,238,0.35)',
              }}
            >
              {t.type === 'ok'   && <CheckCircle    size={18} className="text-cockpit-ok shrink-0" />}
              {t.type === 'err'  && <XCircle        size={18} className="text-cockpit-danger shrink-0" />}
              {t.type === 'warn' && <AlertTriangle  size={18} className="text-cockpit-amber shrink-0" />}
              {t.type === 'info' && <Info           size={18} className="text-cockpit-cyan shrink-0" />}
              <span className="text-sm text-slate-100">{t.msg}</span>
              <button
                onClick={() => dismiss(t.id)}
                className="ml-auto text-slate-500 hover:text-white"
                aria-label="Cerrar"
              >
                ×
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastCtx);
  if (!ctx) {
    // Fallback: console (evita romper si alguien lo usa fuera de provider)
    return {
      ok: console.log, err: console.error,
      warn: console.warn, info: console.info,
    };
  }
  return ctx;
}
