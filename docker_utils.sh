#!/bin/bash

set -euo pipefail

echo "Stopping all containers..."
docker ps -q | grep . && docker stop $(docker ps -q) || echo "No running containers found."

echo "Images are not processes and cannot be stopped. Skipping."

read -p "Do you want to remove all containers? (y/n): " confirm_containers
if [ "$confirm_containers" = "y" ]; then
  docker ps -a -q | grep . && docker rm $(docker ps -a -q) || echo "No containers to remove."
else
  echo "Container removal canceled."
fi

read -p "Do you want to remove all images? (y/n): " confirm_images
if [ "$confirm_images" = "y" ]; then
  docker images -q | grep . && docker rmi -f $(docker images -q) || echo "No images to remove."
else
  echo "Image removal canceled."
fi

read -p "Do you want to run 'docker system prune' to clean up unused data? (y/n): " confirm_prune
if [ "$confirm_prune" = "y" ]; then
  docker system prune -f
else
  echo "System prune canceled."
fi