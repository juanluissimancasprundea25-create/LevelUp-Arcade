# LevelUp Arcade

Sistema de gestión de inventario, clientes y proveedores para LevelUp Arcade, con integración de IA mediante OpenRouter.

## Tecnologías

- **Backend**: Java 21 + Spring Boot 3.5 (Web, JPA, Security, Thymeleaf, Validation)
- **Base de datos**: PostgreSQL 16 + Flyway para migraciones
- **Frontend**: Thymeleaf + Bootstrap (AdminLTE)
- **Autenticación**: JWT
- **IA**: OpenRouter API
- **DevOps**: Docker, Docker Compose, GitHub Actions
- **Gestión de proyecto**: GitHub Projects (Kanban), Issues, Pull Requests

## Requisitos

- **Docker Desktop** (suficiente para arrancar todo)
- **Git**
- Opcional para desarrollo: Java 21 (Temurin), VS Code o IntelliJ

## Cómo arrancar el proyecto

### Primera vez (clonado)

```bash
git clone https://github.com/juanluissimancasprundea25-create/LevelUp-Arcade.git
cd LevelUp-Arcade
git checkout develop
```

Copia la plantilla de variables a tu fichero local:

**Linux / Mac:**
```bash
cp .env.example .env
```

**Windows (PowerShell):**
```powershell
Copy-Item .env.example .env
```

Edita `.env` con tus valores locales. Los obligatorios son:
- `DB_PASSWORD` (cualquier valor para tu entorno local)
- `JWT_SECRET` (mínimo 32 caracteres aleatorios)
- `PGADMIN_EMAIL` (usa un dominio real como `.com`, no `.local`)

### Modo 1: Stack completo con Docker (recomendado)

Levanta postgres + app Spring Boot + pgAdmin con un solo comando:

```bash
docker compose up --build
```

La primera vez tarda varios minutos (descarga JDK, compila JAR). Las siguientes son segundos.

Accesos:
- **App**: http://localhost:8080
- **pgAdmin**: http://localhost:5050 (con `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` del `.env`)

Para parar:
```bash
docker compose down
```

### Modo 2: Desarrollo local (más rápido para iterar)

Solo postgres en docker, app corriendo localmente con Maven Wrapper. Útil cuando estás tocando código y quieres recompilar al vuelo.

**Requisitos extra**: Java 21 instalado localmente.

**Linux / Mac:**
```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
        Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
    }
}
$env:DB_HOST = "localhost"
docker compose up -d postgres
.\mvnw spring-boot:run
```

Puedes guardar este bloque PowerShell como `arranca.ps1` en la raíz (ya está en `.gitignore`).

### Trabajo diario

```bash
git checkout develop
git pull origin develop
git checkout -b feature/tu-nombre-descripcion
# ...trabajas...
git add .
git commit -m "feat(modulo): descripción"
git push origin feature/tu-nombre-descripcion
```

Después abre un Pull Request a `develop` desde la web de GitHub.

## Gestión de credenciales

Este proyecto **NUNCA** debe tener credenciales hardcodeadas en el código fuente.

### Variables locales (.env)

El fichero `.env` (ignorado por Git) contiene las credenciales que cada desarrollador usa en su máquina. Está basado en `.env.example` que sirve de plantilla.

Si añades una variable nueva al proyecto:
1. Añádela al `.env.example` con valor placeholder
2. Añade el campo correspondiente en `docker-compose.yml` para que se inyecte al contenedor
3. Documenta su uso en la sección correspondiente

### GitHub Secrets (para CI/CD)

Los secrets configurados en GitHub Actions (Settings → Secrets and variables → Actions) son usados solo por el workflow de CI. No son accesibles desde código de aplicación.

Secrets configurados:
- `OPENROUTER_API_KEY` — API key de OpenRouter (LLM)
- `JWT_SECRET` — clave secreta para firmar tokens JWT
- `MAIL_USERNAME`, `MAIL_PASSWORD` — credenciales SMTP para emails
- `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET` — Google reCAPTCHA v3
- `DB_PASSWORD_CI` — contraseña de la BD temporal usada en tests del CI

