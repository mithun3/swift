#!/bin/bash
set -euo pipefail

echo "1. Disabling Hyperthreading..."
echo off | sudo tee /sys/devices/system/cpu/smt/control || true

echo "2. Applying Enterprise Low-Latency Tuning (Tuned Daemon)..."
sudo dnf install -y tuned tuned-profiles-cpu-partitioning
sudo systemctl enable --now tuned

# Dynamically calculate the maximum core index (total cores - 1)
MAX_CORE=$(($(nproc) - 1))

# Configure tuned to isolate all cores except 0
sudo sed -i "s/^isolated_cores=.*/isolated_cores=1-${MAX_CORE}/" /etc/tuned/cpu-partitioning-variables.conf
sudo tuned-adm profile cpu-partitioning

echo "3. Installing Docker, Java, Maven, Python, and Dependencies..."
sudo dnf install -y dnf-plugins-core epel-release
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin git htop rsync java-21-openjdk-devel maven python3-pandas python3-matplotlib python3-numpy

sudo systemctl enable --now docker

# Add current user to docker group
if [ "$USER" != "root" ]; then
    sudo usermod -aG docker "$USER"
fi

echo "Done. Rebooting now for cpu-partitioning parameters to take effect..."
sudo reboot
