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

- Java 21 (Temurin recomendado)
- Docker Desktop
- Git
- VS Code o IntelliJ

## Cómo arrancar el proyecto

### Primera vez

```bash
git clone https://github.com/juanluissimancasprundea25-create/LevelUp-Arcade.git
cd LevelUp-Arcade
git checkout develop
cp .env.example .env
```

Edita `.env` con tus valores locales (al menos `DB_PASSWORD` y `JWT_SECRET`). Luego:

```bash
docker compose up --build
```

Accesos:
- App: http://localhost:8080
- pgAdmin: http://localhost:5050

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
3. Refer­éncialo en `.github/workflows/ci.yml` con `${{ secrets.NOMBRE_SECRET }}`

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
- Mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat(modulo): añadir X`
  - `fix(modulo): corregir Y`
  - `docs(modulo): actualizar Z`
  - `refactor`, `test`, `chore`, `ci`

### Gestión de tareas

- Todas las tareas viven como **Issues** en GitHub.
- El **GitHub Project "LevelUp Arcade - Roadmap"** muestra el tablero Kanban (Todo / In Progress / Done).
- Al abrir un PR, incluye `Closes #N` en la descripción para cerrar automáticamente el issue al mergear.

## CI/CD

Cada push y cada PR ejecuta `.github/workflows/ci.yml`:

1. Compila el proyecto
2. Ejecuta tests JUnit contra un PostgreSQL temporal
3. Reporta el resultado

Si el CI falla, GitHub bloquea el merge del PR.

## Equipo

- **Iván López Flash** — Productos, categorías, autenticación, IA
- **Juan Luis Simancas Prundea** — Clientes, proveedores, pedidos, facturas, devoluciones
- **Chacho** — Frontend, AdminLTE, despliegue, documentación