import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Plus, Search, RefreshCw, Edit, Trash2, PackagePlus,
  Package, Filter, X, ImageOff,
} from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useAuth } from '../../lib/auth.jsx';
import { useToast } from '../../components/admin/Toast.jsx';
import { ProductoFormModal } from '../../components/admin/ProductoFormModal.jsx';
import { StockAdjustModal } from '../../components/admin/StockAdjustModal.jsx';
import { ConfirmModal } from '../../components/admin/ConfirmModal.jsx';

/**
 * Consola de Productos.
 *
 * Funcionalidad:
 *  - Tabla con todos los productos
 *  - Buscador en vivo (filtra cliente-side por SKU/nombre/descripcion)
 *  - Filtros: categoria, proveedor, "solo stock bajo"
 *  - Crear / Editar / Borrar (solo ADMIN)
 *  - Ajustar stock (solo ADMIN)
 *  - Refrescar manual
 *
 * Soporta query params:
 *  - ?bajoStock=true   -> activa el filtro de stock bajo al cargar
 *  - ?stockId={id}     -> abre el modal de ajuste de stock de ese producto
 *                          (usado desde la alerta de stock del dashboard)
 */
export default function Productos() {
  const { user } = useAuth();
  const esAdmin = user?.rol === 'ADMIN';
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  // Filtros (inicializados desde query params)
  const [busqueda, setBusqueda]   = useState('');
  const [catId, setCatId]         = useState('');
  const [provId, setProvId]       = useState('');
  const [bajoStock, setBajoStock] = useState(
    searchParams.get('bajoStock') === 'true'
  );

  // Modales
  const [editando, setEditando] = useState(null);
  const [stockProd, setStockProd] = useState(null);
  const [borrarProd, setBorrarProd] = useState(null);
  const [borrando, setBorrando] = useState(false);

  // Carga de datos
  const productos   = useFetch('/productos');
  const categorias  = useFetch('/categorias');
  const proveedores = useFetch('/proveedores');

  // Si la URL trae ?stockId=N, abrimos el modal de stock para ese producto
  // en cuanto los productos esten cargados. Luego limpiamos el query param
  // para que un refresco manual no reabra el modal sin razon.
  useEffect(() => {
    const stockId = searchParams.get('stockId');
    if (!stockId) return;
    if (!productos.data) return;

    const id = Number(stockId);
    const objetivo = productos.data.find(p => p.id === id);
    if (objetivo) {
      setStockProd(objetivo);
    } else {
      toast.err('Ese producto ya no esta disponible');
    }
    // Limpia el query param sin recargar la pagina
    const params = new URLSearchParams(searchParams);
    params.delete('stockId');
    setSearchParams(params, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productos.data]);

  // Filtrado client-side
  const filtrados = useMemo(() => {
    if (!productos.data) return [];
    let arr = productos.data;
    if (catId)   arr = arr.filter(p => p.categoriaId === Number(catId));
    if (provId)  arr = arr.filter(p => p.proveedorId === Number(provId));
    if (bajoStock) arr = arr.filter(p => p.bajoStock);
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(p =>
        (p.sku || '').toLowerCase().includes(q) ||
        (p.nombre || '').toLowerCase().includes(q) ||
        (p.descripcion || '').toLowerCase().includes(q)
      );
    }
    return arr;
  }, [productos.data, busqueda, catId, provId, bajoStock]);

  function limpiarFiltros() {
    setBusqueda('');
    setCatId('');
    setProvId('');
    setBajoStock(false);
  }

  async function confirmarBorrado() {
    if (!borrarProd || borrando) return;
    setBorrando(true);
    try {
      await api.delete(`/productos/${borrarProd.id}`);
      toast.ok(`"${borrarProd.nombre}" eliminado`);
      setBorrarProd(null);
      productos.refresh();
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
          <p className="hud-label text-cockpit-cyan/70">// MODULO</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Productos
          </h1>
          <p className="hud-readout mt-1">
            {productos.loading
              ? 'Sincronizando...'
              : `${filtrados.length} de ${productos.data?.length || 0} productos`}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => productos.refresh()}
            className="hud-btn hud-btn--cyan !px-3 !py-2"
            disabled={productos.loading}
          >
            <RefreshCw size={14} className={productos.loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refrescar</span>
          </button>
          {esAdmin && (
            <button onClick={() => setEditando({})} className="hud-btn !px-4 !py-2">
              <Plus size={14} /> Nuevo producto
            </button>
          )}
        </div>
      </motion.header>

      {/* Barra de filtros */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="hud-panel p-4 mb-5"
      >
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          {/* Buscador */}
          <div className="md:col-span-2 relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
            <input
              type="text"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar SKU, nombre o descripcion..."
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

          {/* Categoria */}
          <select
            value={catId}
            onChange={(e) => setCatId(e.target.value)}
            className="hud-input"
          >
            <option value="">Todas las categorias</option>
            {(categorias.data || []).map((c) => (
              <option key={c.id} value={c.id}>{c.nombre}</option>
            ))}
          </select>

          {/* Proveedor */}
          <select
            value={provId}
            onChange={(e) => setProvId(e.target.value)}
            className="hud-input"
          >
            <option value="">Todos los proveedores</option>
            {(proveedores.data || []).map((p) => (
              <option key={p.id} value={p.id}>{p.nombreEmpresa || p.nombre}</option>
            ))}
          </select>
        </div>

        {/* Toggle stock bajo + reset */}
        <div className="mt-3 flex items-center justify-between gap-3">
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={bajoStock}
              onChange={(e) => setBajoStock(e.target.checked)}
              className="accent-cockpit-amber w-4 h-4"
            />
            <span className="hud-label">// SOLO STOCK BAJO</span>
          </label>
          {(busqueda || catId || provId || bajoStock) && (
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

      {/* Tabla */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.15 }}
        className="hud-panel overflow-hidden"
      >
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-cockpit-line">
                <th className="hud-label text-left p-3 w-12"></th>
                <th className="hud-label text-left p-3">SKU</th>
                <th className="hud-label text-left p-3">Nombre</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Categoria</th>
                <th className="hud-label text-right p-3">Precio</th>
                <th className="hud-label text-right p-3">Stock</th>
                <th className="hud-label text-center p-3 hidden lg:table-cell">Estado</th>
                <th className="hud-label text-right p-3">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {productos.loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={8}>
                      <div className="h-8 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtrados.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-10 text-center">
                    <Package size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {productos.data?.length === 0
                        ? '// NO HAY PRODUCTOS . CREA EL PRIMERO'
                        : '// NINGUN RESULTADO CON ESOS FILTROS'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtrados.map((p) => (
                  <ProductoRow
                    key={p.id}
                    producto={p}
                    esAdmin={esAdmin}
                    onEditar={() => setEditando({ producto: p })}
                    onAjustarStock={() => setStockProd(p)}
                    onBorrar={() => setBorrarProd(p)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Modales */}
      <ProductoFormModal
        open={!!editando}
        producto={editando?.producto}
        onClose={() => setEditando(null)}
        onSaved={() => productos.refresh()}
      />
      <StockAdjustModal
        open={!!stockProd}
        producto={stockProd}
        onClose={() => setStockProd(null)}
        onSaved={() => productos.refresh()}
      />
      <ConfirmModal
        open={!!borrarProd}
        onClose={() => setBorrarProd(null)}
        onConfirm={confirmarBorrado}
        title={`Eliminar "${borrarProd?.nombre}"?`}
        message="Esta accion es permanente. Si el producto esta asociado a pedidos antiguos, considera marcarlo como Inactivo en lugar de borrarlo."
        busy={borrando}
      />
    </div>
  );
}

/* ---------- Fila de la tabla ---------- */
function ProductoRow({ producto: p, esAdmin, onEditar, onAjustarStock, onBorrar }) {
  const [imgError, setImgError] = useState(false);

  const stockColor =
    p.bajoStock
      ? (p.stock === 0 ? 'text-cockpit-danger' : 'text-cockpit-amber')
      : 'text-slate-200';

  return (
    <tr className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors">
      {/* Miniatura */}
      <td className="p-3">
        {p.imagenUrl && !imgError ? (
          <img
            src={p.imagenUrl}
            alt={p.nombre}
            className="h-10 w-10 object-cover border border-cockpit-line"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="h-10 w-10 border border-cockpit-line/40 grid place-items-center">
            <ImageOff size={14} className="text-slate-600" />
          </div>
        )}
      </td>

      {/* SKU */}
      <td className="p-3">
        <span className="hud-readout text-slate-300">{p.sku}</span>
      </td>

      {/* Nombre */}
      <td className="p-3">
        <div className="text-sm text-white">{p.nombre}</div>
        {p.descripcion && (
          <div className="text-xs text-slate-500 truncate max-w-xs">
            {p.descripcion.slice(0, 80)}{p.descripcion.length > 80 ? '...' : ''}
          </div>
        )}
      </td>

      {/* Categoria */}
      <td className="p-3 hidden md:table-cell">
        {p.categoriaNombre ? (
          <span className="inline-block px-2 py-0.5 text-[10px] tracking-widest uppercase
                           border border-cockpit-neon/40 text-cockpit-neon">
            {p.categoriaNombre}
          </span>
        ) : (
          <span className="text-slate-600 text-xs">-</span>
        )}
      </td>

      {/* Precio */}
      <td className="p-3 text-right font-mono text-cockpit-cyan">
        {Number(p.precio).toFixed(2)} EUR
      </td>

      {/* Stock */}
      <td className={`p-3 text-right font-mono ${stockColor}`}>
        {p.stock}
        {p.stockMinimo != null && (
          <span className="text-slate-500 text-[10px] block">
            / {p.stockMinimo} min
          </span>
        )}
      </td>

      {/* Estado */}
      <td className="p-3 text-center hidden lg:table-cell">
        <span
          className={[
            'inline-block px-2 py-0.5 text-[10px] tracking-widest uppercase',
            p.activo
              ? 'border border-cockpit-ok/40 text-cockpit-ok'
              : 'border border-slate-600 text-slate-500',
          ].join(' ')}
        >
          {p.activo ? 'Activo' : 'Inactivo'}
        </span>
      </td>

      {/* Acciones */}
      <td className="p-3 text-right whitespace-nowrap">
        {esAdmin && (
          <button
            onClick={onAjustarStock}
            className="p-1.5 mr-1 text-cockpit-cyan hover:text-white hover:bg-cockpit-cyan/10 transition-colors"
            title="Ajustar stock"
          >
            <PackagePlus size={15} />
          </button>
        )}
        <button
          onClick={onEditar}
          className="p-1.5 mr-1 text-cockpit-neon hover:text-white hover:bg-cockpit-neon/10 transition-colors"
          title={esAdmin ? 'Editar' : 'Ver detalle'}
        >
          <Edit size={15} />
        </button>
        {esAdmin && (
          <button
            onClick={onBorrar}
            className="p-1.5 text-slate-500 hover:text-cockpit-danger hover:bg-cockpit-danger/10 transition-colors"
            title="Eliminar"
          >
            <Trash2 size={15} />
          </button>
        )}
      </td>
    </tr>
  );
}
