import { motion } from 'framer-motion';
import { Construction } from 'lucide-react';

/**
 * Placeholder para módulos que aún no están construidos.
 * Se usa para Productos, Clientes, etc. hasta sus fases respectivas.
 */
export function ModulePlaceholder({ titulo, fase }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="pb-8"
    >
      <header className="mb-6 pt-2">
        <p className="hud-label text-cockpit-cyan/70">// MÓDULO</p>
        <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
          {titulo}
        </h1>
      </header>

      <div className="hud-panel p-10 text-center">
        <Construction size={48} className="mx-auto mb-4 text-cockpit-amber opacity-70" />
        <p className="hud-label text-cockpit-amber">// EN CONSTRUCCIÓN</p>
        <h2 className="mt-2 font-display text-xl tracking-wider text-white">
          Consola sin alimentación
        </h2>
        <p className="mt-3 text-slate-400 text-sm max-w-md mx-auto">
          Este módulo se activará en la <span className="text-cockpit-neon">{fase}</span>.
          De momento puedes seguir usando la versión clásica en
          <code className="mx-1 px-1.5 py-0.5 bg-white/5 text-cockpit-cyan font-mono text-xs">
            /admin
          </code>
          mientras migramos.
        </p>
      </div>
    </motion.div>
  );
}
