#!/bin/bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
    echo "ERROR: This script must be run as root (sudo bash $0)"
    exit 1
fi

echo "============================================================"
echo "  FX Pipeline — Baremetal OS Provisioning & Latency Tuning"
echo "  Host: $(hostname)  Kernel: $(uname -r)"
echo "============================================================"

echo ""
echo "1. Disabling Hyperthreading (SMT)..."
SMT_CONTROL=/sys/devices/system/cpu/smt/control
if [ -f "$SMT_CONTROL" ]; then
    echo off > "$SMT_CONTROL"
    echo "   SMT disabled. (NOTE: This resets on reboot unless disabled in BIOS)."
fi

echo ""
echo "2. Setting THP policy to 'madvise' (disables khugepaged on mmap regions)..."
THP_ENABLED=/sys/kernel/mm/transparent_hugepage/enabled
THP_DEFRAG=/sys/kernel/mm/transparent_hugepage/defrag
if [ -f "$THP_ENABLED" ]; then
    echo madvise > "$THP_ENABLED"
    echo "defer+madvise" > "$THP_DEFRAG"
fi

echo ""
echo "3. Setting CPU governor to 'performance' on all cores..."
for gov_file in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    if [ -f "$gov_file" ]; then
        echo performance > "$gov_file" 2>/dev/null || echo "   Warning: Could not set performance governor on $gov_file (Device or resource busy)"
    fi
done

echo ""
echo "4. Applying Enterprise Low-Latency Tuning (Tuned Daemon)..."
dnf install -y tuned tuned-profiles-cpu-partitioning
systemctl enable --now tuned

# Dynamically calculate the maximum core index (total cores - 1)
# Note: nproc reflects available logical cores. With SMT off on a 6-core machine, nproc is 6, max is 5.
MAX_CORE=$(($(nproc) - 1))
sed -i "s/^isolated_cores=.*/isolated_cores=1-${MAX_CORE}/" /etc/tuned/cpu-partitioning-variables.conf
tuned-adm profile cpu-partitioning

echo ""
echo "5. Patching GRUB for absolute CPU isolation and C-state disabling..."
GRUB_FILE=/etc/default/grub
REQUIRED_PARAMS="isolcpus=1-${MAX_CORE} nohz_full=1-${MAX_CORE} rcu_nocbs=1-${MAX_CORE} intel_idle.max_cstate=0 processor.max_cstate=0 idle=poll"

if [ -f "$GRUB_FILE" ]; then
    CURRENT_LINE=$(grep '^GRUB_CMDLINE_LINUX_DEFAULT=' "$GRUB_FILE" | head -1)
    NEEDS_UPDATE=false
    for param in $REQUIRED_PARAMS; do
        if ! echo "$CURRENT_LINE" | grep -qF "$param"; then
            NEEDS_UPDATE=true
            break
        fi
    done

    if [ "$NEEDS_UPDATE" = true ]; then
        cp "$GRUB_FILE" "${GRUB_FILE}.bak.$(date +%Y%m%d%H%M%S)"
        sed -i "s|^GRUB_CMDLINE_LINUX_DEFAULT=\"\(.*\)\"|GRUB_CMDLINE_LINUX_DEFAULT=\"\1 $REQUIRED_PARAMS\"|" "$GRUB_FILE"
        echo "   Rebuilding GRUB config..."
        grub2-mkconfig -o /boot/grub2/grub.cfg
    else
        echo "   All required GRUB parameters already present."
    fi
else
    echo "   WARNING: $GRUB_FILE not found."
fi

echo ""
echo "6. Installing Docker, Java, Maven, Python, and Dependencies..."
dnf install -y dnf-plugins-core epel-release
dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin git htop rsync java-21-openjdk-devel maven python3-pandas python3-matplotlib python3-numpy

systemctl enable --now docker

if [ "${SUDO_USER:-}" != "" ] && [ "${SUDO_USER}" != "root" ]; then
    usermod -aG docker "${SUDO_USER}"
fi

echo ""
echo "============================================================"
echo "Provisioning complete."
echo "A REBOOT IS REQUIRED to activate the GRUB kernel parameters."
echo "============================================================"
