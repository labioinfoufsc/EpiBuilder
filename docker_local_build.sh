#!/bin/bash
set -euo pipefail

# Path to the directory where the Dockerfiles are located
project_root="$(cd "$(dirname "$0")" && pwd)"
dockerfile_ubuntu="$project_root/Dockerfile.ubuntu"
dockerfile_debian="$project_root/Dockerfile.debian"

# Image names
image_name_ubuntu="epibuilder:ubuntu"
image_name_debian="epibuilder:debian"

# Check and build Dockerfile.ubuntu
if [[ -f "$dockerfile_ubuntu" ]]; then
    read -p "Do you want to build the image using Dockerfile.ubuntu? [y/N]: " build_ubuntu
    if [[ "${build_ubuntu,,}" == "y" ]]; then
        docker build -f "$dockerfile_ubuntu" -t "$image_name_ubuntu" "$project_root"
        echo "Image '$image_name_ubuntu' built successfully."

        docker run -it --rm \
          -p 80:80 \
          -p 8080:8080 \
          -p 5432:5432 \
          --name epibuilder_container \
          "$image_name_ubuntu"
    fi
else
    echo "Dockerfile.ubuntu not found at: $dockerfile_ubuntu"
fi

# Check and build Dockerfile.debian
if [[ -f "$dockerfile_debian" ]]; then
    read -p "Do you want to build the image using Dockerfile.debian? [y/N]: " build_debian
    if [[ "${build_debian,,}" == "y" ]]; then
        docker build -f "$dockerfile_debian" -t "$image_name_debian" "$project_root"
        echo "Image '$image_name_debian' built successfully."

        docker run -it --rm \
          -p 80:80 \
          -p 8080:8080 \
          -p 5432:5432 \
          --name epibuilder_container \
          "$image_name_debian"
    fi
else
    echo "Dockerfile.debian not found at: $dockerfile_debian"
fi