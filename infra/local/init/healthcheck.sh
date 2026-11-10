#!/bin/bash
# Healthcheck do container do LocalStack: saudável só quando todos os scripts de
# ready.d terminaram e nenhum deu erro. Um script com erro deixa o container
# "unhealthy", e o "make up" para mostrando o problema.
estado=$(curl -sf localhost:4566/_localstack/init/ready) || exit 1
grep -q '"completed": true' <<<"$estado" || exit 1
if grep -q '"ERROR"' <<<"$estado"; then
  exit 1
fi
exit 0