### Para añadir un Secret nuevo

1. Ve a Settings → Secrets and variables → Actions → New repository secret
2. Añade el secret con su valor real
3. Referéncialo en `.github/workflows/ci.yml` con `${{ secrets.NOMBRE_SECRET }}`

## Estructura del proyecto
src/main/java/com/leveluparcade/
├── config/         # Configuración Spring (Security, etc.)
├── controller/
│   ├── web/        # Controladores Thymeleaf (devuelven vistas)
│   └── api/        # Controladores REST (devuelven JSON)
├── service/        # Interfaces de servicios
│   └── impl/       # Implementaciones
├── repository/     # Repositorios JPA
├── entity/         # Entidades JPA
├── dto/            # Data Transfer Objects (request / response)
├── security/       # Autenticación y autorización
├── exception/      # Excepciones personalizadas
├── llm/            # Integración con OpenRouter
├── util/           # Utilidades
├── mapper/         # Mapeo Entity ↔ DTO
└── validator/      # Validadores personalizados

## Reglas del equipo

### Flujo Git

- **`main`** y **`develop`** están protegidas. No se permite push directo.
- Todo el trabajo va en ramas `feature/<nombre>-<descripcion>` o `fix/<nombre>-<descripcion>`.
- Cada PR debe tener:
  - 1 approval de un compañero
  - CI en verde
  - Rama actualizada con develop antes de mergear
- Mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/) **sin tildes** (encoding PowerShell):
  - `feat(modulo): anadir X`
  - `fix(modulo): corregir Y`
  - `docs(modulo): actualizar Z`
  - `refactor`, `test`, `chore`, `ci`

### Gestión de tareas

- Todas las tareas viven como **Issues** en GitHub.
- El **GitHub Project "LevelUp Arcade - Roadmap"** muestra el tablero Kanban (Todo / In Progress / Done).
- Al abrir un PR, incluye `Closes #N` en la descripción para cerrar automáticamente el issue al mergear.

## Comandos útiles

### Tests

```bash
./mvnw test                          # todos los tests
./mvnw test -Dtest=NombreClaseTest   # un solo test
```

**Windows (PowerShell)**: las comas necesitan comillas:
```powershell
.\mvnw test "-Dtest=ChatApiControllerTest,FacturaApiControllerTest"
```

### Reset completo de BD

```bash
docker compose down -v   # -v borra el volumen, perdiendo los datos
docker compose up --build
```

Flyway volverá a aplicar todas las migraciones desde cero.

### Logs

```bash
docker compose logs -f app        # logs de la app en tiempo real
docker compose logs -f postgres   # logs de postgres
```

## CI/CD

Cada push y cada PR ejecuta `.github/workflows/ci.yml`:

1. Compila el proyecto
2. Ejecuta tests JUnit contra un PostgreSQL temporal
3. Reporta el resultado

Si el CI falla, GitHub bloquea el merge del PR.

## Solución de problemas comunes

### `Could not resolve placeholder 'JWT_SECRET'`
No has cargado las variables del `.env`. En PowerShell usa el bloque del **Modo 2** o el script `arranca.ps1`.

### pgAdmin sale en `exited with code 1` con error de email
`PGADMIN_EMAIL` no puede usar dominios reservados como `.local`. Usa `.com`, `.org`, etc.

### `JAVA_HOME is not defined correctly`
Tienes que apuntar `JAVA_HOME` a tu instalación de JDK 21. En PowerShell:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
```

### Tests fallan en PowerShell con comas
Las comas en `-Dtest=A,B` requieren comillas en PowerShell:
```powershell
.\mvnw test "-Dtest=A,B"
```

## Equipo

- **Iván López Flash** — Productos, categorías, autenticación, IA
- **Juan Luis Simancas Prundea** — Clientes, proveedores, pedidos, facturas, devoluciones
- **Chacho** — Frontend, AdminLTE, despliegue, documentación