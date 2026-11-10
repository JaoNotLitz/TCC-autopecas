#!/bin/bash
# Secrets (seção 3.7) e identidade do SES.
source "$(dirname "$0")/00-common.sh"

cria_secret() { # cria_secret <nome> <valor>  (não sobrescreve: a chave JWT sobrevive a reinícios)
  if awslocal secretsmanager describe-secret --secret-id "$1" >/dev/null 2>&1; then
    log "secret $1 já existe"
  else
    awslocal secretsmanager create-secret --name "$1" --secret-string "$2" >/dev/null
    log "secret $1 criado"
  fi
}

# Chave HS512 aleatória de 64 bytes, em Base64.
cria_secret "$SECRET_JWT" "$(head -c 64 /dev/urandom | base64 | tr -d '\n')"
cria_secret "$SECRET_REDIS" "${REDIS_PASSWORD:-wc-redis-local}"

awslocal ses verify-email-identity --email-address "$SES_SENDER" >/dev/null
log "identidade SES $SES_SENDER ok"
