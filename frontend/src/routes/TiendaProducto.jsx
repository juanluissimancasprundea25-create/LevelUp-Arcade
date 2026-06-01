import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft, ImageOff, ShoppingCart, Minus, Plus,
  Tag, Package, Truck, ShieldCheck, AlertCircle,
} from 'lucide-react';
import { api } from '../lib/api.js';
import { useCarrito } from '../lib/carrito.js';
import { useToast } from '../components/admin/Toast.jsx';
import { HeaderTienda } from '../components/tienda/HeaderTienda.jsx';

/**
 * Ficha de detalle de un producto público. URL: /cockpit/tienda/producto/:id
 *
 *  - Layout dos columnas (imagen grande izq, info dcha)
 *  - Selector de cantidad con +/- limitado por stock
 *  - Botón principal "Añadir al carrito"
 *  - Bloque de información (envío, devoluciones, stock)
 */
export default function TiendaProducto() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [producto, setProducto] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cantidad, setCantidad] = useState(1);
  const [imgError, setImgError] = useState(false);
  const { añadir } = useCarrito();
  const toast = useToast();

  useEffect(() => {
    let cancelado = false;
    async function cargar() {
      setLoading(true);
      setError(null);
      try {
        const { data } = await api.get(`/publico/productos/${id}`);
        if (!cancelado) setProducto(data);
      } catch (e) {
        if (!cancelado) setError(e.response?.status === 404
          ? 'Este producto no existe o ya no está disponible'
          : 'No se pudo cargar el producto');
      } finally {
        if (!cancelado) setLoading(false);
      }
    }
    cargar();
    return () => { cancelado = true; };
  }, [id]);

  function añadirAlCarrito() {
    if (!producto || cantidad <= 0) return;
    añadir(producto, cantidad);
    toast.ok(`${cantidad} × "${producto.nombre}" añadidos al carrito`);
  }

  const sinStock  = producto && (!producto.stock || producto.stock <= 0);
  const maxCant   = Math.max(0, producto?.stock || 0);

  return (
    <div className="min-h-screen flex flex-col">
      <HeaderTienda />

      <main className="flex-1 max-w-6xl mx-auto w-full px-4 sm:px-6 py-6">
        {/* Migas */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mb-4"
        >
          <Link
            to="/cockpit/tienda"
            className="inline-flex items-center gap-1.5 text-xs tracking-widest uppercase text-cockpit-cyan hover:text-white transition-colors"
          >
            <ArrowLeft size={12} />
            Volver al catálogo
          </Link>
        </motion.div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="aspect-square bg-white/5 animate-pulse hud-panel" />
            <div className="space-y-3">
              <div className="h-8 w-3/4 bg-white/5 animate-pulse rounded" />
              <div className="h-4 w-1/3 bg-white/5 animate-pulse rounded" />
              <div className="h-24 bg-white/5 animate-pulse rounded mt-6" />
            </div>
          </div>
        ) : error ? (
          <div className="hud-panel p-10 text-center">
            <AlertCircle size={42} className="mx-auto mb-3 opacity-50 text-cockpit-danger" />
            <p className="text-slate-300">{error}</p>
            <button onClick={() => navigate('/cockpit/tienda')} className="hud-btn !px-4 !py-2 mt-4">
              Volver al catálogo
            </button>
          </div>
        ) : producto && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className="grid grid-cols-1 md:grid-cols-2 gap-6"
          >
            {/* Imagen */}
            <div className="hud-panel aspect-square overflow-hidden grid place-items-center bg-black/40">
              {producto.imagenUrl && !imgError ? (
                <img
                  src={producto.imagenUrl}
                  alt={producto.nombre}
                  onError={() => setImgError(true)}
                  className="w-full h-full object-cover"
                />
              ) : (
                <ImageOff size={64} className="text-slate-700" />
              )}
            </div>

            {/* Info */}
            <div className="flex flex-col">
              {producto.categoriaNombre && (
                <Link
                  to={`/cockpit/tienda?cat=${producto.categoriaId}`}
                  className="inline-flex items-center gap-1.5 text-xs tracking-widest uppercase text-cockpit-neon mb-2 w-fit hover:text-white transition-colors"
                >
                  <Tag size={11} />
                  {producto.categoriaNombre}
                </Link>
              )}

              <h1 className="font-display text-2xl xl:text-3xl tracking-wide text-white">
                {producto.nombre}
              </h1>
              <p className="hud-readout text-slate-500 text-xs mt-1">SKU · {producto.sku}</p>

              {/* Precio + estado */}
              <div className="mt-5 flex items-end gap-3 flex-wrap">
                <span className="font-display text-4xl text-cockpit-cyan">
                  {Number(producto.precio).toFixed(2)} €
                </span>
                {sinStock ? (
                  <span className="px-2 py-1 text-[10px] tracking-widest uppercase border border-cockpit-danger/60 text-cockpit-danger">
                    Agotado
                  </span>
                ) : producto.bajoStock ? (
                  <span className="px-2 py-1 text-[10px] tracking-widest uppercase border border-cockpit-amber/60 text-cockpit-amber">
                    Solo quedan {producto.stock}
                  </span>
                ) : (
                  <span className="px-2 py-1 text-[10px] tracking-widest uppercase border border-cockpit-ok/60 text-cockpit-ok">
                    En stock
                  </span>
                )}
              </div>

              {/* Descripción */}
              {producto.descripcion && (
                <div className="mt-5 hud-panel p-4 !bg-black/30">
                  <p className="hud-label mb-2">// DESCRIPCIÓN</p>
                  <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-wrap">
                    {producto.descripcion}
                  </p>
                </div>
              )}

              {/* Cantidad + añadir */}
              {!sinStock && (
                <div className="mt-5 flex flex-wrap items-center gap-3">
                  <div className="flex items-center border border-cockpit-line">
                    <button
                      onClick={() => setCantidad(c => Math.max(1, c - 1))}
                      className="px-3 py-2 text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
                      disabled={cantidad <= 1}
                    >
                      <Minus size={14} />
                    </button>
                    <input
                      type="number"
                      min="1"
                      max={maxCant}
                      value={cantidad}
                      onChange={(e) => {
                        const n = Math.max(1, Math.min(maxCant, parseInt(e.target.value) || 1));
                        setCantidad(n);
                      }}
                      className="w-14 bg-transparent text-center font-mono text-white border-0 focus:outline-none"
                    />
                    <button
                      onClick={() => setCantidad(c => Math.min(maxCant, c + 1))}
                      className="px-3 py-2 text-cockpit-cyan hover:text-white hover:bg-white/5 transition-colors"
                      disabled={cantidad >= maxCant}
                    >
                      <Plus size={14} />
                    </button>
                  </div>

                  <button
                    onClick={añadirAlCarrito}
                    className="hud-btn !px-6 !py-3 flex-1 sm:flex-initial"
                  >
                    <ShoppingCart size={14} />
                    Añadir al carrito · {(producto.precio * cantidad).toFixed(2)} €
                  </button>
                </div>
              )}

              {/* Garantías */}
              <div className="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-2">
                <Garantia icon={Truck} label="Envío 24-72h" />
                <Garantia icon={ShieldCheck} label="Pago seguro" />
                <Garantia icon={Package} label="Devolución 14 días" />
              </div>
            </div>
          </motion.div>
        )}
      </main>
    </div>
  );
}

function Garantia({ icon: Icon, label }) {
  return (
    <div className="hud-panel p-3 !bg-black/30 flex items-center gap-2">
      <Icon size={14} className="text-cockpit-cyan shrink-0" />
      <span className="text-xs text-slate-300">{label}</span>
    </div>
  );
}
