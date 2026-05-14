#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

PROJECT_BEPIPRED3="${REPO_ROOT}/src/bepipred3"
PROJECT_CORE="${REPO_ROOT}/src/core"
PROJECT_WEB="${REPO_ROOT}/src/web"

DOCKERFILE_BEPIPRED3="${PROJECT_BEPIPRED3}/Dockerfile"
DOCKERFILE_CORE="${PROJECT_CORE}/Dockerfile"
DOCKERFILE_WEB="${PROJECT_WEB}/Dockerfile"

IMAGE_BEPIPRED3="bioinfoufsc/bepipred3"
IMAGE_CORE="bioinfoufsc/epibuilder-core"
IMAGE_WEB="bioinfoufsc/epibuilder"

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

docker_build() {
    local dockerfile="$1"
    local image="$2"

    docker build \
        --no-cache \
        -f "$dockerfile" \
        -t "$image" \
        "$REPO_ROOT"
}

require_file "$DOCKERFILE_BEPIPRED3"
require_file "$DOCKERFILE_CORE"
require_file "$DOCKERFILE_WEB"

if confirm "Build Docker image (${IMAGE_BEPIPRED3})?"; then

    log "Building ${IMAGE_BEPIPRED3}"

    docker_build "$DOCKERFILE_BEPIPRED3" "$IMAGE_BEPIPRED3"

    echo "SUCCESS: ${IMAGE_BEPIPRED3}"
fi

if confirm "Build Docker image (${IMAGE_CORE})?"; then

    log "Building Docker image: ${IMAGE_CORE}"

    docker_build "$DOCKERFILE_CORE" "$IMAGE_CORE"

    echo "SUCCESS: ${IMAGE_CORE}"
fi

if confirm "Build Docker image (${IMAGE_WEB})?"; then

    log "Building Docker image: ${IMAGE_WEB}"

    docker_build "$DOCKERFILE_WEB" "$IMAGE_WEB"

    echo "SUCCESS: ${IMAGE_WEB}"
fi

log "Build process completed"