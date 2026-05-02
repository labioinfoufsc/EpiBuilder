#!/usr/bin/env bash

set -e

echo "Starting EpiBuilder backend development environment setup"

# -----------------------------------
# Utils
# -----------------------------------
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# -----------------------------------
# Base dependencies
# -----------------------------------
install_base() {
  echo "Installing base dependencies..."

  sudo apt update

  sudo apt install -y \
    curl \
    git \
    ca-certificates \
    gnupg \
    lsb-release \
    software-properties-common
}

# -----------------------------------
# Java 21
# -----------------------------------
install_java() {
  if command_exists java; then
    echo "Java is already installed:"
    java -version
  else
    echo "Installing OpenJDK 21..."

    sudo apt install -y openjdk-21-jdk

    echo "Java installed successfully"
  fi
}

# -----------------------------------
# Maven
# -----------------------------------
install_maven() {
  if command_exists mvn; then
    echo "Maven is already installed:"
    mvn -v
  else
    echo "Installing Maven..."

    sudo apt install -y maven

    echo "Maven installed successfully"
  fi
}

# -----------------------------------
# Docker
# -----------------------------------
install_docker() {
  if command_exists docker; then
    echo "Docker is already installed"
    return
  fi

  echo "Installing Docker..."

  sudo mkdir -p /etc/apt/keyrings

  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) \
    signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

  sudo apt update

  sudo apt install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

  sudo usermod -aG docker "$USER"

  sudo systemctl enable docker
  sudo systemctl start docker

  echo "Docker installed successfully"
  echo "Run 'newgrp docker' after this script finishes"
}

# -----------------------------------
# PostgreSQL container
# -----------------------------------
start_postgres() {
  if ! command_exists docker; then
    echo "Docker is required to start PostgreSQL"
    exit 1
  fi

  if docker ps -a --format '{{.Names}}' | grep -q "^epibuilder-postgres$"; then
    echo "PostgreSQL container already exists"

    docker start epibuilder-postgres >/dev/null 2>&1 || true

    echo "PostgreSQL container started"
  else
    echo "Creating PostgreSQL container..."

    docker run -d \
      --name epibuilder-postgres \
      -e POSTGRES_DB=epibuilder \
      -e POSTGRES_USER=epiuser \
      -e POSTGRES_PASSWORD=epiuser \
      -p 5432:5432 \
      postgres:15

    echo "PostgreSQL container created successfully"
  fi
}

# -----------------------------------
# Execute setup
# -----------------------------------
install_base
install_java
install_maven
install_docker
start_postgres

echo
echo "Checking JWT_SECRET..."

if grep -q "export JWT_SECRET=" ~/.bashrc; then
    echo "JWT_SECRET already configured."
else
    JWT_SECRET=$(openssl rand -base64 32)

    echo "" >> ~/.bashrc
    echo "# EpiBuilder JWT secret" >> ~/.bashrc
    echo "export JWT_SECRET=\"$JWT_SECRET\"" >> ~/.bashrc

    export JWT_SECRET="$JWT_SECRET"

    echo "JWT_SECRET generated and added to ~/.bashrc"
fi

echo ""
echo "Backend development environment is ready"
echo ""

echo "Database configuration:"
echo "spring.datasource.url=jdbc:postgresql://localhost:5432/epibuilder"
echo "spring.datasource.username=epiuser"
echo "spring.datasource.password=epiuser"

echo ""
echo "Run backend:"
echo "cd src/web/backend"
echo "mvn spring-boot:run"

echo ""
echo "Check PostgreSQL container:"
echo "docker ps"

echo ""
echo "If Docker permissions do not work, run:"
echo "newgrp docker"
