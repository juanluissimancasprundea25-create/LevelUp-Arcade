import { useMemo } from 'react';
import { motion } from 'framer-motion';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis } from 'recharts';
import { TrendingUp } from 'lucide-react';

/**
 * Ingresos del mes en curso. Suma los totales de pedidos en estado
 * PAGADO / ENVIADO / ENTREGADO cuya fechaPedido está en el mes actual.
 * Renderiza un mini-AreaChart con la evolución diaria.
 */
const ESTADOS_INGRESO = new Set(['PAGADO', 'ENVIADO', 'ENTREGADO']);

export function IngresosMes({ pedidos, loading }) {
  const { total, daily } = useMemo(() => {
    if (!pedidos) return { total: 0, daily: [] };
    const ahora = new Date();
    const year = ahora.getFullYear();
    const month = ahora.getMonth();
    const diasDelMes = new Date(year, month + 1, 0).getDate();

    // Mapa día(1..31) -> €
    const mapa = new Map();
    for (let d = 1; d <= diasDelMes; d++) mapa.set(d, 0);

    let suma = 0;
    for (const p of pedidos) {
      if (!ESTADOS_INGRESO.has(p.estado)) continue;
      const f = new Date(p.fechaPedido);
      if (f.getFullYear() !== year || f.getMonth() !== month) continue;
      const dia = f.getDate();
      const monto = Number(p.total) || 0;
      mapa.set(dia, (mapa.get(dia) || 0) + monto);
      suma += monto;
    }

    const daily = Array.from(mapa, ([dia, v]) => ({ dia, v: Math.round(v * 100) / 100 }));
    return { total: suma, daily };
  }, [pedidos]);

  const totalFmt = new Intl.NumberFormat('es-ES', {
    style: 'currency', currency: 'EUR', maximumFractionDigits: 0
  }).format(total);

  const mesNombre = new Date().toLocaleString('es-ES', { month: 'long' }).toUpperCase();

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.15 }}
      className="hud-panel p-5"
    >
      <div className="flex items-start justify-between mb-2">
        <div>
          <p className="hud-label">// INGRESOS · {mesNombre}</p>
          {loading ? (
            <div className="mt-2 h-10 w-32 bg-white/5 animate-pulse rounded" />
          ) : (
            <div className="mt-1 font-display text-3xl xl:text-4xl tracking-wider text-cockpit-ok">
              {totalFmt}
            </div>
          )}
        </div>
        <div className="text-cockpit-ok opacity-80">
          <TrendingUp size={22} strokeWidth={1.5} />
        </div>
      </div>

      <div className="h-20 -mx-2 -mb-2">
        {!loading && (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={daily} margin={{ top: 4, right: 8, left: 8, bottom: 0 }}>
              <defs>
                <linearGradient id="ingresosFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#34d399" stopOpacity={0.55} />
                  <stop offset="100%" stopColor="#34d399" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="dia" hide />
              <Tooltip
                contentStyle={{
                  background: 'rgba(8,10,24,0.95)',
                  border: '1px solid rgba(52,211,153,0.5)',
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: 11,
                }}
                labelFormatter={(d) => `Día ${d}`}
                formatter={(v) => [`${v} €`, 'Ingresos']}
              />
              <Area type="monotone" dataKey="v" stroke="#34d399" strokeWidth={1.6}
                    fill="url(#ingresosFill)" />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </motion.div>
  );
}
