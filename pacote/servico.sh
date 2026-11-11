#!/bin/sh
# Entrada da imagem wc-servicos: carrega os nomes dos recursos do LocalStack e roda o jar de $SERVICO.
set -e
: "${SERVICO:?defina SERVICO (ex.: auth-service)}"
set -a; . /app/resources.env; set +a
exec java $JAVA_OPTS -jar "/app/$SERVICO.jar"
