#!/bin/bash
# Certificado A1 autoassinado de teste (HU-B00.4 RF04) no secret wc-sefaz-certificate:
# {"pfx": "<.pfx em Base64>", "senha": "..."}. Só serve para o simulador da SEFAZ.
source "$(dirname "$0")/00-common.sh"

if awslocal secretsmanager describe-secret --secret-id "$SECRET_SEFAZ_CERT" >/dev/null 2>&1; then
  log "secret $SECRET_SEFAZ_CERT já existe"
  exit 0
fi

senha="${SEFAZ_CERT_SENHA:-wc-teste-a1}"
dir=$(mktemp -d)
openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 365 \
  -subj "/C=BR/O=ICP-Brasil/OU=Certificado de teste/CN=WC AUTOPECAS LTDA:12345678000195" \
  -keyout "$dir/chave.pem" -out "$dir/cert.pem" 2>/dev/null
openssl pkcs12 -export -inkey "$dir/chave.pem" -in "$dir/cert.pem" -name wc-a1 \
  -passout "pass:$senha" -out "$dir/a1.pfx"
pfx=$(base64 -w0 "$dir/a1.pfx")
awslocal secretsmanager create-secret --name "$SECRET_SEFAZ_CERT" \
  --secret-string "{\"pfx\":\"$pfx\",\"senha\":\"$senha\"}" >/dev/null
rm -r "$dir"
log "secret $SECRET_SEFAZ_CERT criado (A1 autoassinado, 365 dias)"
