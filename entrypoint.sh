#!/bin/bash
set -e
set -o pipefail

log() { echo -e "\e[32m[INFO]\e[0m $1"; }
err() { echo -e "\e[31m[ERROR]\e[0m $1"; }

export FRONTEND_PORT="${FRONTEND_PORT:-80}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"
export DB_PORT="${DB_PORT:-5432}"
export DB_HOST="localhost"
export DB_NAME="${DB_NAME:-epibuilder}"
export DB_USERNAME="${DB_USERNAME:-epiuser}"
export DB_PASSWORD="${DB_PASSWORD:-epiuser}"
export ENV="${ENV:-development}"
export PORT="${BACKEND_PORT}"

log "Generating JWT_SECRET..."
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
export JWT_SECRET

DATA_DIR="/var/lib/postgresql/data"
mkdir -p "$DATA_DIR"
chown -R postgres:postgres "$DATA_DIR"

if [ ! -s "$DATA_DIR/PG_VERSION" ]; then
  log "Initializing PostgreSQL cluster..."
  su - postgres -c "/usr/lib/postgresql/13/bin/initdb -D $DATA_DIR"
fi

PG_HBA="$DATA_DIR/pg_hba.conf"
if [[ "$ENV" != "production" ]]; then
  log "Setting 'trust' in pg_hba.conf (dev mode)"
  sed -i 's/^host.*all.*all.*.*md5$/host all all 0.0.0.0\/0 trust/' "$PG_HBA"
fi

log "Starting PostgreSQL..."
su - postgres -c "/usr/lib/postgresql/13/bin/pg_ctl -D $DATA_DIR -o \"-p $DB_PORT\" -w start"

log "Waiting for PostgreSQL to be ready..."
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U postgres > /dev/null 2>&1; do
  sleep 2
done

log "Creating role if not exists..."
psql -U postgres -h "$DB_HOST" -p "$DB_PORT" -v ON_ERROR_STOP=1 <<EOSQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${DB_USERNAME}') THEN
    CREATE ROLE "${DB_USERNAME}" WITH LOGIN PASSWORD '${DB_PASSWORD}' CREATEDB;
  END IF;
END
\$\$;
EOSQL

log "Checking if database '${DB_NAME}' exists..."
DB_EXISTS=$(psql -U postgres -h "$DB_HOST" -p "$DB_PORT" -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'")

if [ "$DB_EXISTS" != "1" ]; then
  log "Creating database '${DB_NAME}' owned by '${DB_USERNAME}'..."
  psql -U postgres -h "$DB_HOST" -p "$DB_PORT" -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"${DB_NAME}\" OWNER \"${DB_USERNAME}\""
else
  log "Database '${DB_NAME}' already exists, skipping creation."
fi

log "Granting all privileges on database '${DB_NAME}' to '${DB_USERNAME}'..."
psql -U postgres -h "$DB_HOST" -p "$DB_PORT" -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE \"${DB_NAME}\" TO \"${DB_USERNAME}\""

log "Database setup complete."

log "Updating NGINX configuration..."
NGINX_CONF="/etc/nginx/sites-enabled/default"
sed -i "s/listen\s\+[0-9]\+;/listen ${FRONTEND_PORT};/" "$NGINX_CONF"
sed -i "s|proxy_pass http://localhost:[0-9]\+/|proxy_pass http://localhost:${BACKEND_PORT}/|" "$NGINX_CONF"

log "Starting NGINX..."
nginx

log "Starting Spring Boot backend..."
exec java -jar /epibuilder/epibuilder-backend.jar \
  --server.port=${BACKEND_PORT} \
  --spring.datasource.url=jdbc:postgresql://$DB_HOST:${DB_PORT}/${DB_NAME} \
  --spring.datasource.username=${DB_USERNAME} \
  --spring.datasource.password=${DB_PASSWORD} \
  --jwt.secret=${JWT_SECRET}