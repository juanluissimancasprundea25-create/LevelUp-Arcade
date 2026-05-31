import { Construction } from 'lucide-react';

/**
 * Placeholder reutilizable para módulos de "Mi cuenta" que se completarán
 * en las siguientes sub-fases (7c, 7d).
 */
export function MiCuentaPlaceholder({ titulo, fase }) {
  return (
    <div className="hud-panel p-10 text-center">
      <Construction size={42} className="mx-auto mb-3 opacity-30 text-cockpit-amber" />
      <p className="hud-label text-cockpit-amber mb-1">// EN CONSTRUCCIÓN</p>
      <h3 className="font-display text-xl text-white mb-2">{titulo}</h3>
      <p className="text-xs text-slate-500">
        Disponible próximamente en la <span className="text-cockpit-neon">{fase}</span>.
      </p>
    </div>
  );
}
