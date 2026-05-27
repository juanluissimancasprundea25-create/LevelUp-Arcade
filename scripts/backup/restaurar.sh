#!/bin/sh
# ============================================================
# LevelUp Arcade - Restaurar un backup
# Uso:  ./restaurar.sh <fichero.sql.gz>
# Ejemplo: ./restaurar.sh backups/levelup_levelup_20260527_030000.sql.gz
# ============================================================
set -e

if [ -z "$1" ]; then
    echo "Uso: $0 <fichero.sql.gz>"
    echo ""
    echo "Backups disponibles:"
    ls -lh backups/levelup_*.sql.gz 2>/dev/null || echo "  (ninguno)"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "ERROR: no existe el fichero ${BACKUP_FILE}"
    exit 1
fi

# Leemos las variables del .env
if [ -f ".env" ]; then
    set -a
    . ./.env
    set +a
fi

DB_NAME="${DB_NAME:-levelup}"
DB_USER="${DB_USER:-levelup_user}"
CONTAINER="${1:-levelup-postgres}"

echo "ATENCION: esto va a SOBRESCRIBIR la BD '${DB_NAME}' del contenedor 'levelup-postgres'."
echo "Fichero a restaurar: ${BACKUP_FILE}"
printf "Continuar? (escribe SI): "
read CONFIRM

if [ "${CONFIRM}" != "SI" ]; then
    echo "Cancelado."
    exit 0
fi

echo "[*] Restaurando ${BACKUP_FILE}..."
gunzip -c "${BACKUP_FILE}" | docker exec -i levelup-postgres psql -U "${DB_USER}" -d "${DB_NAME}"

echo "[OK] BD restaurada."
echo "Recuerda reiniciar la app para que recargue el pool de conexiones:"
echo "  docker compose restart app"