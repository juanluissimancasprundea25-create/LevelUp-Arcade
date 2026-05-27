#!/bin/sh
# ============================================================
# Entrypoint del contenedor de backups
# 1. Hace un backup inmediato al arrancar (smoke test)
# 2. Despues entra en bucle: backup + sleep 24h
# ============================================================
set -e

echo "============================================================"
echo "LevelUp Arcade - Servicio de backups automaticos"
echo "  BD:          ${POSTGRES_DB}@${POSTGRES_HOST}"
echo "  Destino:     ${BACKUP_DIR}"
echo "  Retencion:   ${RETENTION_DAYS} dias"
echo "  Frecuencia:  cada 24h"
echo "============================================================"

# Esperamos a que postgres este listo (max 60s)
echo "[entrypoint] Esperando a que Postgres este disponible..."
RETRIES=30
until pg_isready -h "${POSTGRES_HOST}" -p "${POSTGRES_PORT}" -U "${POSTGRES_USER}" > /dev/null 2>&1; do
    RETRIES=$((RETRIES - 1))
    if [ ${RETRIES} -le 0 ]; then
        echo "[entrypoint] ERROR: Postgres no responde tras 60 segundos. Abortando."
        exit 1
    fi
    sleep 2
done
echo "[entrypoint] Postgres listo."

# Bucle infinito: backup -> sleep 24h
while true; do
    /scripts/backup.sh || echo "[entrypoint] WARN: backup fallo, reintentamos en 24h"
    echo "[entrypoint] Siguiente backup en 24h..."
    sleep 86400
done