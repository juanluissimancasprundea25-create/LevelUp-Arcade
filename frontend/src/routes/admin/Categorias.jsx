import { useMemo, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, Search, RefreshCw, Edit, Trash2, Tag, X, Package } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useAuth } from '../../lib/auth.jsx';
import { useToast } from '../../components/admin/Toast.jsx';
import { CategoriaFormModal } from '../../components/admin/CategoriaFormModal.jsx';
import { ConfirmModal } from '../../components/admin/ConfirmModal.jsx';

/**
 * Consola de Categorías.
 *  - Grid de tarjetas HUD (no tabla — suelen ser pocas)
 *  - Crear / Editar / Borrar (solo ADMIN)
 *  - Cuenta cuántos productos hay en cada una (usando /productos)
 */
export default function Categorias() {
  const { user } = useAuth();
  const esAdmin = user?.rol === 'ADMIN';
  const toast = useToast();

  const [busqueda, setBusqueda] = useState('');
  const [editando, setEditando] = useState(null);     // null | {} crear | {categoria} editar
  const [borrar, setBorrar] = useState(null);
  const [borrando, setBorrando] = useState(false);

  const categorias = useFetch('/categorias');
  const productos  = useFetch('/productos');

  // Mapa id-categoria -> nº productos
  const conteoProductos = useMemo(() => {
    if (!productos.data) return {};
    return productos.data.reduce((acc, p) => {
      if (p.categoriaId != null) acc[p.categoriaId] = (acc[p.categoriaId] || 0) + 1;
      return acc;
    }, {});
  }, [productos.data]);

  // Filtrado client-side por nombre/descripción
  const filtradas = useMemo(() => {
    if (!categorias.data) return [];
    const q = busqueda.trim().toLowerCase();
    if (!q) return categorias.data;
    return categorias.data.filter(c =>
      (c.nombre || '').toLowerCase().includes(q) ||
      (c.descripcion || '').toLowerCase().includes(q)
    );
  }, [categorias.data, busqueda]);

  async function confirmarBorrado() {
    if (!borrar || borrando) return;
    const enUso = conteoProductos[borrar.id] || 0;
    if (enUso > 0) {
      toast.warn(`No se puede eliminar: ${enUso} productos la usan. Marca como Inactiva.`);
      setBorrar(null);
      return;
    }
    setBorrando(true);
    try {
      await api.delete(`/categorias/${borrar.id}`);
      toast.ok(`"${borrar.nombre}" eliminada`);
      setBorrar(null);
      categorias.refresh();
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo eliminar');
    } finally {
      setBorrando(false);
    }
  }

  return (
    <div className="pb-8">
      {/* Cabecera */}
      <motion.header
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex items-end justify-between mb-5 pt-2 gap-4 flex-wrap"
      >
        <div>
          <p className="hud-label text-cockpit-cyan/70">// MÓDULO</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Categorías
          </h1>
          <p className="hud-readout mt-1">
            {categorias.loading
              ? 'Sincronizando…'
              : `${filtradas.length} de ${categorias.data?.length || 0} categorías`}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => { categorias.refresh(); productos.refresh(); }}
            className="hud-btn hud-btn--cyan !px-3 !py-2"
            disabled={categorias.loading}
          >
            <RefreshCw size={14} className={categorias.loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refrescar</span>
          </button>
          {esAdmin && (
            <button onClick={() => setEditando({})} className="hud-btn !px-4 !py-2">
              <Plus size={14} /> Nueva categoría
            </button>
          )}
        </div>
      </motion.header>

      {/* Buscador */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="hud-panel p-4 mb-5"
      >
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar nombre o descripción…"
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
      </motion.div>

      {/* Grid de tarjetas */}
      {categorias.loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="hud-panel p-6 h-40">
              <div className="h-6 w-32 bg-white/5 animate-pulse rounded mb-3" />
              <div className="h-3 w-full bg-white/5 animate-pulse rounded mb-2" />
              <div className="h-3 w-2/3 bg-white/5 animate-pulse rounded" />
            </div>
          ))}
        </div>
      ) : filtradas.length === 0 ? (
        <div className="hud-panel p-10 text-center">
          <Tag size={42} className="mx-auto mb-3 opacity-30 text-slate-500" />
          <p className="hud-label text-slate-500">
            {categorias.data?.length === 0
              ? '// NO HAY CATEGORÍAS · CREA LA PRIMERA'
              : '// NINGÚN RESULTADO'}
          </p>
        </div>
      ) : (
        <motion.div
          initial="hidden"
          animate="show"
          variants={{
            hidden: {},
            show: { transition: { staggerChildren: 0.04 } },
          }}
          className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"
        >
          <AnimatePresence>
            {filtradas.map((c) => (
              <CategoriaCard
                key={c.id}
                categoria={c}
                numProductos={conteoProductos[c.id] || 0}
                esAdmin={esAdmin}
                onEditar={() => setEditando({ categoria: c })}
                onBorrar={() => setBorrar(c)}
              />
            ))}
          </AnimatePresence>
        </motion.div>
      )}

      {/* Modales */}
      <CategoriaFormModal
        open={!!editando}
        categoria={editando?.categoria}
        onClose={() => setEditando(null)}
        onSaved={() => categorias.refresh()}
      />
      <ConfirmModal
        open={!!borrar}
        onClose={() => setBorrar(null)}
        onConfirm={confirmarBorrado}
        title={`¿Eliminar "${borrar?.nombre}"?`}
        message={
          (conteoProductos[borrar?.id] || 0) > 0
            ? `Esta categoría tiene ${conteoProductos[borrar?.id]} productos asociados. No se podrá eliminar — márcala como Inactiva en su lugar.`
            : 'Esta acción es permanente.'
        }
        busy={borrando}
      />
    </div>
  );
}

