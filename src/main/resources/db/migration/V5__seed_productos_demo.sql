-- ============================================================
-- V5: Datos de demo. Carga 12 productos (videojuegos) listos
-- para usar en cualquier despliegue limpio (incluido Docker).
--
-- Las imagenes se sirven desde placehold.co usando el nombre
-- del juego como overlay. El template detecta automaticamente
-- las URL que empiezan por "http" y las usa tal cual (ver
-- tienda/producto.html, fragmento img th:src), asi que no hace
-- falta empaquetar binarios en la imagen Docker.
--
-- "Novedad" en el catalogo se traduce en orden por fecha_alta
-- DESC. Los 3 ultimos INSERT usan CURRENT_TIMESTAMP para que
-- aparezcan en lo alto del filtro "Novedades"; el resto usan
-- una fecha mas antigua (now() - intervalos de dias) para
-- escalonarlos y que el orden tenga sentido.
--
-- Todos los INSERT son idempotentes: si ya existe una fila
-- con el mismo identificador unico no se duplica. Asi se
-- puede re-ejecutar la migracion sin romper bases de datos
-- que ya hayan sido sembradas a mano.
-- ============================================================

-- ---------- Categorias de juego ----------
-- Las 3 originales (Videojuegos, Consolas, Merchandising) las
-- creo la V1. Aqui anado 4 categorias mas para que los 12
-- productos puedan repartirse con coherencia tematica.
INSERT INTO categorias (nombre, descripcion) VALUES
    ('Accion',     'Juegos de accion, shooters y combate intenso'),
    ('Aventura',   'Mundo abierto, exploracion y narrativa'),
    ('RPG',        'Rol, progresion de personaje y largas campanas'),
    ('Indie',      'Producciones independientes y experiencias unicas')
ON CONFLICT (nombre) DO NOTHING;


-- ---------- Proveedor de demo ----------
INSERT INTO proveedores
    (nombre_empresa, cif, email_contacto, telefono, direccion, ciudad, codigo_postal, pais)
VALUES
    ('Distribuciones LevelUp S.L.', 'B12345678',
     'compras@distribuciones-levelup.local', '+34 911 000 000',
     'Calle del Pixel, 1', 'Madrid', '28001', 'Espana')
ON CONFLICT (cif) DO NOTHING;


-- ============================================================
-- 12 productos. Cada uno con:
--   - SKU unico
--   - imagen externa (placehold.co, sin copyright)
--   - categoria coherente con el juego
--   - fecha_alta escalonada para que el orden "novedad" funcione
--
-- Los 3 ultimos (Stellar Drift, Pixel Pioneers, Velocity Rush)
-- son los "Novedades": llevan CURRENT_TIMESTAMP, asi que el
-- catalogo los muestra primero al ordenar por fecha.
-- ============================================================

INSERT INTO productos
    (sku, nombre, descripcion, precio, stock, stock_minimo, categoria_id,
     proveedor_id, imagen_url, activo, fecha_alta, fecha_actualizacion)
SELECT t.sku, t.nombre, t.descripcion, t.precio, t.stock, t.stock_minimo,
       (SELECT id FROM categorias WHERE nombre = t.categoria_nombre),
       (SELECT id FROM proveedores WHERE cif = 'B12345678'),
       t.imagen_url, TRUE, t.fecha_alta, t.fecha_alta
