import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Search, X, Package, Filter, ArrowDownAZ, ArrowDown01, ArrowUp01 } from 'lucide-react';
import { api } from '../lib/api.js';
import { useFetch } from '../lib/hooks.js';
import { HeaderTienda } from '../components/tienda/HeaderTienda.jsx';
import { ProductoCard } from '../components/tienda/ProductoCard.jsx';

/**
 * Catálogo público de la tienda. URL: /cockpit/tienda
 *
 *  - Usa endpoints públicos (/api/publico/productos y /api/publico/categorias)
 *  - Filtros: búsqueda libre + categoría
 *  - Orden: por nombre, precio ascendente o descendente
 *  - Grid responsive (1/2/3/4 columnas)
 */

const ORDENES = {
  nombre:    { label: 'Nombre A-Z', icon: ArrowDownAZ, fn: (a, b) => a.nombre.localeCompare(b.nombre) },
  precioAsc: { label: 'Precio ↑',    icon: ArrowDown01, fn: (a, b) => Number(a.precio) - Number(b.precio) },
  precioDesc:{ label: 'Precio ↓',    icon: ArrowUp01,   fn: (a, b) => Number(b.precio) - Number(a.precio) },
};

export default function Tienda() {
  const [busqueda, setBusqueda] = useState('');
  const [catId, setCatId] = useState('');
  const [orden, setOrden] = useState('nombre');

  // Endpoints públicos
  const productos  = useFetch('/publico/productos');
  const categorias = useFetch('/publico/categorias');

  const filtrados = useMemo(() => {
    if (!productos.data) return [];
    let arr = productos.data;
    if (catId) arr = arr.filter(p => p.categoriaId === Number(catId));
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(p =>
        (p.sku || '').toLowerCase().includes(q) ||
        (p.nombre || '').toLowerCase().includes(q) ||
        (p.descripcion || '').toLowerCase().includes(q)
      );
    }
    return [...arr].sort(ORDENES[orden].fn);
  }, [productos.data, busqueda, catId, orden]);

  function limpiarFiltros() {
    setBusqueda('');
    setCatId('');
  }

  const hayFiltros = busqueda || catId;

  return (
    <div className="min-h-screen flex flex-col">
      <HeaderTienda />

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 py-6">
        {/* Hero pequeño */}
        <motion.div
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-6"
        >
          <p className="hud-label text-cockpit-cyan/70">// CATÁLOGO</p>
          <h1 className="font-display text-3xl xl:text-4xl tracking-wider text-white mt-1">
            Tienda LevelUp Arcade
          </h1>
          <p className="text-sm text-slate-400 mt-2 max-w-2xl">
            Videojuegos, merchandising y coleccionables. Compra ahora con envío en 24-72h.
          </p>
        </motion.div>

        {/* Filtros */}
        <motion.div
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.05 }}
          className="hud-panel p-4 mb-5"
        >
          <div className="grid grid-cols-1 md:grid-cols-[1fr_auto_auto] gap-3">
            {/* Buscador */}
            <div className="relative">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              <input
                type="text"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                placeholder="Buscar producto…"
                className="hud-input !pl-9"
              />
              {busqueda && (
                <button
                  onClick={() => setBusqueda('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
                  aria-label="Limpiar"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            {/* Categoría */}
            <select
              value={catId}
              onChange={(e) => setCatId(e.target.value)}
              className="hud-input"
            >
              <option value="">Todas las categorías</option>
              {(categorias.data || []).map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>

            {/* Orden */}
            <select
              value={orden}
              onChange={(e) => setOrden(e.target.value)}
              className="hud-input"
            >
              {Object.entries(ORDENES).map(([k, v]) => (
                <option key={k} value={k}>{v.label}</option>
              ))}
            </select>
          </div>

          <div className="mt-3 flex items-center justify-between gap-3 flex-wrap">
            <p className="hud-readout text-slate-500 text-[10px]">
              {productos.loading
                ? '// CARGANDO CATÁLOGO…'
                : `// ${filtrados.length} de ${productos.data?.length || 0} productos`}
            </p>
            {hayFiltros && (
              <button
                onClick={limpiarFiltros}
                className="text-[10px] tracking-[0.3em] uppercase text-cockpit-cyan hover:text-white"
              >
                <Filter size={10} className="inline mr-1" />
                Quitar filtros
              </button>
            )}
          </div>
        </motion.div>

        {/* Grid de productos */}
        {productos.loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="hud-panel">
                <div className="aspect-square bg-white/5 animate-pulse" />
                <div className="p-3 space-y-2">
                  <div className="h-3 bg-white/5 animate-pulse rounded" />
                  <div className="h-3 w-1/2 bg-white/5 animate-pulse rounded" />
                </div>
              </div>
            ))}
          </div>
        ) : filtrados.length === 0 ? (
          <div className="hud-panel p-10 text-center">
            <Package size={42} className="mx-auto mb-3 opacity-30 text-slate-500" />
            <p className="hud-label text-slate-500">
              {productos.data?.length === 0
                ? '// EL CATÁLOGO ESTÁ VACÍO POR EL MOMENTO'
                : '// NINGÚN PRODUCTO COINCIDE CON ESOS FILTROS'}
            </p>
          </div>
        ) : (
          <motion.div
            initial="hidden"
            animate="show"
            variants={{
              hidden: {},
              show: { transition: { staggerChildren: 0.03 } },
            }}
            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4"
          >
            {filtrados.map((p) => (
              <motion.div
                key={p.id}
                variants={{
                  hidden: { opacity: 0, y: 12 },
                  show:   { opacity: 1, y: 0 },
                }}
              >
                <ProductoCard producto={p} />
              </motion.div>
            ))}
          </motion.div>
        )}
      </main>
    </div>
  );
}
