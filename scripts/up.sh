#!/usr/bin/env bash
set -eu

vagrant up

# Clear-up
nomad job status | grep -v ID | grep -v "No running jobs" | awk '{print $1}' | xargs -L1 nomad stop -purge
nomad system gc
./scripts/streams.sh destroy

find jobs -type f | xargs -n1 nomad run

./scripts/streams.sh create
