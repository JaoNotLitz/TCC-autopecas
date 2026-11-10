# Stack local (LocalStack + Docker Compose)

Tudo roda na sua máquina, sem conta AWS. Pré-requisitos: Docker com Compose v2 e `make`
(no Windows, use o WSL2).

```bash
make up       # copia .env.example para .env (se faltar), builda e sobe tudo, esperando ficar healthy
make verify   # confere tabelas, filas, tópicos, buckets e secrets no LocalStack
make status   # containers e saúde
make logs     # logs de todos os containers
make down     # derruba (mantém o volume do Redis)
make reset    # derruba e apaga volumes
```

A primeira subida leva uns 2 minutos (download das imagens e build do Maven dentro do Docker).

## O que sobe

| Container | Porta | Função |
|---|---|---|
| `wc-localstack` | 4566 | DynamoDB, SQS, SNS, S3, Secrets Manager, SES, Lambda, API Gateway (Community 3.8.1) |
| `wc-redis` | 6379 | Sessões, contadores de login, cache e locks (senha em `REDIS_PASSWORD`) |
| `wc-lambda-build` | — | Compila o jar da Lambda `wc-jwt-authorizer` num volume e encerra |
| `wc-auth-service` | 8080 | Login, `/me`, logout |
| `wc-customer-service` | 8081 | Clientes (com 10 clientes de exemplo no perfil local) |
| `wc-product-service` | 8082 | Produtos, estoque e o listener da `inventory-updates-queue` (com 20 produtos de exemplo) |
| `wc-sales-service` | 8083 | Vendas e orçamentos; chama customer e product pelas rotas internas e publica reserva, `sale.created` e `sale_completed` |
| `wc-fiscal-service` | 8084 | Notas fiscais: cria a nota de cada venda finalizada, consulta, cancelamento, inutilização e devolução |

Os demais serviços entram aqui à medida que os épicos forem entregues.

## Recursos criados

Os scripts de `init/ready.d/` rodam quando o LocalStack fica pronto, em ordem numérica, e são
idempotentes (rodar de novo não duplica nada). Os nomes ficam em `init/resources.env`, que os
serviços também recebem como variáveis de ambiente.

- `10-dynamodb.sh`: as 13 tabelas da seção 3.4 da arquitetura, com os GSIs, on-demand e TTL em `ttl`.
- `20-sqs-sns.sh`: filas da seção 6 (DLQ `fiscal-dlq` com `maxReceiveCount` 3), tópicos da seção 7,
  o `sales-events-topic` da decisão A2 e as assinaturas (com filter policy na `email-queue`).
- `30-s3.sh`: os 4 buckets, com CORS para o front e versionamento em `wc-documents-bucket` e `wc-backups`.
- `40-secrets-ses.sh`: `wc-jwt-secret` (64 bytes aleatórios), `wc-redis-auth-token` e a identidade SES.
- Exemplo de XML de fornecedor para a tela Nota de Entrada: [`exemplos/nfe-fornecedor.xml`](exemplos/nfe-fornecedor.xml) (o item PF-001 existe no catálogo do seed, o XYZ-999 não).
- Lote do contador: o e-mail vai para `CONTADOR_EMAIL` (padrão `contador@wcautopecas.local`) e o ZIP fica em `wc-reports-bucket/lotes/{ano}/{mes}/`. O link pré-assinado aponta para `http://localhost:4566` e abre direto no navegador.
- E-mails: o notification-service envia pelo SES do LocalStack, que não entrega nada de verdade. Para ver o que saiu (destinatário, assunto e HTML): `curl -s http://localhost:4566/_aws/ses | jq '.messages[] | {to: .Destination.ToAddresses, assunto: .Subject}'`. Os destinatários dos alertas ficam em `ALERTA_ESTOQUE_DESTINATARIOS` e `OPS_DESTINATARIOS` no `.env`.
- Simulador da SEFAZ: container `wc-sefaz-mock` (WireMock, porta 8089). Os cenários (autoriza, rejeita, lote em processamento, fora do ar, timeout) saem do centavo do total da nota; tabela em [`sefaz-mock/README.md`](sefaz-mock/README.md).
- `45-certificado.sh`: certificado A1 **autoassinado de teste** (OpenSSL, 365 dias) em `wc-sefaz-certificate` como `{"pfx": base64, "senha": ...}`. Só serve para o simulador da SEFAZ; os testes geram a versão vencida com o `keytool`.
- `50-gateway.sh`: a Lambda `wc-jwt-authorizer` e a API REST `wc-autopecas-api` (id fixo `wcapi`, stage `local`),
  com as rotas da seção 3.2, respostas de erro do gateway no formato da seção 5.0 e o usage plan.

