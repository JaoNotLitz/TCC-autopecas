# EPB-00 — Ambiente local (LocalStack + Docker Compose)

**Unidade entregue:** stack de infraestrutura em `infra/local/` (Docker Compose + scripts de init do LocalStack) e a biblioteca compartilhada `wc-commons`.
**Tipo:** infraestrutura · **Sprint sugerida:** 1

## Objetivo

Permitir que qualquer pessoa clone o repositório e suba **todo** o back com um comando, sem conta AWS. O LocalStack emula DynamoDB, SQS, SNS, S3, Secrets Manager, Lambda, API Gateway (REST), SES, EventBridge e CloudWatch Logs. Redis e o simulador da SEFAZ rodam em containers próprios.

## Escopo

- `docker-compose.yml` com LocalStack, Redis, `sefaz-mock` e um serviço por microsserviço.
- Criação idempotente dos recursos AWS da arquitetura (11 tabelas, filas, tópicos, buckets, secrets).
- Projeto Maven multi-módulo e a lib `wc-commons` (erro padrão, paginação, segurança, clientes AWS).
- Dados de seed e certificado A1 de teste.
- CI no GitHub Actions rodando os testes com Testcontainers.

## Fora do escopo

Deploy em AWS real (seção 15 da arquitetura), Terraform de produção.

## Critérios de aceite do épico

- `make up` (ou `docker compose up -d`) sobe a stack e `make status` mostra todos os containers `healthy` em até 3 minutos numa máquina com 8 GB de RAM.
- `awslocal dynamodb list-tables`, `sqs list-queues`, `sns list-topics` e `s3 ls` listam exatamente os recursos da tabela de recursos (HU-B00.2).
- `make down && make up` não duplica recursos nem perde o seed.
- Um serviço de exemplo usando `wc-commons` responde erro no formato da seção 5.0.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B00.1](HU-B00.1-docker-compose-base.md) | Docker Compose base com LocalStack e Redis | 3 |
| [HU-B00.2](HU-B00.2-provisionamento-recursos-aws.md) | Provisionar recursos AWS no LocalStack | 5 |
| [HU-B00.3](HU-B00.3-projeto-maven-wc-commons.md) | Projeto Maven multi-módulo e biblioteca `wc-commons` | 8 |
| [HU-B00.4](HU-B00.4-seed-certificado-teste.md) | Dados de seed e certificado A1 de teste | 3 |
| [HU-B00.5](HU-B00.5-template-microsservico.md) | Template de microsserviço (Dockerfile, health, perfil `local`) | 3 |
| [HU-B00.6](HU-B00.6-pipeline-ci.md) | Pipeline de CI com Testcontainers | 3 |
