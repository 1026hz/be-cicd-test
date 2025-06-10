#!/bin/bash

CONTAINER_NAME=backend-app

echo "[1] Stop and remove existing container if exists..."
docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

echo "[2] Pull latest image from ECR..."
docker pull <ECR_REPO_URI>:<IMAGE_TAG>

echo "[3] Run new container..."
docker run -d \
  --name $CONTAINER_NAME \
  -e SPRING_PROFILES_ACTIVE=prod \
  -p 8080:8080 \
  <ECR_REPO_URI>:<IMAGE_TAG>

echo "[✅] Deployment completed."