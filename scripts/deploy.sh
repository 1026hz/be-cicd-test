#!/bin/bash
set -e

CONTAINER_NAME=backend-app
REGION=ap-northeast-2
ECR_URI=<ECR_REPO_URI>

echo "[0] Load image tag from file..."
IMAGE_TAG=$(cat ../image-tag.txt)

echo "[1] Authenticate with ECR..."
aws ecr get-login-password --region $REGION | \
  docker login --username AWS --password-stdin $ECR_URI

echo "[2] Stop and remove existing container if exists..."
docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

echo "[3] Pull image from ECR..."
docker pull $ECR_URI:$IMAGE_TAG

echo "[4] Run new container..."
docker run -d \
  --name $CONTAINER_NAME \
  -e SPRING_PROFILES_ACTIVE=prod \
  -p 8080:8080 \
  $ECR_URI:$IMAGE_TAG

echo "[✅] Deployment completed. [$ECR_URI:$IMAGE_TAG]"
