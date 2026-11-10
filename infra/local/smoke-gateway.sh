#!/bin/bash
# Smoke do gateway e do authorizer (EPB-02). Uso: infra/local/smoke-gateway.sh  (stack no ar)
set -uo pipefail
G="${GATEWAY_URL:-http://localhost:4566/restapis/wcapi/local/_user_request_/api}"
falhas=0
confere() { if [ "$2" = "$3" ]; then echo "ok    $1"; else echo "FALHA $1 (esperado $3, veio $2)"; falhas=$((falhas+1)); fi; }

login=$(curl -s -X POST "$G/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@wcautopecas.com.br","senha":"'"${SEED_ADMIN_SENHA:-admin123}"'"}')
token=$(sed -nE 's/.*"token":"([^"]+)".*/\1/p' <<<"$login")
confere "login pelo gateway devolve token" "$([ -n "$token" ] && echo sim)" sim
confere "GET /auth/me com token" "$(curl -s -o /dev/null -w '%{http_code}' "$G/auth/me" -H "Authorization: Bearer $token")" 200
confere "GET /auth/me sem token" "$(curl -s -o /dev/null -w '%{http_code}' "$G/auth/me")" 401
confere "preflight CORS sem token" "$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$G/auth/me" \
  -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: GET')" 200

# O LocalStack Community não executa authorizers no gateway; o authorizer é testado chamando a Lambda.
invoca() {
  docker exec wc-localstack sh -c "awslocal lambda invoke --function-name wc-jwt-authorizer \
    --payload '$1' /tmp/saida.json >/dev/null 2>&1; cat /tmp/saida.json"
}
arn='arn:aws:execute-api:sa-east-1:000000000000:wcapi/local/GET/api/clientes'
ok=$(invoca "{\"type\":\"REQUEST\",\"methodArn\":\"$arn\",\"headers\":{\"Authorization\":\"Bearer $token\"}}")
confere "authorizer libera token válido" "$(grep -o '"Effect":"Allow"' <<<"$ok")" '"Effect":"Allow"'
confere "authorizer repassa o papel" "$(grep -o '"papel":"ADMIN"' <<<"$ok")" '"papel":"ADMIN"'
ruim=$(invoca "{\"type\":\"REQUEST\",\"methodArn\":\"$arn\",\"headers\":{\"Authorization\":\"Bearer abc.def.ghi\"}}")
confere "authorizer recusa token inválido" "$(grep -o 'Unauthorized' <<<"$ruim" | head -1)" Unauthorized

# Venda pelo gateway: sales-service chama customer e product pela rede do Compose.
auth="Authorization: Bearer $token"
venda='{"tipoDocumento":"nfce","modalidade":"fisica","clienteId":null,"metodoPagamento":"pix","bandeira":null,
"destinoDinheiro":null,"desconto":0,"status":"orcamento","itens":[{"produtoId":1,"quantidade":1,"valorUnitario":89.9}]}'
criada=$(curl -s -w ' %{http_code}' -X POST "$G/vendas" -H "$auth" -H 'Content-Type: application/json' -d "$venda")
confere "POST /vendas cria orçamento" "${criada##* }" 201
id=$(sed -nE 's/^\{"id":([0-9]+),.*/\1/p' <<<"$criada")
confere "DELETE /vendas/{id} cancela" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$G/vendas/$id" -H "$auth")" 204
confere "estoque insuficiente responde 409" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$G/vendas" -H "$auth" \
  -H 'Content-Type: application/json' -d "${venda/\"quantidade\":1/\"quantidade\":99999}")" 409

[ $falhas -eq 0 ] && echo "gateway ok" || { echo "$falhas falha(s)"; exit 1; }
