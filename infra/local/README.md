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
| `wc-auth-service` | 8080 | Login, `/me`, logout |

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
| Persistência do LocalStack | A Community não persiste: a cada `make up` os recursos são recriados pelo init e o seed roda de novo. Por isso o `wc-jwt-secret` muda a cada subida e os tokens antigos deixam de valer |

A imagem do LocalStack está fixa em `3.8.1`: as versões mais novas exigem token de conta da LocalStack.
