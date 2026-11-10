#!/bin/bash
# Marca o fim do init; o healthcheck do container olha este arquivo.
source "$(dirname "$0")/00-common.sh"
touch /tmp/wc-init-done
log "init concluído"
