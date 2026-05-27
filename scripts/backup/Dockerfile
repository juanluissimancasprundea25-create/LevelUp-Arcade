# ============================================================
# Imagen del servicio de backups de LevelUp Arcade
# Postgres 16 client sobre Alpine = ~30 MB
# ============================================================
FROM postgres:16-alpine

# Copiamos los scripts y les damos permisos de ejecucion
COPY backup.sh /scripts/backup.sh
COPY entrypoint.sh /scripts/entrypoint.sh
RUN chmod +x /scripts/backup.sh /scripts/entrypoint.sh

# El contenedor no necesita postgres-server, solo el cliente (ya incluido).
# No exponemos puertos, no es un servicio de red.

ENTRYPOINT ["/scripts/entrypoint.sh"]