# LevelUp Arcade

> Tienda online de videojuegos y merchandising con panel de gestion integrado, integracion de IA (OpenRouter) y experiencia de cliente premium.

Proyecto academico Trimestre 3 — Centro Educativo CampusFP.
Equipo: **Juan Luis Simancas** · **Ivan Lopez** · **Chacho**.

---

## Tabla de contenidos

1. [Que es LevelUp Arcade](#que-es-levelup-arcade)
2. [Funcionalidades](#funcionalidades)
3. [Requisitos previos](#requisitos-previos)
4. [Como arrancar el proyecto](#como-arrancar-el-proyecto)
5. [URLs principales](#urls-principales)
6. [Cuentas de prueba](#cuentas-de-prueba)
7. [Estructura del proyecto](#estructura-del-proyecto)
8. [Variables de entorno](#variables-de-entorno)
9. [Problemas frecuentes](#problemas-frecuentes)
10. [Equipo](#equipo)

---

## Que es LevelUp Arcade

LevelUp Arcade es una **aplicacion web completa** para una tienda de videojuegos. Tiene dos caras:

- **Tienda publica** (lo que ve el cliente): catalogo, carrito, checkout, area "Mi cuenta", chat con soporte, asistente IA.
- **Panel de gestion** (lo que ve el admin/empleado): CRUD completo del inventario, pedidos, facturas, devoluciones, auditoria, IA generativa.

La interfaz es una **SPA estilo cockpit** (panel de mando) inspirada en interfaces de juegos sci-fi: fondo 3D animado, transiciones con efecto "warp" entre pantallas, paneles HUD. Conviven en paralelo la version moderna (SPA bajo `/cockpit/*`) y una version clasica Thymeleaf (bajo `/login`, `/admin`, etc.) que se mantiene por compatibilidad.

---

## Funcionalidades

### Panel admin (`/cockpit/admin`)

10 modulos CRUD completos:

| Modulo | Que hace |
|---|---|
| **Productos** | Alta, edicion, control de stock, alertas de bajo stock, imagenes desde URL |
| **Categorias** | Clasificacion de productos |
| **Clientes** | Gestion de clientes registrados |
| **Proveedores** | Gestion de proveedores y empresas distribuidoras |
| **Pedidos** | Timeline visual (pendiente → pagado → enviado → entregado), cancelacion |
| **Facturas** | Generacion automatica, descarga en PDF, verificacion publica via QR |
| **Devoluciones** | Aprobar/rechazar devoluciones, lineas individuales por producto |
| **Chat** | Atencion a clientes en tiempo real (polling cada 4 segundos) |
| **Asistente IA** | Conversacion libre con LLM via OpenRouter |
| **Auditoria** | Registro de todas las acciones criticas con usuario y timestamp |

### Tienda cliente (`/cockpit`)

- Catalogo publico con filtros (categoria, busqueda, orden por precio/nombre)
- Ficha de producto con stock, descripcion e imagenes
- Carrito persistente (sessionStorage)
- Registro con auto-login (JWT)
- Checkout con tres metodos de pago (tarjeta, PayPal, transferencia) con validaciones reales
- Area "Mi cuenta": dashboard, mis pedidos, mis facturas, mis devoluciones, mi perfil, asistente IA
- Chat directo con el equipo de soporte

### IA generativa (OpenRouter)

- **Generar descripcion de producto** desde el panel admin (un boton dentro del formulario de producto)
- **Sugerir categoria** automaticamente para un producto nuevo
- **Asistente del cliente**: chat libre tipo FAQ que ayuda al usuario con preguntas generales (no toca datos)

---

## Requisitos previos

Necesitas tener instalado:

| Herramienta | Version minima | Para que |
|---|---|---|
| **Java JDK** | 21 | Compilar y ejecutar el backend Spring Boot |
| **Maven** | 3.9+ | Gestor de dependencias (viene incluido `mvnw`) |
| **Node.js** | 18+ | Ejecutar el frontend React |
| **npm** | 9+ | Gestor de paquetes del frontend (viene con Node) |
| **Docker Desktop** | ultima | Levantar PostgreSQL y pgAdmin con un solo comando |
| **Git** | cualquiera | Clonar el repo |

> **Windows**: si PowerShell te bloquea `npm`, ejecuta una vez:
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

---

## Como arrancar el proyecto

### 1. Clonar el repo

```bash
git clone <url-del-repo>
cd LevelUp-Arcade
```

### 2. Crear el fichero `.env` en la raiz

Copia el ejemplo y rellenalo:

```env
DB_NAME=levelup
DB_USER=levelup_user
DB_PASSWORD=levelup_pass_2026

JWT_SECRET=esta_es_mi_clave_super_secreta_de_desarrollo_2026_levelup_arcade
JWT_EXPIRATION_MS=86400000

OPENROUTER_API_KEY=sk-or-v1-...        # pidela a un miembro del equipo
OPENROUTER_MODEL=meta-llama/llama-3.3-70b-instruct:free
```

> `JWT_SECRET` debe tener **al menos 32 caracteres**. Si lo cambias, todos los tokens existentes dejan de ser validos.

### 3. Levantar PostgreSQL + pgAdmin con Docker

```powershell
docker compose up -d
```

Esto deja corriendo:
- **PostgreSQL** en `localhost:5432` con base de datos `levelup`
- **pgAdmin** en `localhost:5050` (opcional, solo para ver la BD desde el navegador)

Para parar: `docker compose down`. Para borrarlo todo: `docker compose down -v`.

### 4. Arrancar el backend (Spring Boot)

En PowerShell #1, dentro de la carpeta raiz del proyecto:

```powershell
# Carga las variables del .env y arranca Spring
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw spring-boot:run
```

Spring tarda unos 15-20 segundos en arrancar. Cuando veas `Started LevelupArcadeApplication`, esta listo.

- API REST: <http://localhost:8080/api/...>
- App Thymeleaf clasica: <http://localhost:8080>

> En la primera arrancada, **Flyway** crea automaticamente todas las tablas y carga los datos de demostracion (12 videojuegos reales con portadas de Steam, usuarios admin/empleado, proveedor demo).

### 5. Arrancar el frontend (Vite + React)

En PowerShell #2, dentro de la carpeta `frontend/`:

```powershell
cd frontend
npm install        # solo la primera vez
npm run dev
```

Esto abre Vite en <http://localhost:5173>.

### 6. Abrir la app

- **SPA cockpit (la nueva)**: <http://localhost:5173/cockpit>
- App clasica Thymeleaf: <http://localhost:8080>

---

## URLs principales

### En desarrollo

| URL | Que hay |
|---|---|
| `localhost:5173/cockpit` | Landing 3D del cockpit |
| `localhost:5173/cockpit/tienda` | Catalogo publico (sin login) |
| `localhost:5173/cockpit/registro` | Registro de cliente nuevo |
| `localhost:5173/cockpit/login` | Login (cliente, admin y empleado) |
| `localhost:5173/cockpit/admin` | Panel admin (rol ADMIN o EMPLEADO) |
| `localhost:5173/cockpit/mi-cuenta` | Area del cliente |
| `localhost:8080` | App Thymeleaf clasica (legacy) |
| `localhost:8080/api/...` | API REST (acceso directo, con Bearer JWT) |
| `localhost:5050` | pgAdmin (opcional) |

### En produccion

La SPA se sirve desde el mismo Spring Boot bajo `/app/cockpit/*`, asi que todo va por `localhost:8080`.

---

## Cuentas de prueba

El seed de Flyway crea estas cuentas. Cambialas en produccion.

| Email | Password | Rol |
|---|---|---|
| `admin@leveluparcade.local` | `admin123` | ADMIN |
| `empleado@leveluparcade.local` | `empleado123` | EMPLEADO |

Los clientes se crean registrandose desde `/cockpit/registro`.

---

## Estructura del proyecto

```
LevelUp-Arcade/
├── src/main/java/com/leveluparcade/      # Backend Java
│   ├── auditoria/         # Sistema de auditoria de eventos
│   ├── config/            # Configuracion (Security, Web, etc.)
│   ├── controller/
│   │   ├── api/           # REST controllers (consume la SPA)
│   │   └── web/           # Thymeleaf controllers (app clasica)
│   ├── dto/
│   │   ├── request/       # DTOs de entrada (peticion)
│   │   └── response/      # DTOs de salida (respuesta)
│   ├── entity/            # Entidades JPA (= tablas de BD)
│   ├── exception/         # Manejo global de errores
│   ├── llm/               # Cliente OpenRouter (IA)
│   ├── repository/        # Acceso a datos (Spring Data JPA)
│   ├── security/          # JWT, UserDetailsService
│   └── service/           # Logica de negocio (interfaces + impl)
│
├── src/main/resources/
│   ├── db/migration/      # Migraciones Flyway (V1, V2, V3...)
│   ├── templates/         # Vistas Thymeleaf (app clasica)
│   ├── static/            # CSS, JS y la SPA compilada (tras build)
│   └── application.yml    # Configuracion de Spring
│
├── frontend/              # SPA React
│   ├── src/
│   │   ├── components/    # Componentes reutilizables
│   │   ├── lib/           # API client, auth, carrito, hooks
│   │   ├── routes/        # Una carpeta = una pagina
│   │   ├── scenes/        # Escena 3D (Three.js + R3F)
│   │   └── main.jsx       # Entry point
│   └── vite.config.js     # Configuracion de Vite
│
├── docker-compose.yml     # Postgres + pgAdmin
├── pom.xml                # Dependencias Maven (backend)
├── .env                   # Variables sensibles (NO se sube al repo)
└── README.md              # Este fichero
```

Para entender mas en profundidad la arquitectura y las tecnologias, ver **[TECNOLOGIAS.md](TECNOLOGIAS.md)**.

---

## Variables de entorno

| Variable | Obligatoria | Descripcion |
|---|---|---|
| `DB_NAME` | si | Nombre de la base de datos PostgreSQL |
| `DB_USER` | si | Usuario de la BD |
| `DB_PASSWORD` | si | Contrasena de la BD |
| `JWT_SECRET` | si | Clave para firmar tokens JWT (minimo 32 caracteres) |
| `JWT_EXPIRATION_MS` | no | Duracion del JWT en milisegundos (default: 86400000 = 24h) |
| `OPENROUTER_API_KEY` | si para IA | Clave de OpenRouter.ai (obtener gratis en su web) |
| `OPENROUTER_MODEL` | no | Modelo a usar (default: `meta-llama/llama-3.3-70b-instruct:free`) |
| `APP_BASE_URL` | no | URL base para emails de reset password (default: `http://localhost:8080`) |

Si quieres comprobar si la IA esta configurada correctamente, abre como admin:
`GET http://localhost:8080/api/llm/estado/ping` con tu Bearer token.

---

## Problemas frecuentes

### "Migration checksum mismatch" al arrancar Spring

Has cambiado el contenido de una migracion Flyway que ya estaba aplicada. Solucion rapida:

```powershell
docker exec -it <contenedor-postgres> psql -U levelup_user -d levelup -c "DELETE FROM flyway_schema_history WHERE version = '<numero>';"
```

Para evitar que vuelva a pasar durante desarrollo, anade en `application.yml`:

```yaml
spring:
  flyway:
    repair-on-migrate: true
```

### "0 de 0 productos" en la tienda

La BD no tiene productos activos. Comprueba:

```powershell
docker exec -it <contenedor> psql -U levelup_user -d levelup -c "SELECT id, sku, nombre, activo FROM productos;"
```

Si la tabla esta vacia, fuerza la reaplicacion del seed:

```powershell
docker exec -it <contenedor> psql -U levelup_user -d levelup -c "DELETE FROM flyway_schema_history WHERE version IN ('5','6','7');"
```

Y reinicia Spring.

### La IA devuelve "La IA no respondio"

Comprueba el estado de OpenRouter desde el panel admin con:
`GET /api/llm/estado/ping`

Causas tipicas:
- `OPENROUTER_API_KEY` vacio o invalido en el `.env`
- El modelo ya no esta disponible: cambia `OPENROUTER_MODEL` a otro `:free`
- Rate limit del modelo gratis: espera unos segundos o cambia de modelo

### PowerShell bloquea `npm`

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

O ejecuta como `npm.cmd` (con extension explicita).

### Puerto 8080 / 5173 ocupado

```powershell
# Ver que esta usando el puerto
netstat -ano | findstr :8080
# Matar el proceso (PID es el ultimo numero)
taskkill /PID <pid> /F
```

### "La SPA carga pero se queda en INICIANDO COCKPIT"

Suele ser un error de JavaScript que no llega a renderizar. Abre la consola del navegador (F12 → Console) y mira los errores en rojo. Lo mas frecuente es:
- Imports rotos al editar `Tienda.jsx` o `TiendaProducto.jsx`
- Hooks (`useEffect`, `useState`) colocados fuera del componente

### Email duplicado al registrarse

Si "borraste" un cliente con soft-delete y vuelves a registrarte con el mismo email:

```powershell
docker exec -it <contenedor> psql -U levelup_user -d levelup -c "DELETE FROM usuarios WHERE email = 'el-email@ejemplo.com';"
```

---

## Equipo

| Persona | Areas principales |
|---|---|
| **Ivan Lopez** | Productos, Categorias, Autenticacion JWT, integracion IA base |
| **Juan Luis Simancas** | Clientes, Proveedores, roles y permisos, Spring Security funcional, rediseno completo SPA cockpit, modulos del cliente (Fase 7), generador de imagenes con IA |
| **Chacho** | Infraestructura, PostgreSQL, Docker, frontend clasico Thymeleaf, documentacion, despliegue |

---

## Licencia

Proyecto academico sin uso comercial. Las portadas de los videojuegos son propiedad de sus respectivos editores y se usan unicamente con fines didacticos.