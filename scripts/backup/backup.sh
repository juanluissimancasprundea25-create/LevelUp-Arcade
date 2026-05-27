#!/bin/sh
# ============================================================
# LevelUp Arcade - Script de backup automatico de Postgres
# Genera un dump comprimido con timestamp y rota los antiguos
# (conserva los ultimos RETENTION_DAYS dias, por defecto 7)
# ============================================================
set -e

# Variables esperadas (vienen del docker-compose):
#   POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, PGPASSWORD
#   BACKUP_DIR, RETENTION_DAYS

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/levelup_${POSTGRES_DB}_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Iniciando backup de ${POSTGRES_DB}@${POSTGRES_HOST}..."

# pg_dump -> gzip -> fichero. Si falla en cualquier punto, el set -e aborta.
pg_dump \
    -h "${POSTGRES_HOST}" \
    -p "${POSTGRES_PORT}" \
    -U "${POSTGRES_USER}" \
    -d "${POSTGRES_DB}" \
    --no-owner \
    --no-privileges \
    --clean \
    --if-exists \
  | gzip > "${BACKUP_FILE}"

SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "[$(date)] Backup OK: ${BACKUP_FILE} (${SIZE})"

# --- Rotacion: borra dumps mas viejos que RETENTION_DAYS dias ---
echo "[$(date)] Limpiando backups con mas de ${RETENTION_DAYS} dias..."
find "${BACKUP_DIR}" -name "levelup_*.sql.gz" -type f -mtime "+${RETENTION_DAYS}" -delete

REMAINING=$(find "${BACKUP_DIR}" -name "levelup_*.sql.gz" -type f | wc -l)
echo "[$(date)] Backups conservados: ${REMAINING}"