#!/bin/bash
# ============================================
# DB-Doctor Docker Build and Push Script
# ============================================

set -e

# Switch to project root directory
cd "$(dirname "$0")/.."

# Read version from pom.xml
VERSION=$(grep -oP '(?<=<version>)[^<]+' "$PROJECT_ROOT/pom.xml" | head -1)
IMAGE_NAME="hanpf23/db-doctor"

echo ""
echo "============================================"
echo "  DB-Doctor Docker Build and Push"
echo "============================================"
echo ""
echo "Version: $VERSION"
echo "Image: $IMAGE_NAME:$VERSION"
echo ""

# Check Docker is running
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker is not running or not installed"
    echo "Please install Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Check if logged in to Docker Hub
echo "[1/4] Checking Docker Hub login..."
if ! docker info | grep -q "Username"; then
    echo "[WARN] Not logged in to Docker Hub"
    echo ""
    echo "Please login first:"
    echo "  docker login -u hanpf2391"
    echo ""
    read -p "Press Enter to login..."
    docker login -u hanpf2391
fi
echo "[OK] Docker Hub login checked"
echo ""

# Build Docker image
echo "[2/4] Building Docker image..."
echo "[INFO] This may take 10-15 minutes for first build..."
echo ""
docker build -t $IMAGE_NAME:$VERSION .
echo "[OK] Image built successfully"
echo ""

# Tag as latest
echo "[3/4] Tagging as latest..."
docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest
echo "[OK] Tagged as latest"
echo ""

# Push to Docker Hub
echo "[4/4] Pushing to Docker Hub..."
echo ""
echo "Pushing $IMAGE_NAME:$VERSION..."
docker push $IMAGE_NAME:$VERSION
echo ""
echo "Pushing $IMAGE_NAME:latest..."
docker push $IMAGE_NAME:latest || echo "[WARN] Push failed for latest (but $VERSION succeeded)"
echo ""

# Display results
echo "============================================"
echo "  Docker images pushed successfully!"
echo "============================================"
echo ""
echo "Image URLs:"
echo "  Docker Hub: https://hub.docker.com/r/hanpf2391/db-doctor"
echo ""
echo "Pull commands:"
echo "  docker pull $IMAGE_NAME:$VERSION"
echo "  docker pull $IMAGE_NAME:latest"
echo ""
echo "Run command:"
echo "  docker run -d -p 8080:8080 --name db-doctor $IMAGE_NAME:$VERSION"
echo ""
