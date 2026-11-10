#!/bin/bash
# Funções usadas pelos scripts de init. Os hooks do LocalStack rodam em ordem alfabética,
# então este arquivo só define funções; os scripts 10-* em diante fazem source dele.
set -euo pipefail
INIT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
set -a; source "$INIT_DIR/resources.env"; set +a
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-sa-east-1}"
log() { echo "[wc-init] $*"; }
# Sem paginador e sem retries longos: deixa cada chamada do awslocal mais rápida.
export AWS_PAGER="" AWS_MAX_ATTEMPTS=2
