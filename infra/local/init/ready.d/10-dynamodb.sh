#!/bin/bash
# Tabelas DynamoDB da seção 3.4 da arquitetura (on-demand, TTL em "ttl").
# Streams: nenhum serviço consome os streams ainda, e ligá-los faz o LocalStack baixar e subir o
# kinesis-local (mais ~1 min de subida). Por isso ficam desligados; DYNAMO_STREAMS=1 liga.
source "$(dirname "$0")/00-common.sh"
DYNAMO_STREAMS="${DYNAMO_STREAMS:-0}"

existe() { awslocal dynamodb describe-table --table-name "$1" >/dev/null 2>&1; }

# cria_tabela <nome> <atributos> <key-schema> [gsis-json] [streams: 1|0]
cria_tabela() {
  local nome=$1 attrs=$2 keys=$3 gsis=${4:-} streams=${5:-1}
  if existe "$nome"; then log "tabela $nome já existe"; return; fi
  local args=(--table-name "$nome" --attribute-definitions $attrs --key-schema $keys --billing-mode PAY_PER_REQUEST)
  [ -n "$gsis" ] && args+=(--global-secondary-indexes "$gsis")
  [ "$streams" = 1 ] && [ "$DYNAMO_STREAMS" = 1 ] && \
    args+=(--stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES)
  if ! saida=$(awslocal dynamodb create-table "${args[@]}" 2>&1 >/dev/null); then
    # Outra execução do init pode ter criado a tabela entre o describe e o create.
    grep -q ResourceInUseException <<<"$saida" || { echo "$saida" >&2; return 1; }
  fi
  awslocal dynamodb update-time-to-live --table-name "$nome" \
    --time-to-live-specification Enabled=true,AttributeName=ttl >/dev/null || true
  log "tabela $nome criada"
}

gsi() { # gsi <nome> <hash> [range]
  local k="{\"AttributeName\":\"$2\",\"KeyType\":\"HASH\"}"
  [ -n "${3:-}" ] && k="$k,{\"AttributeName\":\"$3\",\"KeyType\":\"RANGE\"}"
  echo "{\"IndexName\":\"$1\",\"KeySchema\":[$k],\"Projection\":{\"ProjectionType\":\"ALL\"}}"
}

cria_tabela "$TABLE_COUNTERS" "AttributeName=recurso,AttributeType=S" "AttributeName=recurso,KeyType=HASH" "" 0

cria_tabela "$TABLE_USERS" "AttributeName=id,AttributeType=N AttributeName=email,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" "[$(gsi email-index email)]"

cria_tabela "$TABLE_CUSTOMERS" \
  "AttributeName=id,AttributeType=N AttributeName=documento,AttributeType=S AttributeName=email,AttributeType=S AttributeName=status,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" \
  "[$(gsi documento-index documento),$(gsi email-index email),$(gsi status-index status)]"

cria_tabela "$TABLE_PRODUCTS" \
  "AttributeName=id,AttributeType=N AttributeName=codigo,AttributeType=S AttributeName=categoria,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" "[$(gsi codigo-index codigo),$(gsi categoria-index categoria)]"

cria_tabela "$TABLE_INVENTORY" "AttributeName=produtoId,AttributeType=N AttributeName=status,AttributeType=S" \
  "AttributeName=produtoId,KeyType=HASH" "[$(gsi status-index status)]"

cria_tabela "$TABLE_INVENTORY_MOVEMENTS" \
  "AttributeName=id,AttributeType=N AttributeName=produtoId,AttributeType=N AttributeName=data,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" "[$(gsi produto-data-index produtoId data)]" 0

cria_tabela "$TABLE_SALES" \
  "AttributeName=id,AttributeType=N AttributeName=clienteId,AttributeType=N AttributeName=status,AttributeType=S AttributeName=data,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" \
  "[$(gsi cliente-index clienteId),$(gsi status-index status),$(gsi data-index status data)]"

cria_tabela "$TABLE_SALE_ITEMS" "AttributeName=vendaId,AttributeType=N AttributeName=itemId,AttributeType=N" \
  "AttributeName=vendaId,KeyType=HASH AttributeName=itemId,KeyType=RANGE" "" 0

cria_tabela "$TABLE_NOTAS_FISCAIS" \
  "AttributeName=id,AttributeType=N AttributeName=status,AttributeType=S AttributeName=vendaId,AttributeType=N AttributeName=chaveAcesso,AttributeType=S AttributeName=tipo,AttributeType=S AttributeName=emitidaEm,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" \
  "[$(gsi status-index status),$(gsi venda-index vendaId),$(gsi chave-index chaveAcesso),$(gsi emitidaEm-index tipo emitidaEm)]"

cria_tabela "$TABLE_NOTAS_ITEMS" "AttributeName=notaId,AttributeType=N AttributeName=itemId,AttributeType=N" \
  "AttributeName=notaId,KeyType=HASH AttributeName=itemId,KeyType=RANGE" "" 0

cria_tabela "$TABLE_FISCAL_EVENTS" \
  "AttributeName=id,AttributeType=N AttributeName=notaId,AttributeType=N AttributeName=tipo,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" "[$(gsi nota-index notaId),$(gsi tipo-index tipo)]" 0

cria_tabela "$TABLE_ENTRY_NOTES" \
  "AttributeName=id,AttributeType=N AttributeName=fornecedorId,AttributeType=N AttributeName=dataEntrada,AttributeType=S AttributeName=chaveAcesso,AttributeType=S" \
  "AttributeName=id,KeyType=HASH" \
  "[$(gsi fornecedor-index fornecedorId),$(gsi data-index dataEntrada),$(gsi chave-index chaveAcesso)]"

cria_tabela "$TABLE_SUPPLIERS" "AttributeName=cnpj,AttributeType=S" "AttributeName=cnpj,KeyType=HASH" "" 0
