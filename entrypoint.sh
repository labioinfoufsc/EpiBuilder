#!/bin/bash

set -e

echo "🔐 Generating JWT Secret..."
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')
echo "export JWT_SECRET=\"$JWT_SECRET\"" | tee -a /etc/environment >/dev/null
export JWT_SECRET="$JWT_SECRET"

export DB_USER=epiuser
export DB_PASS=epiuser
export DB_NAME=epibuilder

echo "export DB_USER=\"$DB_USER\"" | tee -a /etc/environment >/dev/null
echo "export DB_PASS=\"$DB_PASS\"" | tee -a /etc/environment >/dev/null
echo "export DB_NAME=\"$DB_NAME\"" | tee -a /etc/environment >/dev/null

echo "Starting MariaDB..."
/usr/bin/mysqld_safe --datadir='/var/lib/mysql' &

echo "Waiting for MariaDB to be ready..."
until mysqladmin ping -h "127.0.0.1" --silent; do
    sleep 2
done

echo "Creating database 'epibuilder' and configuring user..."
mysql -u root <<-EOSQL
    CREATE DATABASE IF NOT EXISTS epibuilder;
    CREATE USER IF NOT EXISTS 'epiuser'@'localhost' IDENTIFIED BY 'epiuser';
    GRANT ALL PRIVILEGES ON epibuilder.* TO 'epiuser'@'localhost';
    FLUSH PRIVILEGES;
EOSQL
echo "Database and user configured."

echo "🔄 Starting NGINX..."
nginx
echo "NGINX started."

echo "🚀 Starting Sprint Boot..."
exec java -jar /epibuilder/epibuilder-backend.jar \
    --spring.datasource.url=jdbc:mariadb://localhost:3306/$DB_NAME \
    --spring.datasource.username=$DB_USER \
    --spring.datasource.password=$DB_PASS \
    --jwt.secret=$JWT_SECRET
