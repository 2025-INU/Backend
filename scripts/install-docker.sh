#!/bin/bash

# ===================================
# Docker Engine & Docker Compose Installation Script
# For Ubuntu/Debian-based EC2 Instances
# ===================================

set -e  # Exit on error

echo "======================================"
echo "Docker Installation Script"
echo "======================================"
echo ""

# Check if running as root
if [ "$EUID" -eq 0 ]; then
  echo "❌ Please run this script as a normal user, not as root."
  echo "   The script will use sudo when needed."
  exit 1
fi

# Update package index
echo "📦 Updating package index..."
sudo apt-get update

# Install prerequisites
echo "📦 Installing prerequisites..."
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Add Docker's official GPG key
echo "🔑 Adding Docker's official GPG key..."
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Set up Docker repository
echo "📚 Setting up Docker repository..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Update package index again
echo "📦 Updating package index with Docker repository..."
sudo apt-get update

# Install Docker Engine and Docker Compose
echo "🐳 Installing Docker Engine and Docker Compose..."
sudo apt-get install -y \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin

# Add current user to docker group
echo "👤 Adding current user to docker group..."
sudo usermod -aG docker $USER

# Enable and start Docker service
echo "🚀 Enabling and starting Docker service..."
sudo systemctl enable docker
sudo systemctl start docker

# Verify installation
echo ""
echo "======================================"
echo "✅ Installation Complete!"
echo "======================================"
echo ""
echo "Docker version:"
docker --version

echo ""
echo "Docker Compose version:"
docker compose version

echo ""
echo "⚠️  IMPORTANT: You need to log out and log back in for group changes to take effect."
echo "   Or run: newgrp docker"
echo ""
echo "🔍 Testing Docker (requires new shell session)..."
echo "   After logout/login, test with: docker run hello-world"
echo ""
