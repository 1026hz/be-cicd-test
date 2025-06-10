#!/bin/bash
set -e

CONTAINER_NAME=backend-app
IMAGE_INFO_FILE="/home/ubuntu/imageDetail.json"

echo "[1] Load image details from file..."
IMAGE_TAG=$(jq -r .imageTag "$IMAGE_INFO_FILE")
ECR_URI=$(jq -r .ecrRepo "$IMAGE_INFO_FILE")
AWS_REGION=$(jq -r .awsRegion "$IMAGE_INFO_FILE")

echo "[2] Authenticate with ECR..."
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$ECR_URI"

echo "[3] Stop and remove existing container if exists..."
docker stop $CONTAINER_NAME || true
docker rm $CONTAINER_NAME || true

echo "[4] Pull image from ECR..."
docker pull $ECR_URI:$IMAGE_TAG

echo "[5] Run new container..."
docker run -d \
  --name $CONTAINER_NAME \
  -e SPRING_PROFILES_ACTIVE=prod \
  -p 8080:8080 \
  $ECR_URI:$IMAGE_TAG

echo "[✅] Deployment completed. [$ECR_URI:$IMAGE_TAG]"
