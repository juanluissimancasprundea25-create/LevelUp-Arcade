-- ============================================================
-- V7: Sustituye los 12 productos ficticios del seed V5 por
-- videojuegos REALES con portadas oficiales servidas por
-- el CDN de Steam (cdn.cloudflare.steamstatic.com).
--
-- - Se conservan los SKU para no romper pedidos, facturas ni
--   devoluciones que ya referencien estos productos.
-- - El UPDATE solo actua si el nombre sigue siendo el original
--   del seed V5. Si alguien renombro un producto desde el panel
--   admin, lo respetamos.
-- - El campo imagen_url vive en BD, asi que el cambio se
--   sincroniza entre dispositivos automaticamente.
--
-- Idempotente: re-ejecutar la migracion no duplica ni machaca
-- datos editados manualmente.
-- ============================================================

-- ---------- Accion ----------
UPDATE productos SET
    nombre      = 'DOOM Eternal',
    descripcion = 'FPS frenetico de id Software. Lucha contra las hordas infernales con un arsenal devastador y combates de alta velocidad.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/782330/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-ACC-001' AND nombre = 'Shadow Protocol';

UPDATE productos SET
    nombre      = 'Hotline Miami',
    descripcion = 'Arcade top-down brutal con estetica neon ochentera. Combos rapidos, mascaras desbloqueables y banda sonora synthwave inolvidable.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/219150/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-ACC-002' AND nombre = 'Neon Berserker';

UPDATE productos SET
    nombre      = 'Cyberpunk 2077',
    descripcion = 'RPG de accion en mundo abierto ambientado en Night City. Personaliza a V, mejora tu cuerpo con cyberware y vive una historia cinematica.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/1091500/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-ACC-003' AND nombre = 'Stellar Drift';

-- ---------- RPG ----------
UPDATE productos SET
    nombre      = 'The Witcher 3: Wild Hunt',
    descripcion = 'JRPG occidental de mundo abierto. Encarna a Geralt de Rivia, caza monstruos y descubre una historia ramificada de mas de 100 horas.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/292030/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-RPG-001' AND nombre = 'Legends of Aethoria';

UPDATE productos SET
    nombre      = 'Baldurs Gate 3',
    descripcion = 'RPG por turnos basado en D&D 5e. Crea tu heroe, recluta companeros con historias propias y enfrentate al Cerebro Devorador.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/1086940/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-RPG-002' AND nombre = 'Witchforge Tactics';

-- ---------- Aventura ----------
UPDATE productos SET
    nombre      = 'Sea of Thieves',
    descripcion = 'Aventura cooperativa de piratas en mundo abierto. Captanea tu barco con amigos, busca tesoros y enfrentate a otros equipos.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/1172620/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-AVT-001' AND nombre = 'Crimson Sails';

UPDATE productos SET
    nombre      = 'Death Stranding',
    descripcion = 'Aventura unica de Hideo Kojima. Reparte paquetes a traves de una America postapocaliptica y reconecta a la humanidad.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/1190460/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-AVT-002' AND nombre = 'Skybound Couriers';

UPDATE productos SET
    nombre      = 'Red Dead Redemption 2',
    descripcion = 'Western de mundo abierto de Rockstar. Vive las ultimas hazanas de la banda de Dutch van der Linde en el ocaso del salvaje oeste.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/1174180/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-AVT-003' AND nombre = 'Velocity Rush';

-- ---------- Indie ----------
UPDATE productos SET
    nombre      = 'Hollow Knight',
    descripcion = 'Metroidvania indie dibujado a mano. Explora el reino caido de Hallownest, derrota jefes desafiantes y descubre sus secretos.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/367520/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-IND-001' AND nombre = 'Hollow Garden';

UPDATE productos SET
    nombre      = 'Stardew Valley',
    descripcion = 'Simulador acogedor de granja en pixel art. Cultiva, cria animales, pesca, mineria y haz amistad con los vecinos del pueblo.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/413150/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-IND-002' AND nombre = 'Tiny Tea Shop';

UPDATE productos SET
    nombre      = 'Terraria',
    descripcion = 'Sandbox 2D de aventura y construccion. Explora, lucha, mina, construye y enfrentate a docenas de jefes en mundos generados.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/105600/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-IND-003' AND nombre = 'Pixel Pioneers';

-- ---------- Videojuegos (general) ----------
UPDATE productos SET
    nombre      = 'Elden Ring',
    descripcion = 'Action-RPG de FromSoftware en colaboracion con George R. R. Martin. Mundo abierto desafiante con combates exigentes.',
    imagen_url  = 'https://cdn.cloudflare.steamstatic.com/steam/apps/1245620/header.jpg',
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE sku = 'LV-VJG-001' AND nombre = 'Arcade Legends Collection';
