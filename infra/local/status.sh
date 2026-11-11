#!/bin/bash
# Painel de saúde da stack local (HU-B16.4): health de cada serviço, tamanho das filas e da DLQ,
# simulador da SEFAZ e Lambdas. Sai com 1 se algo estiver fora do ar (usado no CI).
# Uso: ./status.sh   (ou make status)
set -uo pipefail
cd "$(dirname "$0")"
set -a; source init/resources.env; set +a
AWS="docker exec wc-localstack awslocal"
problemas=0
linha() { printf '  %-28s %s\n' "$1" "$2"; }

echo "Serviços"
for s in auth-service:8080 customer-service:8081 product-service:8082 sales-service:8083 fiscal-service:8084 \
         entry-service:8085 document-service:8086 reports-service:8087 notification-service:8088; do
  nome=${s%%:*}; porta=${s##*:}
  estado=$(curl -s -m 3 "http://localhost:$porta/actuator/health" | sed -nE 's/^\{"status":"([A-Z_]+)".*/\1/p')
  [ -z "$estado" ] && estado=DOWN
  [ "$estado" = UP ] || problemas=$((problemas + 1))
  linha "$nome" "$estado"
done
sefaz=$(curl -s -m 3 -o /dev/null -w '%{http_code}' http://localhost:8089/__admin/health)
[ "$sefaz" = 200 ] && linha sefaz-mock UP || { linha sefaz-mock DOWN; problemas=$((problemas + 1)); }
gw=$(curl -s -m 3 -o /dev/null -w '%{http_code}' -X POST \
  http://localhost:4566/restapis/wcapi/local/_user_request_/api/auth/login -H 'Content-Type: application/json' -d '{}')
[ "$gw" = 400 ] && linha "api gateway" UP || { linha "api gateway" "DOWN ($gw)"; problemas=$((problemas + 1)); }

echo
if ! $AWS sqs list-queues >/dev/null 2>&1; then
  echo "LocalStack fora do ar"; exit 1
fi
echo "Filas (visíveis / em processamento)"
for fila in $(grep '^QUEUE_' init/resources.env | cut -d= -f2); do
  attrs=$($AWS sqs get-queue-attributes --queue-url "http://localhost:4566/000000000000/$fila" \
    --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
    --query 'Attributes.[ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible]' --output text 2>/dev/null)
  set -- $attrs
  marca=""
  [ "$fila" = "$QUEUE_FISCAL_DLQ" ] && [ "${1:-0}" -gt 0 ] && marca="  <- veja: make redrive"
  linha "$fila" "${1:-?} / ${2:-?}$marca"
done

echo
echo "Lambdas"
for f in wc-jwt-authorizer wc-dlq-monitor wc-dlq-redrive; do
  estado=$($AWS lambda get-function --function-name "$f" --query Configuration.State --output text 2>/dev/null)
  [ "$estado" = Active ] || problemas=$((problemas + 1))
  linha "$f" "${estado:-ausente}"
done
regra=$($AWS events describe-rule --name wc-dlq-monitor-cada-minuto --query State --output text 2>/dev/null)
linha "agendamento monitor" "${regra:-ausente}"

echo
if [ $problemas -gt 0 ]; then echo "$problemas problema(s)"; exit 1; fi
echo "tudo no ar"
