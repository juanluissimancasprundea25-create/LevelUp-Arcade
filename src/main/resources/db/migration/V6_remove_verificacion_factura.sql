-- =====================================================================
--  V6 : rollback completo del sistema de verificacion por QR
--  Elimina las columnas anadidas en V5 + la columna hash_qr de V1
--  (el sistema de QR se ha retirado).
-- =====================================================================

-- Indices primero (algunos motores lo exigen)
DROP INDEX IF EXISTS idx_facturas_verificada;

-- Columnas anadidas en V5
ALTER TABLE facturas DROP COLUMN IF EXISTS verificada;
ALTER TABLE facturas DROP COLUMN IF EXISTS fecha_primera_verificacion;
ALTER TABLE facturas DROP COLUMN IF EXISTS numero_verificaciones;

-- Columna original de V1 que guardaba el contenido del QR
ALTER TABLE facturas DROP COLUMN IF EXISTS hash_qr;