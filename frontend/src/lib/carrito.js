import { useEffect, useState, useCallback } from 'react';

/**
 * Hook para el carrito de la tienda.
 *
 * Persiste en sessionStorage (se vacía al cerrar el navegador, lo cual
 * tiene sentido para una sesión de compra). Si quieres carrito persistente
 * entre sesiones, cambia a localStorage.
 *
 * Estructura del item:
 *   { productoId, nombre, sku, precio, imagenUrl, cantidad }
 *
 * El estado se sincroniza entre pestañas vía evento "storage" + un
 * evento custom "lvlup:carrito-cambio" para misma pestaña.
 */

const STORAGE_KEY = 'lvlup_carrito';
const EVENT_NAME  = 'lvlup:carrito-cambio';

function leer() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (_) {
    return [];
  }
}

function guardar(items) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    window.dispatchEvent(new CustomEvent(EVENT_NAME));
  } catch (_) {
    // sin storage: lo dejamos pasar
  }
}

export function useCarrito() {
  const [items, setItems] = useState(leer);

  // Reaccionar a cambios en otras pestañas o componentes
  useEffect(() => {
    const refrescar = () => setItems(leer());
    window.addEventListener('storage', refrescar);
    window.addEventListener(EVENT_NAME, refrescar);
    return () => {
      window.removeEventListener('storage', refrescar);
      window.removeEventListener(EVENT_NAME, refrescar);
    };
  }, []);

  const añadir = useCallback((producto, cantidad = 1) => {
    const actual = leer();
    const idx = actual.findIndex(it => it.productoId === producto.id);
    if (idx >= 0) {
      actual[idx].cantidad += cantidad;
    } else {
      actual.push({
        productoId: producto.id,
        nombre:     producto.nombre,
        sku:        producto.sku,
        precio:     Number(producto.precio || 0),
        imagenUrl:  producto.imagenUrl || null,
        cantidad,
      });
    }
    guardar(actual);
  }, []);

  const cambiarCantidad = useCallback((productoId, nuevaCantidad) => {
    const actual = leer();
    const idx = actual.findIndex(it => it.productoId === productoId);
    if (idx < 0) return;
    if (nuevaCantidad <= 0) {
      actual.splice(idx, 1);
    } else {
      actual[idx].cantidad = nuevaCantidad;
    }
    guardar(actual);
  }, []);

  const quitar = useCallback((productoId) => {
    const actual = leer().filter(it => it.productoId !== productoId);
    guardar(actual);
  }, []);

  const vaciar = useCallback(() => {
    guardar([]);
  }, []);

  // Cálculos derivados
  const totalUnidades = items.reduce((s, it) => s + (it.cantidad || 0), 0);
  const totalEuros   = items.reduce((s, it) => s + (it.precio || 0) * (it.cantidad || 0), 0);

  return {
    items,
    totalUnidades,
    totalEuros,
    añadir,
    cambiarCantidad,
    quitar,
    vaciar,
  };
}