O container do LocalStack só fica `healthy` quando todos esses scripts terminam sem erro (`init/healthcheck.sh`).

## Gateway (endereço único para o front)

```
http://localhost:4566/restapis/wcapi/local/_user_request_/api
http://wcapi.execute-api.localhost.localstack.cloud:4566/local/api   (mesmo gateway, URL mais curta)
```

No front, `VITE_API_URL` recebe uma dessas URLs (já terminando em `/api`). O id `wcapi` é fixo, então a URL
não muda entre subidas. `infra/local/smoke-gateway.sh` confere login, `/me`, 401 sem token, CORS e o authorizer.

| Caminho | Serviço |
|---|---|
| `/api/auth/*` (login sem token) | auth-service :8080 |
| `/api/clientes`, `/api/clientes/*` | customer-service :8081 |
| `/api/produtos`, `/api/produtos/*` | product-service :8082 |
| `/api/vendas`, `/api/vendas/*` | sales-service :8083 |
| `/api/notas`, `/api/notas/devolucao`, `/api/notas/{id}`, `/api/notas/{id}/*`, `/api/inutilizacoes` | fiscal-service :8084 |
| `/api/notas/entrada`, `/api/notas/entrada/*` | entry-service :8085 |
| `/api/notas/{id}/xml`, `/api/notas/{id}/pdf`, `/api/notas/lote-contador` | document-service :8086 |
| `/api/notas/relatorio`, `/api/dashboard`, `/api/relatorios/*` | reports-service :8087 |

Os serviços que ainda não existem respondem erro de integração até o épico deles entrar.

## Usuários de teste

O auth-service cria, no perfil `local`, os usuários do seed (senhas no `.env`):

| E-mail | Senha padrão | Papel |
|---|---|---|
| `admin@wcautopecas.com.br` | `admin123` | ADMIN |
| `vendedor@wcautopecas.com.br` | `vendedor123` | VENDEDOR |

```bash
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@wcautopecas.com.br","senha":"admin123"}'
```

## Diferenças em relação à AWS real (decisão A1 do backlog)

| Na arquitetura | Aqui |
|---|---|
| ECS Fargate | Containers do Docker Compose |
| ALB | API Gateway REST (v1) do LocalStack com `HTTP_PROXY` para cada container (épico EPB-02) |
| ElastiCache | Container `redis` |
| DynamoDB Streams | Desligados (nenhum serviço consome ainda). `DYNAMO_STREAMS=1` no `.env` liga, ao custo de ~1 min a mais na subida |
| Lambda Authorizer no gateway | O LocalStack Community cria o authorizer mas **não o executa** (é recurso do Pro). A Lambda roda de verdade no LocalStack e é testada chamando-a direto (`smoke-gateway.sh`); quem barra token inválido localmente é o filtro JWT + sessão de cada serviço, que já existiria de qualquer forma |
| Respostas chunked | O provider do API Gateway que roda na Community perde o corpo de respostas `Transfer-Encoding: chunked`; o `wc-commons` responde com `Content-Length` (`ContentLengthFilter`) |
| Throttling (usage plan) | Criado, mas a Community não aplica |
| Persistência do LocalStack | A Community não persiste: a cada `make up` os recursos são recriados pelo init e o seed roda de novo. Por isso o `wc-jwt-secret` muda a cada subida e os tokens antigos deixam de valer |

A imagem do LocalStack está fixa em `3.8.1`: as versões mais novas exigem token de conta da LocalStack.
