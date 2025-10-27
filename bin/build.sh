#!/bin/bash
set -euo pipefail

project_bepipred3="../src/bepipred3"
project_core="../src/core"
project_root="../src/web"

dockerfile_bepipred3="$project_bepipred3/Dockerfile"
dockerfile_core="$project_core/Dockerfile"
dockerfile="$project_root/Dockerfile"

image_bepipred3_name="bioinfoufsc/bepipred3"
image_core_name="bioinfoufsc/epibuilder-core"
image_name="bioinfoufsc/epibuilder"


# Check and build Dockerfile.debian
if [[ -f "$dockerfile" ]]; then
    read -p "Do you want to build the image using Dockerfile (bepipred3)? [y/N]: " build
    if [[ "${build,,}" == "y" ]]; then
        docker build --no-cache -f "$dockerfile_bepipred3" -t "$image_bepipred3_name" "$project_bepipred3"
        echo "Image '$image_bepipred3_name' built successfully."
    fi
else
    echo "Dockerfile not found at: $dockerfile_bepipred3"
fi

if [[ -f "$dockerfile" ]]; then
    read -p "Do you want to build the image using Dockerfile (epibuilder-core)? [y/N]: " build
    if [[ "${build,,}" == "y" ]]; then
    	cd ../src/core/
    	docker run --rm -v ${PWD}:/src -w /src -v ${HOME}/.m2/:/.m2/ maven:3.9-eclipse-temurin-21 mvn clean install -Dmaven.repo.local=/.m2/
	    cd ../../bin
      	docker build --no-cache -f "$dockerfile_core" -t "$image_core_name" "$project_core"
      echo "Image '$image_core_name' built successfully."
    fi
else
    echo "Dockerfile not found at: $dockerfile_core"
fi

# Check and build Dockerfile.debian
if [[ -f "$dockerfile" ]]; then
    read -p "Do you want to build the image using Dockerfile (epibuilder)? [y/N]: " build
    if [[ "${build,,}" == "y" ]]; then
      cd ../src/web/backend
      docker run --rm -v ${PWD}:/src -w /src -v ${HOME}/.m2/:/.m2/ maven:3.9-eclipse-temurin-21 mvn clean install -Dmaven.repo.local=/.m2/ -DskipTests
      cd ../../../bin
      docker build --no-cache -f "$dockerfile" -t "$image_name" "$project_root"
      echo "Image '$image_name' built successfully."
    fi
else
    echo "Dockerfile not found at: $dockerfile"
fi
