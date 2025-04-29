#!/bin/bash

# JWT Secret Generator and Environment Setup Script for Ubuntu
# Description: Generates a secure JWT secret and configures it as a system environment variable
# Usage: sudo ./generate_jwt_secret.sh

# Generate a cryptographically secure JWT secret (32 bytes Base64 encoded)
# Using OpenSSL's CSPRNG (Cryptographically Secure Pseudorandom Number Generator)
JWT_SECRET=$(openssl rand -base64 32 | tr -d '\n')

# 1. Permanent System-wide Configuration:
# Append the JWT_SECRET to /etc/environment which loads for all users and services
# Using sudo tee to write to protected system file
echo "export JWT_SECRET=\"$JWT_SECRET\"" | sudo tee -a /etc/environment >/dev/null

# 2. Current Session Configuration:
# Export the variable for immediate use in the current shell session
export JWT_SECRET="$JWT_SECRET"
export DB_USER=epiuser
export DB_PASS=epiuser

# Display success message with usage instructions
echo "----------------------------------------"
echo "JWT_SECRET successfully generated!"
echo "----------------------------------------"
echo "Added to /etc/environment as:"
echo "export JWT_SECRET=\"*********\" (hidden for security)"
echo ""
echo "To use in Spring Boot's application.properties:"
echo "jwt.secret=\${JWT_SECRET}"
echo "----------------------------------------"

# Reload the environment variables
# This makes the variable available without requiring a reboot
source /etc/environment

# Start MariaDB/MySQL (in background)
echo "Starting MariaDB..."
/usr/bin/mysqld_safe --datadir='/var/lib/mysql' &

# Wait for MySQL to be ready
echo "Waiting for MariaDB to be ready..."
until mysqladmin ping -h "127.0.0.1" --silent; do
    sleep 2
done

# Create database and set up privileges
echo "Creating database 'epibuilder' and configuring user..."
mysql -u root <<-EOSQL
    CREATE DATABASE IF NOT EXISTS epibuilder;
    CREATE USER IF NOT EXISTS 'epiuser' IDENTIFIED BY 'epiuser';
    GRANT ALL PRIVILEGES ON epibuilder.* TO 'epiuser';
    FLUSH PRIVILEGES;
EOSQL

# Start NGINX
echo "Starting NGINX..."
nginx
echo "NGINX started."

# Wait for NGINX to be ready
until curl -s http://localhost:8080/ >/dev/null; do
    sleep 2
done

# Start Spring Boot backend
echo "Starting backend..."
exec java -jar /epibuilder/epibuilder-backend.jar
