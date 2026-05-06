#!/usr/bin/env bash

set -Eeuo pipefail

# ============================================================
# EpiBuilder Docker Build Script
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ============================================================
# Paths
# ============================================================

PROJECT_BEPIPRED3="${REPO_ROOT}/src/bepipred3"
PROJECT_CORE="${REPO_ROOT}/src/core"
PROJECT_WEB="${REPO_ROOT}/src/web"

DOCKERFILE_BEPIPRED3="${PROJECT_BEPIPRED3}/Dockerfile"
DOCKERFILE_CORE="${PROJECT_CORE}/Dockerfile"
DOCKERFILE_WEB="${PROJECT_WEB}/Dockerfile"

# ============================================================
# Images
# ============================================================

IMAGE_BEPIPRED3="bioinfoufsc/bepipred3"
IMAGE_CORE="bioinfoufsc/epibuilder-core"
IMAGE_WEB="bioinfoufsc/epibuilder"

# ============================================================
# Helpers
# ============================================================

log() {
    echo
    echo "============================================================"
    echo "$1"
    echo "============================================================"
}

confirm() {
    local prompt="$1"

    read -r -p "$prompt [y/N]: " response

    [[ "${response,,}" == "y" ]]
}

require_file() {
    local file="$1"

    if [[ ! -f "$file" ]]; then
        echo "ERROR: File not found:"
        echo "  $file"
        exit 1
    fi
}

docker_maven_build() {
    local workdir="$1"
    local mvn_args="$2"

    docker run --rm \
        -v "${workdir}:/src" \
        -w /src \
        -v "${HOME}/.m2:/root/.m2" \
        maven:3.9-eclipse-temurin-21 \
        mvn ${mvn_args}
}

docker_build() {
    local dockerfile="$1"
    local image="$2"

    docker build \
        --no-cache \
        -f "$dockerfile" \
        -t "$image" \
        "$REPO_ROOT"
}

# ============================================================
# Validation
# ============================================================

require_file "$DOCKERFILE_BEPIPRED3"
require_file "$DOCKERFILE_CORE"
require_file "$DOCKERFILE_WEB"

# ============================================================
# Build Bepipred3
# ============================================================

if confirm "Build Docker image (${IMAGE_BEPIPRED3})?"; then

    log "Building ${IMAGE_BEPIPRED3}"

    docker_build "$DOCKERFILE_BEPIPRED3" "$IMAGE_BEPIPRED3"

    echo "SUCCESS: ${IMAGE_BEPIPRED3}"
fi

# ============================================================
# Build Core
# ============================================================

if confirm "Build Docker image (${IMAGE_CORE})?"; then

    log "Building Maven project: core"

    docker_maven_build \
        "$PROJECT_CORE" \
        "clean install -DskipTests"

    log "Building Docker image: ${IMAGE_CORE}"

    docker_build "$DOCKERFILE_CORE" "$IMAGE_CORE"

    echo "SUCCESS: ${IMAGE_CORE}"
fi

# ============================================================
# Build Web
# ============================================================

if confirm "Build Docker image (${IMAGE_WEB})?"; then

    log "Building Maven project: backend"

    docker_maven_build \
        "${PROJECT_WEB}/backend" \
        "clean install -DskipTests"

    log "Building Docker image: ${IMAGE_WEB}"

    docker_build "$DOCKERFILE_WEB" "$IMAGE_WEB"

    echo "SUCCESS: ${IMAGE_WEB}"
fi

log "Build process completed"
