import { useMemo } from 'react';
import { motion } from 'framer-motion';
import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

/**
 * Top 6 categorías por número de productos. Agrega productos por
 * categoriaNombre.
 */
export function CategoriasBar({ productos, loading }) {
  const data = useMemo(() => {
    if (!productos) return [];
    const counts = productos.reduce((acc, p) => {
      const nombre = p.categoriaNombre || 'Sin categoría';
      acc[nombre] = (acc[nombre] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(counts)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 6);
  }, [productos]);

  const colores = ['#a855f7', '#22d3ee', '#34d399', '#fbbf24', '#ec4899', '#60a5fa'];

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.25 }}
      className="hud-panel p-5"
    >
      <div className="flex items-center justify-between mb-3">
        <p className="hud-label">// TOP CATEGORÍAS</p>
        <span className="hud-readout">por nº productos</span>
      </div>

      {loading ? (
        <div className="h-52 bg-white/5 animate-pulse rounded" />
      ) : data.length === 0 ? (
        <div className="h-52 grid place-items-center text-slate-500 text-xs tracking-widest">
          SIN DATOS
        </div>
      ) : (
        <div className="h-52">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} layout="vertical" margin={{ left: 10, right: 18 }}>
              <XAxis type="number" hide />
              <YAxis
                type="category"
                dataKey="name"
                width={90}
                tick={{ fill: '#cbd5e1', fontSize: 10, fontFamily: 'JetBrains Mono' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                cursor={{ fill: 'rgba(168,85,247,0.08)' }}
                contentStyle={{
                  background: 'rgba(8,10,24,0.95)',
                  border: '1px solid rgba(168,85,247,0.5)',
                  fontFamily: 'JetBrains Mono, monospace',
                  fontSize: 11,
                }}
                formatter={(v) => [`${v} productos`, '']}
              />
              <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                {data.map((_, i) => (
                  <Cell key={i} fill={colores[i % colores.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </motion.div>
  );
}
