#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME=ima-backend:prod
docker build -t ${IMAGE_NAME} .
docker rm -f ima-backend || true
docker run -d --name ima-backend -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v $(pwd)/scripts:/app/scripts \
  -v $(pwd)/downloads:/app/downloads \
  --restart unless-stopped \
  ${IMAGE_NAME}
