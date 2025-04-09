#!/bin/bash

# Start MySQL
echo "Starting MySQL..."
service mysql start

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
until mysqladmin ping -h "localhost" --silent; do
    sleep 2
done

# Optionally initialize database
echo "Creating database 'epibuilder' if not exists..."
mysql -u root -e "CREATE DATABASE IF NOT EXISTS epibuilder;"

# Start NGINX
echo "Starting NGINX..."
service nginx start

# Start Spring Boot backend
echo "Starting backend..."
exec java -jar /epibuilder/epibuilder-backend.jar
