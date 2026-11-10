# HU-B00.1 — Docker Compose base com LocalStack e Redis

**Épico:** [EPB-00](README.md) · **Unidade:** `infra/local` · **Estimativa:** 3 pts · **Depende de:** —

## História

Como **desenvolvedora do TCC**, quero **subir a infraestrutura com um único comando** para **desenvolver e testar sem conta AWS**.

## Requisitos funcionais

- RF01 — `infra/local/docker-compose.yml` sobe os containers `localstack` (imagem `localstack/localstack` com versão fixa), `redis` (7.x, com senha) e a rede `wc-net`.
- RF02 — O LocalStack expõe a porta `4566` e habilita os serviços `dynamodb,sqs,sns,s3,secretsmanager,lambda,apigateway,ses,events,logs,iam,sts`.
- RF03 — Volumes nomeados persistem os dados do LocalStack (`PERSISTENCE=1`) e do Redis entre reinícios.
- RF04 — `Makefile` na raiz com os alvos `up`, `down`, `reset` (apaga volumes), `logs`, `status` e `seed`.
- RF05 — `.env.example` com todas as variáveis (região `sa-east-1`, credenciais `test/test`, `AWS_ENDPOINT_URL=http://localstack:4566`, senha do Redis).
- RF06 — Healthcheck em cada container; os microsserviços usam `depends_on: condition: service_healthy`.

## Regras e requisitos não funcionais

- Substituições do LocalStack gratuito documentadas no `infra/local/README.md` (decisão A1 do backlog): ECS → containers do Compose, ALB → API Gateway REST com `HTTP_PROXY`, ElastiCache → container Redis.
- Nenhuma credencial real em arquivo versionado.
- Funcionar em Linux, macOS e Windows (WSL2).

## Critérios de aceite

```gherkin
Cenário: subir a stack do zero
  Dado uma máquina com Docker e o repositório clonado
  Quando executo "make up"
  Então os containers localstack e redis ficam "healthy"
  E "curl localhost:4566/_localstack/health" mostra os serviços habilitados como "available" ou "running"

Cenário: dados persistem entre reinícios
  Dado a stack no ar com um item gravado no DynamoDB
  Quando executo "make down" e depois "make up"
  Então o item continua lá

Cenário: reset limpa tudo
  Quando executo "make reset"
  Então os volumes são removidos e a próxima subida recria os recursos do zero
```

## Subtasks

- [ ] ST-B00.1.1 Escrever `docker-compose.yml` com LocalStack, Redis e rede
- [ ] ST-B00.1.2 Configurar healthchecks e volumes
- [ ] ST-B00.1.3 Criar `Makefile` com os alvos
- [ ] ST-B00.1.4 Criar `.env.example` e adicionar `.env` ao `.gitignore`
- [ ] ST-B00.1.5 Documentar substituições do LocalStack gratuito em `infra/local/README.md`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B00.1.1 | Smoke (script) | `make up` seguido de `make status` retorna 0 e todos `healthy` | subir do zero |
| TC-B00.1.2 | Smoke (script) | grava item, reinicia, lê item | persistência |
| TC-B00.1.3 | Smoke (script) | `make reset` e `awslocal dynamodb list-tables` antes do init retorna vazio | reset |
