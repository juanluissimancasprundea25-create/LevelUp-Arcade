#!/bin/bash
set -e

CERTS_DIR="$(dirname "$0")/../certs"
mkdir -p "$CERTS_DIR"

if [ -f "$CERTS_DIR/fullchain.pem" ]; then
    echo "Ya existe $CERTS_DIR/fullchain.pem"
    echo "Borralo a mano si quieres regenerarlo."
    exit 0
fi

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$CERTS_DIR/privkey.pem" \
    -out    "$CERTS_DIR/fullchain.pem" \
    -subj "/C=ES/ST=Madrid/L=Madrid/O=LevelUp Arcade/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

chmod 600 "$CERTS_DIR/privkey.pem"
chmod 644 "$CERTS_DIR/fullchain.pem"

echo ""
echo "Certificado autofirmado generado en $CERTS_DIR"
echo "Tu navegador avisara de inseguro: es normal en autofirmado."