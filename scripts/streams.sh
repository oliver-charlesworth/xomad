#!/usr/bin/env bash
set -eu


command="${1}"
case "${command}" in
  create)
    consul kv put quoter/streams/ABC
    consul kv put quoter/streams/DEF
    consul kv put quoter/streams/GHI
    consul kv put quoter/streams/JKL
    consul kv put quoter/streams/MNO
    consul kv put quoter/streams/PQR
    consul kv put quoter/streams/STU
    consul kv put quoter/streams/VWX
    ;;

  destroy)
    consul kv delete quoter/streams/ABC
    consul kv delete quoter/streams/DEF
    consul kv delete quoter/streams/GHI
    consul kv delete quoter/streams/JKL
    consul kv delete quoter/streams/MNO
    consul kv delete quoter/streams/PQR
    consul kv delete quoter/streams/STU
    consul kv delete quoter/streams/VWX
    ;;

  *)
    echo "Unsupported operation"
    exit 1
    ;;
esac
