-- =====================================================================
--  V5 : verificacion de facturas mediante escaneo del QR
--  Añade el estado "verificada" + fecha del primer escaneo + contador.
-- =====================================================================

ALTER TABLE facturas
    ADD COLUMN verificada BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE facturas
    ADD COLUMN fecha_primera_verificacion TIMESTAMP NULL;

ALTER TABLE facturas
    ADD COLUMN numero_verificaciones INTEGER NOT NULL DEFAULT 0;

-- indice para filtrar facturas no verificadas en el panel de admin
CREATE INDEX idx_facturas_verificada ON facturas (verificada);