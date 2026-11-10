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

# Venda finalizada vira nota "processando" no fiscal-service (sale.created pela sales-events-queue).
final=$(curl -s -X POST "$G/vendas" -H "$auth" -H 'Content-Type: application/json' -d "${venda/orcamento/finalizada}")
vid=$(sed -nE 's/^\{"id":([0-9]+),.*/\1/p' <<<"$final")
nota=""
for _ in $(seq 1 30); do
  nota=$(curl -s "$G/notas?vendaId=$vid" -H "$auth")
  grep -q '"status":"processando"' <<<"$nota" && break
  sleep 1
done
confere "venda finalizada gera nota" "$(grep -o '"tipo":"NFCE"' <<<"$nota")" '"tipo":"NFCE"'
# O job generate_xml assina o XML e grava a chave de acesso (EPB-08).
for _ in $(seq 1 30); do
  nota=$(curl -s "$G/notas?vendaId=$vid" -H "$auth")
  grep -qE '"chaveAcesso":"[0-9]{44}"' <<<"$nota" && break
  sleep 1
done
confere "nota ganha chave de acesso" "$(grep -cE '"chaveAcesso":"[0-9]{44}"' <<<"$nota")" 1
# A nota vai para o simulador da SEFAZ e volta autorizada com protocolo (EPB-09).
for _ in $(seq 1 30); do
  nota=$(curl -s "$G/notas?vendaId=$vid" -H "$auth")
  grep -q '"status":"autorizada"' <<<"$nota" && break
  sleep 1
done
confere "SEFAZ simulada autoriza a nota" "$(grep -o '"status":"autorizada"' <<<"$nota")" '"status":"autorizada"'
# Downloads do document-service (EPB-13): XML autorizado e DANFE gerado a partir dele.
nid=$(sed -nE 's/^\[\{"id":([0-9]+),.*/\1/p' <<<"$nota")
confere "GET /notas/{id}/xml devolve o nfeProc" "$(curl -s "$G/notas/$nid/xml" -H "$auth" | grep -o '<nfeProc' | head -1)" '<nfeProc'
confere "GET /notas/{id}/pdf devolve o DANFE" "$(curl -s "$G/notas/$nid/pdf" -H "$auth" | head -c 5)" '%PDF-'
# Cancelamento pela nota: evento 110111 na SEFAZ simulada, nota e venda canceladas (EPB-10).
confere "POST /notas/{id}/cancelamento" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$G/notas/$nid/cancelamento" \
  -H "$auth" -H 'Content-Type: application/json' -d '{"justificativa":"Cliente desistiu da compra no balcao"}')" 200
for _ in $(seq 1 30); do
  nota=$(curl -s "$G/notas/$nid" -H "$auth")
  grep -q '"status":"cancelada"' <<<"$nota" && break
  sleep 1
done
confere "SEFAZ simulada homologa o cancelamento" "$(grep -o '"status":"cancelada"' <<<"$nota")" '"status":"cancelada"'
confere "DANFE da cancelada continua disponível" "$(curl -s -o /dev/null -w '%{http_code}' "$G/notas/$nid/pdf" -H "$auth")" 200
confere "venda fica cancelada" "$(curl -s "$G/vendas/$vid" -H "$auth" | grep -o '"status":"cancelada"')" '"status":"cancelada"'

