-- ============================================================
-- V3__add_lineas_devolucion.sql
-- Granularidad por linea en devoluciones
-- ============================================================

ALTER TABLE lineas_pedido
    ADD COLUMN cantidad_devuelta INT NOT NULL DEFAULT 0
        CHECK (cantidad_devuelta >= 0);

ALTER TABLE lineas_pedido
    ADD CONSTRAINT chk_cantidad_devuelta_no_excede
        CHECK (cantidad_devuelta <= cantidad);

CREATE TABLE lineas_devolucion (
    id BIGSERIAL PRIMARY KEY,
    devolucion_id BIGINT NOT NULL REFERENCES devoluciones(id) ON DELETE CASCADE,
    linea_pedido_id BIGINT NOT NULL REFERENCES lineas_pedido(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10,2) NOT NULL CHECK (precio_unitario >= 0),
    CONSTRAINT uq_devolucion_linea UNIQUE (devolucion_id, linea_pedido_id)
);

CREATE INDEX idx_lineas_devolucion_devolucion ON lineas_devolucion(devolucion_id);
CREATE INDEX idx_lineas_devolucion_linea_pedido ON lineas_devolucion(linea_pedido_id);
CREATE INDEX idx_devoluciones_pedido ON devoluciones(pedido_id);
CREATE INDEX idx_devoluciones_estado ON devoluciones(estado);