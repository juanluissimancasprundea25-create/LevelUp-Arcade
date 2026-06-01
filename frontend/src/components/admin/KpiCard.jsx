import { motion } from 'framer-motion';

/**
 * Tarjeta KPI estilo HUD. Acepta:
 *  - label   : pequeño tracking encima ("PRODUCTOS")
 *  - value   : número grande (puede ser string para "12,3 K" etc.)
 *  - delta   : opcional, +12 / -3 / "estable", para tendencia
 *  - icon    : componente lucide
 *  - tone    : 'neutral' | 'warn' | 'danger' | 'ok'
 *  - loading : muestra placeholder de carga
 *  - footer  : línea pequeña opcional debajo (ej. "de 142 totales")
 */
export function KpiCard({ label, value, delta, icon: Icon, tone = 'neutral', loading, footer, delay = 0 }) {
  const toneStyles = {
    neutral: { text: 'text-white',           accent: 'text-cockpit-neon',  glow: 'rgba(168,85,247,0.25)' },
    ok:      { text: 'text-cockpit-ok',      accent: 'text-cockpit-ok',    glow: 'rgba(52,211,153,0.3)' },
    warn:    { text: 'text-cockpit-amber',   accent: 'text-cockpit-amber', glow: 'rgba(251,191,36,0.35)' },
    danger:  { text: 'text-cockpit-danger',  accent: 'text-cockpit-danger',glow: 'rgba(239,68,68,0.35)' },
  }[tone];

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45, delay, ease: 'easeOut' }}
      className="hud-panel p-5 relative overflow-hidden"
      style={{ boxShadow: `0 0 0 1px ${toneStyles.glow} inset, 0 0 28px ${toneStyles.glow}` }}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="hud-label">{label}</p>
          {loading ? (
            <div className="mt-2 h-9 w-24 bg-white/5 animate-pulse rounded" />
          ) : (
            <div className={`mt-1 font-display text-3xl xl:text-4xl tracking-wider ${toneStyles.text}`}>
              {value}
            </div>
          )}
          {footer && !loading && (
            <p className="mt-1 hud-readout opacity-80">{footer}</p>
          )}
        </div>
        {Icon && (
          <div className={`p-2 ${toneStyles.accent} opacity-80`}>
            <Icon size={22} strokeWidth={1.5} />
          </div>
        )}
      </div>

      {delta != null && !loading && (
        <div className={`absolute bottom-2 right-3 text-[10px] tracking-widest font-mono ${toneStyles.accent}`}>
          {delta}
        </div>
      )}
    </motion.div>
  );
}
