# ============================================================
# Nginx reverse proxy para LevelUp Arcade
# Termina TLS, hace proxy_pass a la app Spring Boot en :8080
# y anade cabeceras de seguridad estandar (HSTS, X-Frame, etc).
# ============================================================
FROM nginx:1.27-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY conf.d/levelup.conf.template /etc/nginx/templates/levelup.conf.template

EXPOSE 80 443