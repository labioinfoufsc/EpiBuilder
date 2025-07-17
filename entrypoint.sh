#!/bin/bash

set -e

export FRONTEND_PORT="${FRONTEND_PORT:-80}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"
export DB_PORT="${DB_PORT:-5432}"

echo "Generating JWT Secret..."
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
export JWT_SECRET
echo "export JWT_SECRET=\"$JWT_SECRET\"" | tee -a /etc/environment >/dev/null

export DB_USERNAME="epiuser"
export DB_PASSWORD="epiuser"
export DB_NAME="epibuilder"
export DB_HOST="localhost"
export PORT="${BACKEND_PORT}"

echo "export DB_USERNAME=\"$DB_USERNAME\"" | tee -a /etc/environment >/dev/null
echo "export DB_PASSWORD=\"$DB_PASSWORD\"" | tee -a /etc/environment >/dev/null
echo "export DB_NAME=\"$DB_NAME\"" | tee -a /etc/environment >/dev/null
echo "export DB_HOST=\"$DB_HOST\"" | tee -a /etc/environment >/dev/null
echo "export DB_PORT=\"$DB_PORT\"" | tee -a /etc/environment >/dev/null
echo "export PORT=\"$PORT\"" | tee -a /etc/environment >/dev/null
echo "export JWT_SECRET=\"$JWT_SECRET\"" | tee -a /etc/environment >/dev/null

echo "Starting PostgreSQL on port ${DB_PORT}..."
# Inicia o serviço do PostgreSQL (ajuste conforme imagem usada)
pg_ctlcluster 14 main start || service postgresql start || true

echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U postgres; do
    sleep 2
done

echo "Configuring PostgreSQL database..."
psql -U postgres -h "$DB_HOST" -p "$DB_PORT" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}') THEN
            CREATE DATABASE "${DB_NAME}";
        END IF;
    END
    \$\$;
    CREATE USER "${DB_USERNAME}" WITH PASSWORD '${DB_PASSWORD}' CREATEDB;
    GRANT ALL PRIVILEGES ON DATABASE "${DB_NAME}" TO "${DB_USERNAME}";
EOSQL

echo "Adjusting NGINX configuration for ports..."
NGINX_CONF="/etc/nginx/sites-enabled/default"
sed -i "s/listen\s\+[0-9]\+;/listen ${FRONTEND_PORT};/" "$NGINX_CONF"
sed -i "s|proxy_pass http://localhost:[0-9]\+/|proxy_pass http://localhost:${BACKEND_PORT}/|" "$NGINX_CONF"

echo "Starting NGINX on port ${FRONTEND_PORT}..."
nginx

echo "Starting Spring Boot on port ${BACKEND_PORT}..."
exec java -jar /epibuilder/epibuilder-backend.jar \
    --server.port=${BACKEND_PORT} \
    --spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
    --spring.datasource.username=${DB_USERNAME} \
    --spring.datasource.password=${DB_PASSWORD} \
    --jwt.secret=${JWT_SECRET}
