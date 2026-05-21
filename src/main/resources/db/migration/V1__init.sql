-- ============================================================
-- V1__init.sql
-- Esquema inicial de LevelUp Arcade
-- ============================================================

-- === Usuarios (base de autenticación) ===
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150),
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'EMPLEADO', 'CLIENTE')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ultimo_login TIMESTAMP
);

-- === Clientes (datos comerciales asociados a un usuario) ===
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    nif VARCHAR(20) UNIQUE,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    codigo_postal VARCHAR(10),
    pais VARCHAR(100) DEFAULT 'España',
    fecha_alta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === Proveedores ===
CREATE TABLE proveedores (
    id BIGSERIAL PRIMARY KEY,
    nombre_empresa VARCHAR(200) NOT NULL,
    cif VARCHAR(20) NOT NULL UNIQUE,
    email_contacto VARCHAR(150),
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    codigo_postal VARCHAR(10),
    pais VARCHAR(100) DEFAULT 'España',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === Categorías de productos ===
CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

-- === Productos ===
CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    stock_minimo INT NOT NULL DEFAULT 5,
    categoria_id BIGINT REFERENCES categorias(id) ON DELETE SET NULL,
    proveedor_id BIGINT REFERENCES proveedores(id) ON DELETE SET NULL,
    imagen_url VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === Pedidos ===
CREATE TABLE pedidos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    fecha_pedido TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'PAGADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO')),
    total DECIMAL(10,2) NOT NULL CHECK (total >= 0),
    metodo_pago VARCHAR(20) CHECK (metodo_pago IN ('TARJETA', 'PAYPAL', 'TRANSFERENCIA')),
    direccion_envio VARCHAR(255)
);

-- === Líneas de pedido (productos dentro de un pedido) ===
CREATE TABLE lineas_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(10,2) NOT NULL CHECK (precio_unitario >= 0)
);

-- === Facturas (PDF con QR) ===
CREATE TABLE facturas (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL UNIQUE REFERENCES pedidos(id),
    numero_factura VARCHAR(30) NOT NULL UNIQUE,
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ruta_pdf VARCHAR(500),
    hash_qr VARCHAR(255)
);

-- === Devoluciones ===
CREATE TABLE devoluciones (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'SOLICITADA'
        CHECK (estado IN ('SOLICITADA', 'APROBADA', 'RECHAZADA', 'COMPLETADA')),
    importe_devuelto DECIMAL(10,2),
    observaciones_admin TEXT
);

-- === Auditoría (quién hace qué y cuándo) ===
CREATE TABLE auditoria_log (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    accion VARCHAR(50) NOT NULL,
    entidad VARCHAR(50),
    entidad_id BIGINT,
    descripcion TEXT,
    ip_origen VARCHAR(45),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === Mensajes de chat (chat con administrador) ===
CREATE TABLE mensajes_chat (
    id BIGSERIAL PRIMARY KEY,
    remitente_id BIGINT NOT NULL REFERENCES usuarios(id),
    destinatario_id BIGINT REFERENCES usuarios(id),
    contenido TEXT NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- === Tokens de recuperación de contraseña ===
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    fecha_expiracion TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- Índices para queries frecuentes
-- ============================================================
CREATE INDEX idx_productos_categoria ON productos(categoria_id);
CREATE INDEX idx_productos_proveedor ON productos(proveedor_id);
CREATE INDEX idx_productos_activo ON productos(activo);
CREATE INDEX idx_pedidos_cliente ON pedidos(cliente_id);
CREATE INDEX idx_pedidos_estado ON pedidos(estado);
CREATE INDEX idx_lineas_pedido ON lineas_pedido(pedido_id);
CREATE INDEX idx_auditoria_usuario ON auditoria_log(usuario_id);
CREATE INDEX idx_auditoria_fecha ON auditoria_log(fecha);
CREATE INDEX idx_chat_destinatario ON mensajes_chat(destinatario_id, leido);

-- ============================================================
-- Datos iniciales: un usuario administrador para pruebas
-- IMPORTANTE: cambiar la contraseña en el primer login
-- Contraseña por defecto: "admin123" (hash BCrypt)
-- ============================================================
INSERT INTO usuarios (email, password_hash, nombre, apellidos, rol)
VALUES (
    'admin@leveluparcade.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Administrador',
    'LevelUp Arcade',
    'ADMIN'
);

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Videojuegos', 'Juegos físicos y digitales'),
    ('Consolas', 'Consolas y accesorios'),
    ('Merchandising', 'Camisetas, pósters y figuras');