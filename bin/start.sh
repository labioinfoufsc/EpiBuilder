#!/bin/bash

CONTAINER_NAME="epibuilder"
IMAGE_NAME="bioinfoufsc/epibuilder:latest"

# Check if the container already exists
if [ "$(docker ps -a -q -f name=^${CONTAINER_NAME}$)" ]; then
    echo "Container '${CONTAINER_NAME}' already exists."

    # Check if it's running
    if [ "$(docker ps -q -f name=^${CONTAINER_NAME}$)" ]; then
        echo "Container '${CONTAINER_NAME}' is already running."
    else
        echo "Starting existing container '${CONTAINER_NAME}'..."
        docker start  "${CONTAINER_NAME}"
    fi
else
    echo "Container '${CONTAINER_NAME}' not found. Creating and starting a new one..."
    docker run -d --name "${CONTAINER_NAME}" \
        -p 80:80 \
        -p 8080:8080 \
        -p 5435:5432 \
        "${IMAGE_NAME}"
fi

echo
echo "Waiting 15 seconds for the container to initialize..."
for i in {15..1}; do
    echo -ne "⏳ ${i} seconds remaining...\r"
    sleep 1
done
echo -e "\n"

echo
echo "✅ Visit the website at: http://localhost"
