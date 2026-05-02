#!/usr/bin/env bash

set -e

echo "Starting EpiBuilder frontend development environment setup"

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
    ca-certificates
}

# -----------------------------------
# NVM and Node.js
# -----------------------------------
install_node() {
  export NVM_DIR="$HOME/.nvm"

  if [ ! -d "$NVM_DIR" ]; then
    echo "Installing NVM..."

    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
  fi

  # Load NVM
  # shellcheck disable=SC1090
  source "$NVM_DIR/nvm.sh"

  echo "Installing Node.js 16..."

  nvm install 16
  nvm use 16
  nvm alias default 16

  echo "Node.js version:"
  node -v

  echo "NPM version:"
  npm -v
}

# -----------------------------------
# Angular CLI
# -----------------------------------
install_angular() {
  export NVM_DIR="$HOME/.nvm"

  # shellcheck disable=SC1090
  source "$NVM_DIR/nvm.sh"

  if command_exists ng; then
    echo "Angular CLI is already installed:"
    ng version
  else
    echo "Installing Angular CLI 12..."

    npm install -g @angular/cli@12

    echo "Angular CLI installed successfully"
  fi
}

# -----------------------------------
# Execute setup
# -----------------------------------
install_base
install_node
install_angular

echo ""
echo "Frontend development environment is ready"
echo ""

echo "Run frontend:"
echo "cd src/web/frontend"
echo "npm install"
echo "ng serve"

echo ""
echo "Frontend URL:"
echo "http://localhost:4200"

echo ""
echo "If 'ng' is not recognized, run:"
echo "source ~/.nvm/nvm.sh"
