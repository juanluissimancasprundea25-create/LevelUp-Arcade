# Servicio de backup automático de Postgres

Servicio Docker que hace dumps comprimidos de la base de datos cada 24h y conserva los últimos 7 días.

## Cómo funciona

- Servicio `backup` en `docker-compose.yml` y `docker-compose-prod.yml`
- Al arrancar hace un backup inmediato (smoke test)
- Después entra en bucle: dump + `sleep 86400` (24h)
- Salida: `backups/levelup_<bd>_YYYYMMDD_HHMMSS.sql.gz`
- Rotación automática: borra ficheros con más de `RETENTION_DAYS` días (default 7)

## Variables

| Variable | Default | Descripción |
|---|---|---|
| `POSTGRES_HOST` | `postgres` | Host de la BD |
| `POSTGRES_PORT` | `5432` | Puerto |
| `POSTGRES_DB` | (de `.env`) | Nombre de la BD |
| `POSTGRES_USER` | (de `.env`) | Usuario |
| `PGPASSWORD` | (de `.env`) | Password |
| `BACKUP_DIR` | `/backups` | Carpeta dentro del contenedor |
| `RETENTION_DAYS` | `7` | Días a conservar |

## Ver el último backup

```bash
ls -lh backups/
```

## Forzar un backup manual

```bash
docker exec levelup-backup /scripts/backup.sh
```

## Restaurar un backup

**Linux/Mac:**
```bash
bash scripts/backup/restaurar.sh backups/levelup_levelup_20260527_030000.sql.gz
```

**Windows (PowerShell):**
```powershell
.\scripts\backup\restaurar.ps1 backups\levelup_levelup_20260527_030000.sql.gz
```

El script pide confirmación (escribir `SI`) antes de sobrescribir nada.

## Ver logs del servicio

```bash
docker compose logs -f backup
```