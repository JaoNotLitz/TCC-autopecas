#!/bin/bash
# API Gateway REST (v1) "wc-autopecas-api" com id fixo "wcapi" e o Lambda Authorizer (EPB-02).
# Substitui o ALB da arquitetura (decisão A1): cada rota faz HTTP_PROXY direto para o container.
# Recria a API a cada execução: o id é fixo, então a URL do front não muda.
source "$(dirname "$0")/00-common.sh"

if ! awslocal apigateway get-rest-apis >/dev/null 2>&1; then
  log "API Gateway desabilitado neste LocalStack; pulando"; exit 0
fi
JAR=/opt/wc/lambdas/wc-jwt-authorizer.jar
if [ ! -f "$JAR" ]; then
  log "jar do authorizer não encontrado em $JAR; pulando o gateway"; exit 0
fi

REGION=$AWS_DEFAULT_REGION
ACCOUNT=000000000000
API_ID=wcapi
STAGE=local
FUNCAO=wc-jwt-authorizer
FRONT_ORIGIN="${FRONT_ORIGIN:-http://localhost:5173}"

# --- Lambda --------------------------------------------------------------------------------
awslocal lambda delete-function --function-name "$FUNCAO" >/dev/null 2>&1 || true
awslocal lambda create-function --function-name "$FUNCAO" --runtime java21 --memory-size 512 --timeout 15 \
  --handler br.com.wcautopecas.authorizer.AuthorizerHandler::handleRequest \
  --role "arn:aws:iam::$ACCOUNT:role/wc-lambda-role" --zip-file "fileb://$JAR" \
  --environment "Variables={SECRET_JWT=$SECRET_JWT}" >/dev/null
awslocal lambda wait function-active-v2 --function-name "$FUNCAO"
LAMBDA_ARN="arn:aws:lambda:$REGION:$ACCOUNT:function:$FUNCAO"
log "lambda $FUNCAO ok"

# --- API -----------------------------------------------------------------------------------
awslocal apigateway delete-rest-api --rest-api-id "$API_ID" >/dev/null 2>&1 || true
awslocal apigateway create-rest-api --name wc-autopecas-api --tags "_custom_id_=$API_ID" \
  --binary-media-types "multipart/form-data" "application/pdf" "application/zip" "application/octet-stream" \
  >/dev/null
ROOT=$(awslocal apigateway get-resources --rest-api-id "$API_ID" --query "items[?path=='/'].id" --output text)

AUTH_ID=$(awslocal apigateway create-authorizer --rest-api-id "$API_ID" --name "$FUNCAO" --type REQUEST \
  --identity-source "method.request.header.Authorization" --authorizer-result-ttl-in-seconds 0 \
  --authorizer-uri "arn:aws:apigateway:$REGION:lambda:path/2015-03-31/functions/$LAMBDA_ARN/invocations" \
  --query id --output text)

declare -A RECURSOS=(["/"]=$ROOT)

# recurso <caminho completo>: cria o recurso (e os pais que faltarem) e guarda o id em RECURSOS.
# Roda no shell atual (sem $(...)) para o mapa RECURSOS não se perder num subshell.
recurso() {
  local caminho=$1
  [ -n "${RECURSOS[$caminho]:-}" ] && return
  local pai="${caminho%/*}"; [ -z "$pai" ] && pai="/"
  recurso "$pai"
  RECURSOS[$caminho]=$(awslocal apigateway create-resource --rest-api-id "$API_ID" \
    --parent-id "${RECURSOS[$pai]}" --path-part "${caminho##*/}" --query id --output text)
}

# metodo <id> <verbo> <auth: NONE|JWT> <uri> [parâmetros de caminho...]
metodo() {
  local id=$1 verbo=$2 auth=$3 uri=$4; shift 4
  local req_params="" int_params=""
  for p in "$@"; do
    req_params+="\"method.request.path.$p\":true,"
    int_params+="\"integration.request.path.$p\":\"method.request.path.$p\","
  done
  local args=(--rest-api-id "$API_ID" --resource-id "$id" --http-method "$verbo")
  if [ "$auth" = JWT ]; then
    awslocal apigateway put-method "${args[@]}" --authorization-type CUSTOM --authorizer-id "$AUTH_ID" \
      --request-parameters "{${req_params%,}}" >/dev/null
    # Repasse do contexto do authorizer e do correlationId (HU-B02.4); só para log nos serviços.
    int_params+="\"integration.request.header.X-Usuario-Id\":\"context.authorizer.usuarioId\","
    int_params+="\"integration.request.header.X-Papel\":\"context.authorizer.papel\","
  else
    awslocal apigateway put-method "${args[@]}" --authorization-type NONE \
      --request-parameters "{${req_params%,}}" >/dev/null
  fi
  awslocal apigateway put-integration "${args[@]}" --type HTTP_PROXY --integration-http-method "$verbo" \
    --uri "$uri" --request-parameters "{${int_params%,}}" >/dev/null
}