# Nota de entrada do fornecedor: PF-001 por código, soma estoque e vira nota ENTRADA (EPB-11).
dv_chave() { local b=$1 s=0 p=2 i; for ((i=${#b}-1; i>=0; i--)); do s=$((s + ${b:i:1} * p)); p=$((p == 9 ? 2 : p + 1)); done
  local r=$((s % 11)); echo $((r < 2 ? 0 : 11 - r)); }
nnf=$(printf '%09d' $(( $(date +%s) % 1000000000 )))
base="3526091122233300018155001${nnf}112345678"
chave="${base}$(dv_chave "$base")"
antes=$(curl -s "$G/produtos/1" -H "$auth" | sed -nE 's/.*"estoqueAtual":([0-9]+).*/\1/p')
entrada='{"fornecedor":{"cnpj":"11222333000181","razaoSocial":"Distribuidora Freios Brasil LTDA","ie":"244123456119","uf":"SP"},
"numero":"'"$((10#$nnf))"'","serie":"1","dataEmissao":"2026-09-20","dataEntrada":"2026-09-21","chaveAcesso":"'"$chave"'",
"naturezaOperacao":"Compra para comercializacao","itens":[{"produtoId":null,"codigo":"PF-001","descricao":"Pastilha",
"ncm":"87083010","cfop":"1102","quantidade":5,"valorUnitario":45.5}]}'
resp=$(curl -s -w ' %{http_code}' -X POST "$G/notas/entrada" -H "$auth" -H 'Content-Type: application/json' -d "$entrada")
confere "POST /notas/entrada registra" "${resp##* }" 201
confere "entrada vira nota ENTRADA" "$(grep -o '"tipo":"ENTRADA"' <<<"$resp")" '"tipo":"ENTRADA"'
depois=""
for _ in $(seq 1 20); do
  depois=$(curl -s "$G/produtos/1" -H "$auth" | sed -nE 's/.*"estoqueAtual":([0-9]+).*/\1/p')
  [ "$depois" = "$((antes + 5))" ] && break
  sleep 1
done
confere "entrada soma o estoque" "$depois" "$((antes + 5))"
previa=$(curl -s -w ' %{http_code}' -X POST "$G/notas/entrada/importar-xml" -H "$auth" \
  -F "arquivo=@$(dirname "$0")/exemplos/nfe-fornecedor.xml;type=text/xml")
confere "importar-xml devolve o formulário" "${previa##* }" 200
confere "importar-xml acha PF-001 no catálogo" "$(grep -o '"produtoId":[0-9]*,"codigo":"PF-001"' <<<"$previa" | head -1 | grep -c PF-001)" 1
confere "chave repetida responde 409" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$G/notas/entrada" -H "$auth" \
  -H 'Content-Type: application/json' -d "$entrada")" 409

# Lote do contador (EPB-13): ZIP no wc-reports-bucket e e-mail na email-queue; o segundo envio não tem o que mandar.
confere "POST /notas/lote-contador" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$G/notas/lote-contador" -H "$auth")" 204
confere "lote repetido no mês responde 409" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$G/notas/lote-contador" -H "$auth")" 409

# Relatório do Painel Fiscal (EPB-14): CSV com as mesmas notas de GET /notas no período.
hoje=$(TZ=America/Sao_Paulo date +%F)
csv=$(curl -s -D /tmp/relatorio.h "$G/notas/relatorio?status=cancelada&de=$hoje&ate=$hoje" -H "$auth")
confere "GET /notas/relatorio devolve CSV" "$(grep -ci '^content-type: text/csv' /tmp/relatorio.h)" 1
lista=$(curl -s "$G/notas?status=cancelada&de=$hoje&ate=$hoje" -H "$auth" | grep -o '"id":' | wc -l)
confere "listagem acha a cancelada de hoje" "$([ "$lista" -ge 1 ] && echo sim)" sim
confere "relatório tem as notas da listagem" "$(($(wc -l <<<"$csv") - 1))" "$lista"

# E-mails (EPB-15): o notification-service manda pelo SES local, que guarda tudo em /_aws/ses.
ses() { curl -s "http://localhost:4566/_aws/ses"; }
cpf() { local b=$(printf '%09d' $(( (RANDOM * 32768 + RANDOM) % 1000000000 ))) s i d1 d2
  s=0; for i in $(seq 0 8); do s=$((s + ${b:i:1} * (10 - i))); done; d1=$(( (s * 10) % 11 % 10 ))
  s=0; for i in $(seq 0 8); do s=$((s + ${b:i:1} * (11 - i))); done; s=$((s + d1 * 2)); d2=$(( (s * 10) % 11 % 10 ))
  echo "$b$d1$d2"; }
novo="smoke+$(date +%s)@email.com"
confere "POST /clientes com e-mail" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$G/clientes" -H "$auth" \
  -H 'Content-Type: application/json' -d "{\"tipo\":\"PF\",\"documento\":\"$(cpf)\",\"nome\":\"Cliente Smoke\",\"email\":\"$novo\"}")" 201
for _ in $(seq 1 30); do ses | grep -q "$novo" && break; sleep 1; done
confere "cliente novo recebe boas-vindas" "$(ses | grep -c "$novo")" 1
confere "contador recebe o lote" "$(ses | grep -o 'contador@wcautopecas.local' | head -1)" contador@wcautopecas.local

[ $falhas -eq 0 ] && echo "gateway ok" || { echo "$falhas falha(s)"; exit 1; }
