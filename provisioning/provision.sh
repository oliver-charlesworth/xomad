#!/usr/bin/env bash
set -eu
SOURCE_DIR=/vagrant/provisioning

server=0

while (( "$#" )); do
  case $1 in
    -s|--server)
      server=1
      ;;
    *)
      exit 1
      ;;
  esac
  shift
done


echo "--------- Install packages ---------"

dnf config-manager --add-repo https://rpm.releases.hashicorp.com/fedora/hashicorp.repo
dnf config-manager --add-repo https://adoptopenjdk.jfrog.io/adoptopenjdk/rpm/fedora/31/x86_64
dnf install -y --nogpgcheck \
  unzip vim \
  adoptopenjdk-15-hotspot-jre \
  consul \
  nomad


echo "--------- Configure Nomad ---------"

envsubst < ${SOURCE_DIR}/nomad.service > /etc/systemd/system/
envsubst < ${SOURCE_DIR}/nomad.hcl > /etc/nomad.d/nomad.hcl
if [[ "$server" == 1 ]]; then
  envsubst < ${SOURCE_DIR}/nomad-server.hcl > /etc/nomad.d/server.hcl
fi

systemctl enable nomad
systemctl start nomad


echo "--------- Configure Consul ---------"

envsubst < ${SOURCE_DIR}/consul.service > /etc/systemd/system/
envsubst < ${SOURCE_DIR}/consul.hcl > /etc/consul.d/consul.hcl
if [[ "$server" == 1 ]]; then
  envsubst < ${SOURCE_DIR}/consul-server.hcl > /etc/consul.d/server.hcl
fi

chown consul: /etc/consul.d/*

systemctl enable consul
systemctl start consul


