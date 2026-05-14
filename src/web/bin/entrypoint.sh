#!/bin/bash
set -e
set -o pipefail

log() { echo -e "\e[32m[INFO]\e[0m $1"; }

export FRONTEND_PORT="${FRONTEND_PORT:-80}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"

export DB_HOST="${DB_HOST:-host.docker.internal}"
export DB_PORT="${DB_PORT:-5432}"
export DB_NAME="${DB_NAME:-epibuilder}"
export DB_USERNAME="${DB_USERNAME:-epiuser}"
export DB_PASSWORD="${DB_PASSWORD:-epiuser}"

log "Generating JWT_SECRET..."
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
export JWT_SECRET

log "Updating NGINX configuration..."

NGINX_CONF="/etc/nginx/sites-enabled/default"

sed -i "s/listen\s\+[0-9]\+;/listen ${FRONTEND_PORT};/" "$NGINX_CONF"

sed -i "s|proxy_pass http://localhost:[0-9]\+/|proxy_pass http://localhost:${BACKEND_PORT}/|" "$NGINX_CONF"

log "Starting NGINX..."
nginx

export EPIBUILDER_DB="/tmp/epibuilder/db"

log "Starting Spring Boot backend..."

exec java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar /epibuilder/epibuilder-backend.jar \
  --server.port=${BACKEND_PORT} \
  --spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
  --spring.datasource.username=${DB_USERNAME} \
  --spring.datasource.password=${DB_PASSWORD} \
  --jwt.secret=${JWT_SECRET}
