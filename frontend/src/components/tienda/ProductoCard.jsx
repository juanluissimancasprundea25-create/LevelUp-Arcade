import { Link } from 'react-router-dom';
import { useState } from 'react';
import { ImageOff, ShoppingCart, Plus } from 'lucide-react';
import { useCarrito } from '../../lib/carrito.js';
import { useToast } from '../admin/Toast.jsx';

/**
 * Tarjeta de producto del catálogo.
 *  - Click en la imagen/título → ficha del producto
 *  - Botón "+" añade directamente al carrito (1 unidad)
 *  - Si stock = 0, deshabilita el botón
 */
export function ProductoCard({ producto }) {
  const [imgError, setImgError] = useState(false);
  const { añadir } = useCarrito();
  const toast = useToast();

  const sinStock = !producto.stock || producto.stock <= 0;
  const stockBajo = producto.bajoStock && !sinStock;

  function añadirRapido(e) {
    e.preventDefault();
    e.stopPropagation();
    if (sinStock) return;
    añadir(producto, 1);
    toast.ok(`"${producto.nombre}" añadido al carrito`);
  }

  return (
    <Link
      to={`/cockpit/tienda/producto/${producto.id}`}
      className="group hud-panel overflow-hidden flex flex-col transition-all hover:border-cockpit-neon/60"
      style={{ position: 'relative' }}
    >
      {/* Imagen */}
      <div className="aspect-square bg-black/40 border-b border-cockpit-line/40 grid place-items-center overflow-hidden relative">
        {producto.imagenUrl && !imgError ? (
          <img
            src={producto.imagenUrl}
            alt={producto.nombre}
            onError={() => setImgError(true)}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          />
        ) : (
          <ImageOff size={36} className="text-slate-700" />
        )}

        {/* Categoría chip arriba izquierda */}
        {producto.categoriaNombre && (
          <span className="absolute top-2 left-2 px-1.5 py-0.5 text-[9px] tracking-widest uppercase
                           border border-cockpit-neon/40 text-cockpit-neon
                           bg-cockpit-bg/80 backdrop-blur-sm">
            {producto.categoriaNombre}
          </span>
        )}

        {/* Badge stock arriba derecha */}
        {sinStock ? (
          <span className="absolute top-2 right-2 px-1.5 py-0.5 text-[9px] tracking-widest uppercase
                           border border-cockpit-danger/60 text-cockpit-danger
                           bg-cockpit-bg/80 backdrop-blur-sm">
            Agotado
          </span>
        ) : stockBajo ? (
          <span className="absolute top-2 right-2 px-1.5 py-0.5 text-[9px] tracking-widest uppercase
                           border border-cockpit-amber/60 text-cockpit-amber
                           bg-cockpit-bg/80 backdrop-blur-sm">
            Últimas {producto.stock}
          </span>
        ) : null}
      </div>

      {/* Info */}
      <div className="p-3 flex-1 flex flex-col gap-2">
        <h3 className="text-sm text-white line-clamp-2 group-hover:text-cockpit-neon transition-colors">
          {producto.nombre}
        </h3>
        <p className="hud-readout text-slate-500 text-[10px]">{producto.sku}</p>

        <div className="mt-auto pt-2 flex items-center justify-between gap-2">
          <span className="font-display text-lg text-cockpit-cyan">
            {Number(producto.precio).toFixed(2)} €
          </span>
          <button
            onClick={añadirRapido}
            disabled={sinStock}
            className="hud-btn !px-2.5 !py-1.5 !text-[10px] disabled:opacity-30 disabled:cursor-not-allowed"
            title={sinStock ? 'Sin stock' : 'Añadir al carrito'}
          >
            {sinStock ? <ShoppingCart size={12} /> : <Plus size={12} />}
          </button>
        </div>
      </div>
    </Link>
  );
}
