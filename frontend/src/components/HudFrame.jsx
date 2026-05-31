import { motion } from 'framer-motion';

/**
 * Marco HUD que vive encima del Canvas 3D. Esquinas de targeting,
 * scanline, readouts laterales. Cada ruta lo usa para mantener
 * consistencia visual.
 */
export function HudFrame({ children, title, subtitle, telemetry }) {
  return (
    <div className="relative w-full h-full pointer-events-none">
      {/* Esquinas de targeting */}
      <Corner pos="top-left" />
      <Corner pos="top-right" />
      <Corner pos="bottom-left" />
      <Corner pos="bottom-right" />

      {/* Header HUD */}
      <motion.div
  initial={{ opacity: 0, y: -10 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.5, delay: 0.1 }}
  className="absolute top-6 left-12 md:left-16 text-left select-none"
>
  {title && (
    <h1 className="font-display tracking-[0.45em] text-cockpit-neon text-sm md:text-base">
      {title}
    </h1>
  )}
  {subtitle && (
    <p className="mt-1 text-[10px] tracking-[0.3em] text-cockpit-cyan/70 uppercase">
      {subtitle}
    </p>
  )}
</motion.div>

      {/* Telemetría lateral izquierda */}
      {telemetry && (
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="absolute top-1/2 left-6 -translate-y-1/2 space-y-3 hidden xl:block"
        >
          {telemetry.map((row, i) => (
            <div key={i} className="flex items-center gap-2">
              <span className="w-1 h-3 bg-cockpit-cyan animate-pulse" />
              <span className="hud-label">{row.label}</span>
              <span className="hud-readout">{row.value}</span>
            </div>
          ))}
        </motion.div>
      )}

      {/* Contenido principal — recibe pointer-events */}
      <div className="absolute inset-0 grid place-items-center pointer-events-auto">
        {children}
      </div>

      {/* Footer estado */}
      <div className="absolute bottom-4 right-12 md:right-16 text-[10px] tracking-[0.4em] text-cockpit-neon/50 select-none">
        LVL UP // ARCADE.SYS // v2.0
      </div>
    </div>
  );
}

function Corner({ pos }) {
  const map = {
    'top-left':     { className: 'top-4 left-4',     d: 'M0 18 L0 0 L18 0' },
    'top-right':    { className: 'top-4 right-4',    d: 'M0 0 L18 0 L18 18' },
    'bottom-left':  { className: 'bottom-4 left-4',  d: 'M0 0 L0 18 L18 18' },
    'bottom-right': { className: 'bottom-4 right-4', d: 'M0 18 L18 18 L18 0' },
  };
  const { className, d } = map[pos];
  return (
    <svg
      className={`absolute ${className} pointer-events-none`}
      width="22" height="22" viewBox="0 0 22 22" fill="none"
    >
      <path d={d} stroke="#a855f7" strokeWidth="1.5"
            style={{ filter: 'drop-shadow(0 0 4px #a855f7)' }} />
    </svg>
  );
}
