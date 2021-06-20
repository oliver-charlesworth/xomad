#!/usr/bin/env bash
set -eu

vagrant up

find jobs -type f | xargs -n1 nomad run

./streams.sh create