FROM (VALUES
    -- ----- Antiguos (mas viejos) -----
    ('LV-ACC-001', 'Shadow Protocol',
     'Shooter tactico en primera persona. Infiltracion, sigilo y arsenal pesado en escenarios urbanos.',
     49.99, 18, 5, 'Accion',
     'https://placehold.co/600x600/1a1a2e/ffffff?text=Shadow+Protocol',
     CURRENT_TIMESTAMP - INTERVAL '60 days'),

    ('LV-RPG-001', 'Legends of Aethoria',
     'JRPG por turnos clasico. Un viaje epico por un continente magico con 60+ horas de campana.',
     59.99, 12, 4, 'RPG',
     'https://placehold.co/600x600/4a148c/ffffff?text=Legends+of+Aethoria',
     CURRENT_TIMESTAMP - INTERVAL '55 days'),

    ('LV-AVT-001', 'Crimson Sails',
     'Aventura de mundo abierto en alta mar. Captanea tu barco, comercia, explora islas perdidas.',
     54.99, 20, 5, 'Aventura',
     'https://placehold.co/600x600/0d3b66/ffffff?text=Crimson+Sails',
     CURRENT_TIMESTAMP - INTERVAL '50 days'),

    ('LV-IND-001', 'Hollow Garden',
     'Indie metroidvania con arte pintado a mano. Una jardinera explora un bosque caido en silencio.',
     19.99, 35, 8, 'Indie',
     'https://placehold.co/600x600/2d6a4f/ffffff?text=Hollow+Garden',
     CURRENT_TIMESTAMP - INTERVAL '45 days'),

    ('LV-ACC-002', 'Neon Berserker',
     'Brawler 2D arcade con estetica cyberpunk. Combos rapidos, jefes enormes y banda sonora synthwave.',
     24.99, 22, 5, 'Accion',
     'https://placehold.co/600x600/d62828/ffffff?text=Neon+Berserker',
     CURRENT_TIMESTAMP - INTERVAL '40 days'),

    ('LV-RPG-002', 'Witchforge Tactics',
     'Tactico por casillas con elementos roguelite. Mezcla heroes, forja runas y derrota al Conclave.',
     34.99, 16, 4, 'RPG',
     'https://placehold.co/600x600/3a0ca3/ffffff?text=Witchforge+Tactics',
     CURRENT_TIMESTAMP - INTERVAL '35 days'),

    ('LV-AVT-002', 'Skybound Couriers',
     'Aventura cooperativa sobre dirigibles. Hasta 4 jugadores reparten paquetes entre islas voladoras.',
     29.99, 25, 6, 'Aventura',
     'https://placehold.co/600x600/118ab2/ffffff?text=Skybound+Couriers',
     CURRENT_TIMESTAMP - INTERVAL '30 days'),

    ('LV-IND-002', 'Tiny Tea Shop',
     'Simulador acogedor de cafeteria. Atiende clientes, decora tu local y cultiva tus propias hierbas.',
     14.99, 40, 10, 'Indie',
     'https://placehold.co/600x600/e07a5f/ffffff?text=Tiny+Tea+Shop',
     CURRENT_TIMESTAMP - INTERVAL '25 days'),

    ('LV-VJG-001', 'Arcade Legends Collection',
     'Recopilatorio de 50 clasicos arcade restaurados en 4K. Incluye filtros CRT y leaderboards online.',
     39.99, 14, 4, 'Videojuegos',
     'https://placehold.co/600x600/ffb703/000000?text=Arcade+Legends',
     CURRENT_TIMESTAMP - INTERVAL '20 days'),

    -- ----- NOVEDADES (3 ultimos, fecha_alta = CURRENT_TIMESTAMP) -----
    ('LV-ACC-003', 'Stellar Drift',
     'NUEVO. Combate espacial arcade. Pilota cazas modulares y conquista sectores en partidas rapidas de 8 vs 8.',
     44.99, 30, 6, 'Accion',
     'https://placehold.co/600x600/06d6a0/ffffff?text=Stellar+Drift',
     CURRENT_TIMESTAMP),

    ('LV-IND-003', 'Pixel Pioneers',
     'NUEVO. Constructor de ciudades en pixel art. Empieza con una hoguera y termina con una metropoli electrica.',
     22.99, 28, 6, 'Indie',
     'https://placehold.co/600x600/ff006e/ffffff?text=Pixel+Pioneers',
     CURRENT_TIMESTAMP),

    ('LV-AVT-003', 'Velocity Rush',
     'NUEVO. Carreras anti-gravedad. 24 circuitos, modo carrera narrativo y editor de pistas comunitario.',
     39.99, 26, 5, 'Aventura',
     'https://placehold.co/600x600/fb5607/ffffff?text=Velocity+Rush',
     CURRENT_TIMESTAMP)
) AS t(sku, nombre, descripcion, precio, stock, stock_minimo, categoria_nombre,
       imagen_url, fecha_alta)
ON CONFLICT (sku) DO NOTHING;