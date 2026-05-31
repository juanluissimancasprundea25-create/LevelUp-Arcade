import { useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X } from 'lucide-react';

/**
 * Modal HUD reutilizable. Backdrop oscuro + panel con clip-path.
 * Cierra con Escape o click fuera (si closeOnBackdrop no es false).
 */
export function HudModal({ open, onClose, title, subtitle, children, size = 'md', closeOnBackdrop = true }) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  const widths = {
    sm: 'max-w-md',
    md: 'max-w-2xl',
    lg: 'max-w-4xl',
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          onClick={() => closeOnBackdrop && onClose()}
        >
          {/* Backdrop */}
          <div className="absolute inset-0 bg-black/75 backdrop-blur-sm" />

          {/* Panel */}
          <motion.div
            initial={{ opacity: 0, scale: 0.92, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
            onClick={(e) => e.stopPropagation()}
            className={`hud-panel relative w-full ${widths[size]} max-h-[90vh] flex flex-col`}
          >
            {/* Cabecera */}
            <div className="flex items-start justify-between p-5 border-b border-cockpit-line">
              <div>
                <p className="hud-label text-cockpit-cyan/70">// {subtitle || 'CONSOLA'}</p>
                <h2 className="mt-1 font-display text-xl tracking-wider text-white">
                  {title}
                </h2>
              </div>
              <button
                onClick={onClose}
                className="p-2 text-slate-400 hover:text-cockpit-danger transition-colors"
                aria-label="Cerrar"
              >
                <X size={20} />
              </button>
            </div>

            {/* Cuerpo (scroll si pasa de la altura) */}
            <div className="flex-1 overflow-y-auto p-5">
              {children}
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
