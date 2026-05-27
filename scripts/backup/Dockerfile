# ============================================================
# Imagen del servicio de backups de LevelUp Arcade
# Postgres 16 client sobre Alpine = ~30 MB
# ============================================================
FROM postgres:16-alpine

# Copiamos los scripts
COPY backup.sh /scripts/backup.sh
COPY entrypoint.sh /scripts/entrypoint.sh

# FIX: convertir CRLF (Windows) a LF (Unix) y dar permisos de ejecucion
RUN sed -i 's/\r$//' /scripts/backup.sh /scripts/entrypoint.sh && \
    chmod +x /scripts/backup.sh /scripts/entrypoint.sh

ENTRYPOINT ["/scripts/entrypoint.sh"]