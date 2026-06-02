-- ============================================================
-- V6: Reemplaza las imagenes placeholder de la V5 por fotos
-- reales servidas por picsum.photos.
--
-- Convencion: la URL usa el SKU como semilla, asi cada producto
-- siempre carga la misma foto (estable) sin necesidad de subir
-- ficheros binarios ni configurar un CDN. La foto vive en la
-- columna imagen_url y por tanto sincroniza entre dispositivos
-- de forma automatica: cualquier cambio que haga el ADMIN desde
-- el panel queda en BD.
--
-- Idempotente: solo actualiza filas cuya imagen_url siga siendo
-- el placeholder de la V5 (placehold.co). Si el ADMIN ya cambio
-- la imagen a mano desde el panel, la respetamos.
-- ============================================================

UPDATE productos
SET imagen_url = 'https://picsum.photos/seed/' || sku || '/600/600',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE imagen_url LIKE 'https://placehold.co/%';