# rota <caminho> <serviço:porta> [auth]  → ANY com authorizer + OPTIONS sem authorizer (CORS)
rota() {
  local caminho=$1 destino=$2 auth=${3:-JWT}
  recurso "$caminho"
  local id=${RECURSOS[$caminho]}
  local params; params=$( (grep -oE '\{[a-z]+\+?\}' <<<"$caminho" || true) | tr -d '{}+' | tr '\n' ' ')
  local uri_path; uri_path=$(sed -E 's/\{([a-z]+)\+\}/{\1}/g' <<<"$caminho")
  local uri="http://$destino$uri_path"
  metodo "$id" ANY "$auth" "$uri" $params
  metodo "$id" OPTIONS NONE "$uri" $params
}

# Rotas da seção 3.2 / HU-B02.2. Os caminhos especiais de /api/notas são recursos próprios,
# porque o API Gateway REST escolhe o recurso mais específico.
rota "/api/auth/{proxy+}"                 auth-service:8080
recurso "/api/auth/login"
metodo "${RECURSOS[/api/auth/login]}" POST NONE "http://auth-service:8080/api/auth/login"
metodo "${RECURSOS[/api/auth/login]}" OPTIONS NONE "http://auth-service:8080/api/auth/login"

rota "/api/clientes"                      customer-service:8081
rota "/api/clientes/{proxy+}"             customer-service:8081
rota "/api/produtos"                      product-service:8082
rota "/api/produtos/{proxy+}"             product-service:8082
rota "/api/vendas"                        sales-service:8083
rota "/api/vendas/{proxy+}"               sales-service:8083

rota "/api/notas"                         fiscal-service:8084
rota "/api/notas/devolucao"               fiscal-service:8084
rota "/api/notas/{id}"                    fiscal-service:8084
rota "/api/notas/{id}/{proxy+}"           fiscal-service:8084
rota "/api/inutilizacoes"                 fiscal-service:8084
rota "/api/inutilizacoes/{proxy+}"        fiscal-service:8084

rota "/api/notas/entrada"                 entry-service:8085
rota "/api/notas/entrada/{proxy+}"        entry-service:8085

rota "/api/notas/{id}/xml"                document-service:8086
rota "/api/notas/{id}/pdf"                document-service:8086
rota "/api/notas/lote-contador"           document-service:8086

rota "/api/notas/relatorio"               reports-service:8087
rota "/api/dashboard"                     reports-service:8087
rota "/api/relatorios/{proxy+}"           reports-service:8087

# --- Respostas do próprio gateway no formato de erro da seção 5.0 --------------------------
cors_headers="{\"gatewayresponse.header.Access-Control-Allow-Origin\":\"'$FRONT_ORIGIN'\",\"gatewayresponse.header.Access-Control-Allow-Headers\":\"'Authorization,Content-Type,X-Correlation-Id'\"}"
resposta() { # resposta <tipo> <status> <erro> <mensagem>
  awslocal apigateway put-gateway-response --rest-api-id "$API_ID" --response-type "$1" --status-code "$2" \
    --response-parameters "$cors_headers" \
    --response-templates "{\"application/json\":\"{\\\"message\\\":\\\"$4\\\",\\\"status\\\":$2,\\\"erro\\\":\\\"$3\\\"}\"}" \
    >/dev/null
}
resposta UNAUTHORIZED 401 UNAUTHORIZED "Sessão expirada ou inválida. Faça login novamente."
resposta ACCESS_DENIED 403 FORBIDDEN "Você não tem permissão para esta operação"
resposta DEFAULT_5XX 502 BAD_GATEWAY "Serviço indisponível. Tente novamente."

awslocal apigateway create-deployment --rest-api-id "$API_ID" --stage-name "$STAGE" >/dev/null

# Throttling da seção 3.3 (registrado; o LocalStack Community não aplica).
for antigo in $(awslocal apigateway get-usage-plans --query "items[?name=='wc-plano-padrao'].id" --output text); do
  awslocal apigateway delete-usage-plan --usage-plan-id "$antigo" >/dev/null
done
PLANO=$(awslocal apigateway create-usage-plan --name wc-plano-padrao \
  --throttle burstLimit=100,rateLimit=1000 --api-stages "apiId=$API_ID,stage=$STAGE" --query id --output text)
log "usage plan $PLANO ok"
log "gateway pronto: http://localhost:4566/restapis/$API_ID/$STAGE/_user_request_/api"
