#!/usr/bin/env bash
set -eu


command="${1}"
case "${command}" in
  create)
    for x in {A..Z}; do
      consul kv put quoter/streams/${x}
    done
    ;;

  destroy)
    for x in {A..Z}; do
      consul kv delete quoter/streams/${x}
    done
    ;;

  *)
    echo "Unsupported operation"
    exit 1
    ;;
esac
