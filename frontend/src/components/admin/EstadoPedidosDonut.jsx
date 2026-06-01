import { useMemo } from 'react';
import { motion } from 'framer-motion';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

/**
 * Donut de distribución de pedidos por estado.
 * Espera `pedidos: PedidoResponse[]` y agrega por `estado`.
 */
const COLOR_ESTADO = {
  PENDIENTE: '#fbbf24',  // ámbar
  PAGADO:    '#22d3ee',  // cyan
  ENVIADO:   '#a855f7',  // neón
  ENTREGADO: '#34d399',  // verde
  CANCELADO: '#ef4444',  // rojo
};

export function EstadoPedidosDonut({ pedidos, loading }) {
  const data = useMemo(() => {
    if (!pedidos) return [];
    const counts = pedidos.reduce((acc, p) => {
      acc[p.estado] = (acc[p.estado] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(counts).map(([name, value]) => ({ name, value }));
  }, [pedidos]);

  const total = data.reduce((s, d) => s + d.value, 0);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5, delay: 0.2 }}
      className="hud-panel p-5"
    >
      <div className="flex items-center justify-between mb-3">
        <p className="hud-label">// PEDIDOS POR ESTADO</p>
        <span className="hud-readout">{total} total</span>
      </div>

      {loading ? (
        <div className="h-56 bg-white/5 animate-pulse rounded" />
      ) : total === 0 ? (
        <div className="h-56 grid place-items-center text-slate-500 text-xs tracking-widest">
          SIN DATOS
        </div>
      ) : (
        <div className="h-56 relative">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                dataKey="value"
                innerRadius={55}
                outerRadius={85}
                paddingAngle={2}
                stroke="none"
              >
                {data.map((d) => (
                  <Cell key={d.name} fill={COLOR_ESTADO[d.name] || '#888'} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  background: 'rgba(8,10,24,0.95)',
                  border: '1px solid rgba(168,85,247,0.5)',
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: 11,
                }}
                itemStyle={{ color: '#fff' }}
              />
            </PieChart>
          </ResponsiveContainer>
          {/* Total en el centro del donut */}
          <div className="absolute inset-0 grid place-items-center pointer-events-none">
            <div className="text-center">
              <div className="font-display text-3xl text-white">{total}</div>
              <div className="hud-label text-cockpit-cyan/70">pedidos</div>
            </div>
          </div>
        </div>
      )}

      {/* Leyenda */}
      <div className="mt-4 grid grid-cols-2 gap-1.5 text-[10px] tracking-wider">
        {data.map((d) => (
          <div key={d.name} className="flex items-center gap-2">
            <span
              className="w-2 h-2 rounded-full"
              style={{ background: COLOR_ESTADO[d.name] || '#888' }}
            />
            <span className="text-slate-300 uppercase">{d.name}</span>
            <span className="ml-auto font-mono text-slate-400">{d.value}</span>
          </div>
        ))}
      </div>
    </motion.div>
  );
}
