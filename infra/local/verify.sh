#!/bin/bash
# Confere se os recursos do init existem no LocalStack (usado no CI e em "make verify").
# Uso: ./verify.sh   (precisa do container wc-localstack no ar)
set -uo pipefail
cd "$(dirname "$0")"
set -a; source init/resources.env; set +a
AWS="docker exec wc-localstack awslocal"
falhas=0
confere() { # confere <descrição> <saída> <esperado>
  if grep -qw -- "$3" <<<"$2"; then echo "ok    $1 $3"; else echo "FALTA $1 $3"; falhas=$((falhas+1)); fi
}
tabelas=$($AWS dynamodb list-tables --output text)
for v in $(grep '^TABLE_' init/resources.env | cut -d= -f2); do confere tabela "$tabelas" "$v"; done
filas=$($AWS sqs list-queues --output text)
for v in $(grep '^QUEUE_' init/resources.env | cut -d= -f2); do confere fila "$filas" "$v"; done
topicos=$($AWS sns list-topics --output text)
for v in $(grep '^TOPIC_' init/resources.env | cut -d= -f2); do confere tópico "$topicos" "$v"; done
buckets=$($AWS s3 ls)
for v in $(grep '^BUCKET_' init/resources.env | cut -d= -f2); do confere bucket "$buckets" "$v"; done
secrets=$($AWS secretsmanager list-secrets --query 'SecretList[].Name' --output text)
for v in $(grep '^SECRET_' init/resources.env | grep -v SEFAZ | cut -d= -f2); do confere secret "$secrets" "$v"; done
redrive=$($AWS sqs get-queue-attributes --queue-url "http://localhost:4566/000000000000/$QUEUE_SEFAZ" \
  --attribute-names RedrivePolicy --output text)
confere "redrive sefaz-queue ->" "$redrive" "$QUEUE_FISCAL_DLQ"
if [ $falhas -gt 0 ]; then echo "$falhas recurso(s) faltando"; exit 1; fi
echo "todos os recursos presentes"
