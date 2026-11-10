#!/bin/bash
# Filas da seção 6 e tópicos da seção 7 (mais o sales-events-topic da decisão A2 do backlog).
source "$(dirname "$0")/00-common.sh"
ACCOUNT=000000000000
REGION=$AWS_DEFAULT_REGION
q_url() { echo "http://sqs.$REGION.localhost.localstack.cloud:4566/$ACCOUNT/$1"; }
q_arn() { echo "arn:aws:sqs:$REGION:$ACCOUNT:$1"; }
t_arn() { echo "arn:aws:sns:$REGION:$ACCOUNT:$1"; }

# cria_fila <nome> <visibility> [dlq]   retenção: 4 dias (14 na DLQ)
cria_fila() {
  local nome=$1 vis=$2 dlq=${3:-} ret=345600
  [ "$nome" = "$QUEUE_FISCAL_DLQ" ] && ret=1209600
  local attrs="{\"VisibilityTimeout\":\"$vis\",\"MessageRetentionPeriod\":\"$ret\""
  if [ -n "$dlq" ]; then
    attrs="$attrs,\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$(q_arn "$dlq")\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\""
  fi
  attrs="$attrs}"
  # create-queue é idempotente quando os atributos são os mesmos; set-queue-attributes garante o estado.
  awslocal sqs create-queue --queue-name "$nome" >/dev/null
  awslocal sqs set-queue-attributes --queue-url "$(q_url "$nome")" --attributes "$attrs"
  log "fila $nome ok"
}

cria_fila "$QUEUE_FISCAL_DLQ" 30
cria_fila "$QUEUE_CUSTOMER_EVENTS" 30
cria_fila "$QUEUE_INVENTORY_UPDATES" 60 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_SALES_EVENTS" 30 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_CUSTOMER_SALES_EVENTS" 30 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_FISCAL_JOBS" 120 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_SEFAZ" 300 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_FISCAL_EVENTS" 300 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_ENTRY_EVENTS" 60 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_DOCUMENT_EVENTS" 60 "$QUEUE_FISCAL_DLQ"
cria_fila "$QUEUE_EMAIL" 30

for t in "$TOPIC_LOW_STOCK" "$TOPIC_SALES_NOTIFICATIONS" "$TOPIC_SYSTEM_NOTIFICATIONS" "$TOPIC_SALES_EVENTS"; do
  awslocal sns create-topic --name "$t" >/dev/null   # idempotente
  log "tópico $t ok"
done

# assina <tópico> <fila> [filter-policy-json]  (raw delivery: a fila recebe o JSON do evento)
assina() {
  local topico=$1 fila=$2 filtro=${3:-}
  local existente
  existente=$(awslocal sns list-subscriptions-by-topic --topic-arn "$(t_arn "$topico")" \
    --query "Subscriptions[?Endpoint=='$(q_arn "$fila")'].SubscriptionArn" --output text)
  if [ -z "$existente" ] || [ "$existente" = "None" ]; then
    existente=$(awslocal sns subscribe --topic-arn "$(t_arn "$topico")" --protocol sqs \
      --notification-endpoint "$(q_arn "$fila")" --query SubscriptionArn --output text)
  fi
  awslocal sns set-subscription-attributes --subscription-arn "$existente" \
    --attribute-name RawMessageDelivery --attribute-value true
  if [ -n "$filtro" ]; then
    awslocal sns set-subscription-attributes --subscription-arn "$existente" \
      --attribute-name FilterPolicy --attribute-value "$filtro"
  fi
  log "assinatura $topico -> $fila ok"
}

assina "$TOPIC_SALES_EVENTS" "$QUEUE_SALES_EVENTS"
assina "$TOPIC_SALES_EVENTS" "$QUEUE_CUSTOMER_SALES_EVENTS"
assina "$TOPIC_LOW_STOCK" "$QUEUE_EMAIL" '{"status":["critico"]}'
assina "$TOPIC_SALES_NOTIFICATIONS" "$QUEUE_EMAIL"
assina "$TOPIC_SYSTEM_NOTIFICATIONS" "$QUEUE_EMAIL" '{"severity":["critical","high"]}'
