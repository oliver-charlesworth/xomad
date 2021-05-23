#!/usr/bin/env bash
set -eu

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
dnf -y install avahi nss-mdns nomad

cp /vagrant/nomad.service /etc/systemd/system/
cp /vagrant/nomad.hcl /etc/nomad.d/
cp /vagrant/client.hcl /etc/nomad.d/
if [[ "$server" == 1 ]]; then
    cp /vagrant/server.hcl /etc/nomad.d/
fi

systemctl enable nomad
systemctl start nomad

systemctl enable avahi-daemon
systemctl start avahi-daemon