/* ---------- Tarjeta de categoría ---------- */
function CategoriaCard({ categoria: c, numProductos, esAdmin, onEditar, onBorrar }) {
  return (
    <motion.div
      variants={{
        hidden: { opacity: 0, y: 14 },
        show:   { opacity: 1, y: 0 },
      }}
      exit={{ opacity: 0, scale: 0.96 }}
      transition={{ duration: 0.3 }}
      className="hud-panel p-5 relative group"
      style={
        !c.activa
          ? { opacity: 0.55 }
          : { boxShadow: '0 0 0 1px rgba(168,85,247,0.1) inset, 0 0 20px rgba(168,85,247,0.1)' }
      }
    >
      {/* Icono + estado */}
      <div className="flex items-start justify-between mb-3">
        <div className="p-2 border border-cockpit-line">
          <Tag size={18} className="text-cockpit-neon" />
        </div>
        <span
          className={[
            'px-2 py-0.5 text-[9px] tracking-widest uppercase',
            c.activa
              ? 'border border-cockpit-ok/40 text-cockpit-ok'
              : 'border border-slate-600 text-slate-500',
          ].join(' ')}
        >
          {c.activa ? 'Activa' : 'Inactiva'}
        </span>
      </div>

      <h3 className="font-display text-lg tracking-wide text-white">
        {c.nombre}
      </h3>

      {c.descripcion ? (
        <p className="mt-2 text-xs text-slate-400 line-clamp-3" title={c.descripcion}>
          {c.descripcion}
        </p>
      ) : (
        <p className="mt-2 text-xs text-slate-600 italic">sin descripción</p>
      )}

      {/* Contador productos */}
      <div className="mt-4 flex items-center justify-between pt-3 border-t border-cockpit-line/40">
        <div className="flex items-center gap-1.5 text-cockpit-cyan">
          <Package size={12} />
          <span className="hud-readout">
            {numProductos} {numProductos === 1 ? 'producto' : 'productos'}
          </span>
        </div>

        {esAdmin && (
          <div className="opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
            <button
              onClick={onEditar}
              className="p-1.5 text-cockpit-neon hover:text-white hover:bg-cockpit-neon/10 transition-colors"
              title="Editar"
            >
              <Edit size={14} />
            </button>
            <button
              onClick={onBorrar}
              className="p-1.5 text-slate-500 hover:text-cockpit-danger hover:bg-cockpit-danger/10 transition-colors"
              title="Eliminar"
            >
              <Trash2 size={14} />
            </button>
          </div>
        )}
      </div>
    </motion.div>
  );
}
