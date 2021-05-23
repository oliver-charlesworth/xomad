#!/usr/bin/env bash
set -eu
SOURCE_DIR=/vagrant/provisioning

server=0

for arg in "$@"; do
    case $arg in
        -s|--server)
            server=1
            ;;
        *)
            ;;
    esac
done

dnf config-manager --add-repo https://rpm.releases.hashicorp.com/fedora/hashicorp.repo
dnf config-manager --add-repo https://adoptopenjdk.jfrog.io/adoptopenjdk/rpm/fedora/31/x86_64
dnf install -y --nogpgcheck \
  avahi nss-mdns \
  adoptopenjdk-15-hotspot-jre \
  nomad

cp ${SOURCE_DIR}/nomad.service /etc/systemd/system/
cp ${SOURCE_DIR}/nomad.hcl /etc/nomad.d/
cp ${SOURCE_DIR}/client.hcl /etc/nomad.d/
if [[ "$server" == 1 ]]; then
    cp ${SOURCE_DIR}/server.hcl /etc/nomad.d/
fi

systemctl enable nomad
systemctl start nomad

# Needed for DNS - maybe remove this and hardcode IPs
systemctl enable avahi-daemon
systemctl start avahi-daemon
