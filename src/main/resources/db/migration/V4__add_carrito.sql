-- ============================================================
-- V4__add_carrito.sql
-- Carrito de compra persistente por cliente (PR #23)
-- ============================================================

-- === Carrito (uno por cliente) ===
CREATE TABLE carritos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL UNIQUE REFERENCES clientes(id) ON DELETE CASCADE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === Lineas del carrito (un producto + cantidad) ===
CREATE TABLE lineas_carrito (
    id BIGSERIAL PRIMARY KEY,
    carrito_id BIGINT NOT NULL REFERENCES carritos(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    -- Un producto no puede aparecer dos veces en el mismo carrito:
    -- si se anade de nuevo, se suma a la linea existente.
    CONSTRAINT uq_carrito_producto UNIQUE (carrito_id, producto_id)
);

CREATE INDEX idx_lineas_carrito_carrito ON lineas_carrito(carrito_id);
CREATE INDEX idx_lineas_carrito_producto ON lineas_carrito(producto_id);
