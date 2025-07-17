#!/bin/bash

set -e

export FRONTEND_PORT="${FRONTEND_PORT:-80}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"
export DB_PORT="${DB_PORT:-3306}"

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

echo "Starting MariaDB on port ${DB_PORT}..."
sed -i "s/^port\s*=.*/port = ${DB_PORT}/" /etc/mysql/my.cnf || true
/usr/bin/mysqld_safe --datadir='/var/lib/mysql' --port=${DB_PORT} &

echo "Waiting for MariaDB to be ready..."
until mysqladmin ping -h "127.0.0.1" --port=$DB_PORT --silent; do
    sleep 2
done

echo "Configuring database..."
mysql -u root -P $DB_PORT <<-EOSQL
    CREATE DATABASE IF NOT EXISTS ${DB_NAME};
    CREATE USER IF NOT EXISTS '${DB_USERNAME}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USERNAME}'@'localhost';
    FLUSH PRIVILEGES;
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
    --spring.datasource.url=jdbc:mariadb://${DB_HOST}:${DB_PORT}/${DB_NAME} \
    --spring.datasource.username=${DB_USERNAME} \
    --spring.datasource.password=${DB_PASSWORD} \
    --jwt.secret=${JWT_SECRET}