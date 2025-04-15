#!/bin/bash

# Start MariaDB/MySQL (in background)
echo "Starting MariaDB..."
/usr/bin/mysqld_safe --datadir='/var/lib/mysql' &

# Wait for MySQL to be ready
echo "Waiting for MariaDB to be ready..."
until mysqladmin ping -h "127.0.0.1" --silent; do
    sleep 2
done
echo "MariaDB is ready!"

# Create database and user
echo "Creating database 'epibuilder' and configuring user..."
mysql -u root <<-EOSQL
    CREATE DATABASE IF NOT EXISTS epibuilder;
    CREATE USER IF NOT EXISTS 'epiuser' IDENTIFIED BY 'epipassword';
    GRANT ALL PRIVILEGES ON epibuilder.* TO 'epiuser';
    FLUSH PRIVILEGES;
EOSQL

# Start NGINX
echo "Starting NGINX..."
nginx
echo "NGINX started!"

# Start the backend application
echo "Starting backend..."
exec java -jar /epibuilder/epibuilder-backend.jar
echo "Backend started!"
