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
            private_ip=$arg
            ;;
    esac
done

dnf config-manager --add-repo https://rpm.releases.hashicorp.com/fedora/hashicorp.repo
dnf config-manager --add-repo https://adoptopenjdk.jfrog.io/adoptopenjdk/rpm/fedora/31/x86_64
dnf install -y --nogpgcheck \
  unzip vim \
  adoptopenjdk-15-hotspot-jre \
  consul \
  nomad

# Configure Nomad
cp ${SOURCE_DIR}/nomad.service /etc/systemd/system/
sed "s/PRIVATE_IP/${private_ip}/g" ${SOURCE_DIR}/nomad.hcl > /etc/nomad.d/nomad.hcl
if [[ "$server" == 1 ]]; then
    cp ${SOURCE_DIR}/nomad-server.hcl /etc/nomad.d/server.hcl
fi

systemctl enable nomad
systemctl start nomad

# Configure Consul
cp ${SOURCE_DIR}/consul.service /etc/systemd/system/
sed "s/PRIVATE_IP/${private_ip}/g" ${SOURCE_DIR}/consul.hcl > /etc/consul.d/consul.hcl
if [[ "$server" == 1 ]]; then
    cp ${SOURCE_DIR}/consul-server.hcl /etc/consul.d/server.hcl
fi
chown consul: /etc/consul.d/*

systemctl enable consul
systemctl start consul


