# Arquitetura Backend - WC Autopeças
## Planejamento Técnico Detalhado - Microsserviços Spring Boot + AWS

> **Documento de Arquitetura v2.0**  
> Sistema de gestão fiscal para autopeças com integração SEFAZ  
> Baseado no TCC "Sistema Fiscal para Autopeças" e Mapeamento de Classes Frontend

---

## Índice

1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Stack Tecnológica](#2-stack-tecnológica)
3. [Infraestrutura AWS](#3-infraestrutura-aws)
4. [Microsserviços - Detalhamento](#4-microsserviços-detalhamento)
5. [APIs e Endpoints](#5-apis-e-endpoints)
6. [Filas SQS - Mensageria](#6-filas-sqs-mensageria)
7. [Tópicos SNS - Pub/Sub](#7-tópicos-sns-pubsub)
8. [Listeners e Consumers](#8-listeners-e-consumers)
9. [Banco de Dados DynamoDB](#9-banco-de-dados-dynamodb)
10. [Integração SEFAZ](#10-integração-sefaz)
11. [Fluxos de Negócio](#11-fluxos-de-negócio)
12. [Segurança](#12-segurança)
13. [Monitoramento](#13-monitoramento)
14. [LocalStack](#14-localstack)
15. [Deployment](#15-deployment)

---

## 1. Visão Geral da Arquitetura

### 1.1 Contexto do Sistema

O sistema WC Autopeças é uma plataforma completa de gestão para lojas de autopeças com foco em **conformidade fiscal brasileira**. O sistema gerencia todo o ciclo desde a venda até a emissão de documentos fiscais (NF-e e NFC-e) com integração direta à SEFAZ.

**Módulos Principais:**
- 🔐 **Autenticação e Autorização**
- 👥 **Gestão de Clientes** (PF/PJ)
- 📦 **Catálogo e Estoque**
- 💰 **Vendas e PDV**
- 📄 **Gestão Fiscal** (NF-e/NFC-e)
- 📥 **Entrada de Mercadorias**
- 📊 **Relatórios e Dashboard**
- 🔔 **Notificações**

### 1.2 Arquitetura de Alto Nível

```
┌─────────────────────────────────────────────────────────────────┐
│                    CAMADA DE FRONTEND                            │
│                     Angular 22 SPA                               │
│              (Executado no navegador do usuário)                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS (TLS 1.3)
                           │ REST/JSON
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AWS API GATEWAY (REST API)                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ • Cognito JWT Authorizer                                  │  │
│  │ • Rate Limiting: 1000 req/s                               │  │
│  │ • Request/Response Validation                             │  │
│  │ • CORS Configuration                                      │  │
│  │ • Custom Domain: api.wcautopecas.com.br                  │  │
│  └───────────────────────────────────────────────────────────┘  │
└────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────────┘
     │      │      │      │      │      │      │      │
     ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼
┌─────────────────────────────────────────────────────────────────┐
│          APPLICATION LOAD BALANCER (ALB)                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ Path-based Routing:                                       │  │
│  │ /api/auth/*       → auth-service:8080                     │  │
│  │ /api/clientes/*   → customer-service:8081                 │  │
│  │ /api/produtos/*   → product-service:8082                  │  │
│  │ /api/vendas/*     → sales-service:8083                    │  │
│  │ /api/notas/*      → fiscal-service:8084                   │  │
│  │ /api/entrada/*    → entry-service:8085                    │  │
│  │ /api/documentos/* → document-service:8086                 │  │
│  │ /api/dashboard/*  → reports-service:8087                  │  │
│  └───────────────────────────────────────────────────────────┘  │
└────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────────┘
     │      │      │      │      │      │      │      │
     ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼
┌─────────────────────────────────────────────────────────────────┐
│            MICROSSERVIÇOS SPRING BOOT (ECS FARGATE)              │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Auth Service  │  │Customer Svc  │  │Product Svc   │          │
│  │Port: 8080    │  │Port: 8081    │  │Port: 8082    │          │
│  │Tasks: 2      │  │Tasks: 2      │  │Tasks: 2-10   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Sales Service │  │Fiscal Svc    │  │Entry Service │          │
│  │Port: 8083    │  │Port: 8084    │  │Port: 8085    │          │
│  │Tasks: 2-10   │  │Tasks: 3-15   │  │Tasks: 2      │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │Document Svc  │  │Reports Svc   │  │Notification  │          │
│  │Port: 8086    │  │Port: 8087    │  │Port: 8088    │          │
│  │Tasks: 2-5    │  │Tasks: 2      │  │Tasks: 2      │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CAMADA DE DADOS E EVENTOS                     │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    DynamoDB (11 Tabelas)                    │ │
│  │  • users • customers • products • inventory                 │ │
│  │  • sales • sale_items • notas_fiscais • notas_items        │ │
│  │  • fiscal_events • entry_notes • inventory_movements       │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              ElastiCache Redis (Cache Layer)                │ │
│  │  • Session Store (JWT tokens)                               │ │
│  │  • Cache de queries frequentes                              │ │
│  │  • Rate limiting counters                                   │ │
│  │  • Distributed locks                                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Amazon SQS (8 Filas)                      │ │
│  │  • customer-events-queue                                    │ │
│  │  • inventory-updates-queue                                  │ │
│  │  • sales-events-queue                                       │ │
│  │  • fiscal-jobs-queue                                        │ │
│  │  • sefaz-queue                                              │ │
│  │  • fiscal-events-queue                                      │ │
│  │  • entry-events-queue                                       │ │
│  │  • email-queue                                              │ │
│  │  • fiscal-dlq (Dead Letter Queue)                           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Amazon SNS (3 Tópicos)                    │ │
│  │  • low-stock-alerts-topic                                   │ │
│  │  • sales-notifications-topic                                │ │
│  │  • system-notifications-topic                               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Amazon S3 (4 Buckets)                     │ │
│  │  • wc-xml-storage (XMLs de entrada)                         │ │
│  │  • wc-documents-bucket (XMLs e PDFs de notas)               │ │
│  │  • wc-reports-bucket (Relatórios CSV/Excel)                 │ │
│  │  • wc-backups (Backups do sistema)                          │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                   INTEGRAÇÕES EXTERNAS                           │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │    SEFAZ     │  │  Amazon SES  │  │ CloudWatch   │          │
│  │ WebServices  │  │    Email     │  │  Logs/Metrics│          │
│  │ SOAP/REST    │  │  Templates   │  │   Alarms     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 Princípios Arquiteturais

#### **Event-Driven Architecture (EDA)**
- Comunicação assíncrona entre microsserviços via SQS/SNS
- Desacoplamento total entre produtores e consumidores
- Resiliência: falha em um serviço não impacta outros
- Reprocessamento: DLQ para mensagens com erro

#### **Microservices Pattern**
- Cada serviço é independente e deployável separadamente
- Banco de dados por serviço (database-per-service pattern)
- API Gateway como ponto único de entrada
- Service discovery via DNS (AWS Cloud Map)

#### **Domain-Driven Design (DDD)**
- Bounded Contexts bem definidos
- Agregados e Entidades de domínio
- Linguagem ubíqua entre time técnico e negócio
- Separação clara entre camadas

#### **Cloud-Native**
- Stateless services (estado em DynamoDB/Redis)
- Configuração externalizada (AWS Secrets Manager)
- Auto-scaling horizontal (ECS + ALB)
- Health checks e graceful shutdown

---

## 2. Stack Tecnológica

### 2.1 Backend

| Tecnologia | Versão | Função |
|-----------|--------|---------|
| **Java** | 21 (LTS) | Linguagem principal |
| **Spring Boot** | 3.3.x | Framework de aplicação |
| **Spring Security** | 6.x | Autenticação e autorização |
| **Spring Cloud AWS** | 3.1.x | Integração com serviços AWS |
| **Spring Data** | 3.3.x | Abstração de acesso a dados |
| **Maven** | 3.9.x | Build tool |

**Bibliotecas Principais:**
- **AWS SDK for Java 2.x** - Acesso programático aos serviços AWS
- **DynamoDB Enhanced Client** - ORM para DynamoDB
- **Lettuce** - Cliente Redis (via Spring Data Redis)
- **Apache CXF** - Cliente SOAP para SEFAZ
- **iText 7** - Geração de PDF (DANFE)
- **XStream** - Serialização/deserialização XML
- **Bouncy Castle** - Assinatura digital de XML
- **Micrometer** - Métricas e observabilidade
- **SLF4J + Logback** - Logging estruturado
- **MapStruct** - Mapeamento de objetos
- **Lombok** - Redução de boilerplate

### 2.2 Infraestrutura

| Serviço AWS | Função |
|------------|---------|
| **ECS Fargate** | Orquestração de containers serverless |
| **Application Load Balancer** | Roteamento de tráfego HTTP/HTTPS |
| **API Gateway** | Gateway HTTP com autenticação |
| **DynamoDB** | Banco NoSQL principal |
| **ElastiCache Redis** | Cache distribuído e session store |
| **SQS** | Filas de mensagens |
| **SNS** | Pub/Sub para notificações |
| **S3** | Storage de objetos (XML, PDF, relatórios) |
| **Secrets Manager** | Gestão de credenciais e certificados |
| **CloudWatch** | Logs, métricas e alarmes |
| **X-Ray** | Tracing distribuído |
| **VPC** | Rede privada virtual |
| **ECR** | Registry de imagens Docker |

### 2.3 Desenvolvimento

| Ferramenta | Função |
|-----------|---------|
| **LocalStack** | Emulação de serviços AWS localmente |
| **Docker** | Containerização |
| **Docker Compose** | Orquestração local |
| **GitHub Actions** | CI/CD pipeline |
| **Postman/Insomnia** | Testes de API |
| **AWS CLI** | Gestão de recursos AWS |

---

## 3. Infraestrutura AWS

### 3.1 Amazon ECS (Elastic Container Service)

**Configuração:**
- **Launch Type:** Fargate (serverless, sem gestão de EC2)
- **Cluster:** wc-autopecas-cluster
- **Service Discovery:** AWS Cloud Map (DNS interno)
- **Networking:** VPC com subnets públicas e privadas

**Task Definitions por Serviço:**

| Serviço | vCPU | Memória | Min Tasks | Max Tasks | Health Check |
|---------|------|---------|-----------|-----------|--------------|
| auth-service | 0.5 | 1GB | 2 | 5 | /actuator/health |
| customer-service | 0.5 | 1GB | 2 | 10 | /actuator/health |
| product-service | 1.0 | 2GB | 2 | 10 | /actuator/health |
| sales-service | 1.0 | 2GB | 2 | 10 | /actuator/health |
| fiscal-service | 1.0 | 2GB | 3 | 15 | /actuator/health |
| entry-service | 0.5 | 1GB | 2 | 5 | /actuator/health |
| document-service | 1.0 | 2GB | 2 | 5 | /actuator/health |
| reports-service | 0.5 | 1GB | 2 | 5 | /actuator/health |
| notification-service | 0.5 | 1GB | 2 | 5 | /actuator/health |

**Auto Scaling:**
- **Métrica:** CPU Utilization
- **Target:** 70%
- **Scale Out:** Adiciona 1 task quando CPU > 70% por 3 minutos
- **Scale In:** Remove 1 task quando CPU < 40% por 5 minutos
- **Cooldown:** 300 segundos

### 3.2 Application Load Balancer (ALB)

**Função:** Distribuir tráfego HTTP/HTTPS entre tasks ECS

**Configuração:**
- **Tipo:** Application Load Balancer
- **Scheme:** Internet-facing
- **IP:** IPv4
- **Subnets:** Mínimo 2 AZs para alta disponibilidade
- **Security Group:** Inbound 443 (HTTPS), 80 (HTTP)

**Listeners:**
- **443 (HTTPS):**
  - Certificado SSL/TLS via ACM (AWS Certificate Manager)
  - Forward para Target Groups baseado em path
  
- **80 (HTTP):**
  - Redirect permanente para HTTPS (301)

**Target Groups:**
- Um target group por serviço
- Health check interval: 30s
- Health check timeout: 5s
- Healthy threshold: 2
- Unhealthy threshold: 3
- Deregistration delay: 30s (graceful shutdown)

**Path-based Routing Rules:**
```
/api/auth/*       → tg-auth-service
/api/clientes/*   → tg-customer-service
/api/produtos/*   → tg-product-service
/api/vendas/*     → tg-sales-service
/api/notas/*      → tg-fiscal-service
/api/entrada/*    → tg-entry-service
/api/documentos/* → tg-document-service
/api/dashboard/*  → tg-reports-service
```

### 3.3 API Gateway

**Função:** Camada adicional de segurança, rate limiting e validação

**Tipo:** HTTP API (REST API alternativo)

**Features:**
- **Authorizer:** JWT via Cognito User Pools
- **Rate Limiting:** 1000 req/s por conta, 100 req/s por IP
- **CORS:** Configurado para domínio do frontend
- **Request Validation:** Schema validation para payloads
- **Custom Domain:** api.wcautopecas.com.br

**Integration:**
- Type: HTTP_PROXY
- URI: http://alb-dns-name/{proxy}
- Timeout: 30s

**Authorizer Configuration:**
```yaml
Type: JWT
Identity Source: $request.header.Authorization
Issuer: https://cognito-idp.us-east-1.amazonaws.com/{userPoolId}
Audience: {clientId}
Token validation: Automatic
```

### 3.4 Amazon DynamoDB

**Tabelas e Função:**

| Tabela | Partition Key | Sort Key | GSI | Streams | Função |
|--------|--------------|----------|-----|---------|---------|
| **users** | id | - | email-index | Sim | Dados de usuários do sistema |
| **customers** | id | - | documento-index, email-index, status-index | Sim | Clientes PF/PJ |
| **products** | id | - | codigo-index, categoria-index | Sim | Catálogo de produtos |
| **inventory** | produtoId | - | status-index | Sim | Estoque atual e status |
| **inventory_movements** | id | - | produto-data-index | Não | Histórico de movimentações |
| **sales** | id | - | cliente-index, status-index, data-index | Sim | Vendas e orçamentos |
| **sale_items** | vendaId | itemId | - | Não | Itens de cada venda |
| **notas_fiscais** | id | - | status-index, venda-index, chave-index | Sim | Notas fiscais emitidas |
| **notas_items** | notaId | itemId | - | Não | Itens de cada nota |
| **fiscal_events** | id | - | nota-index, tipo-index | Não | Cancelamentos, inutilizações |
| **entry_notes** | id | - | fornecedor-index, data-index | Sim | Notas de entrada |

**Configuração Global:**
- **Capacity Mode:** On-Demand (auto-scaling)
- **Encryption:** AWS Managed Keys (KMS)
- **Backup:** Point-in-time recovery habilitado
- **Streams:** NEW_AND_OLD_IMAGES (para CDC)
- **TTL:** Campo `ttl` para expiração automática

### 3.5 ElastiCache Redis

**Cluster Configuration:**
- **Engine:** Redis 7.0
- **Node Type:** 
  - Dev: cache.t3.micro (1 vCPU, 512MB)
  - Prod: cache.m5.large (2 vCPUs, 6.4GB)
- **Replication:** Multi-AZ com failover automático (prod)
- **Encryption:** At-rest e in-transit habilitado
- **Auth:** Token de autenticação obrigatório

**Uso:**
1. **Session Store:** Tokens JWT ativos (TTL: 8h)
2. **Cache de Queries:** Produtos, clientes frequentes (TTL: 1h)
3. **Cache de Dashboard:** Estatísticas agregadas (TTL: 5min)
4. **Rate Limiting:** Contadores por IP/usuário
5. **Distributed Locks:** Para operações idempotentes

**Key Patterns:**
```
session:{userId}:{tokenHash}              → Session data
product:{id}                              → Product details
customer:{id}                             → Customer details
dashboard:stats                           → Dashboard aggregations
ratelimit:{ip}:{endpoint}                 → Rate limit counter
lock:{resource}                           → Distributed lock
```

### 3.6 Amazon S3

**Buckets e Estrutura:**

#### **1. wc-xml-storage**
- **Função:** Armazenar XMLs de notas de fornecedores
- **Estrutura:** `/fornecedor/{cnpj}/{ano}/{mes}/{arquivo}.xml`
- **Lifecycle:**
  - 90 dias → Glacier (cold storage)
  - 365 dias → Delete permanente
- **Versioning:** Disabled
- **Encryption:** SSE-S3 (AES-256)

#### **2. wc-documents-bucket**
- **Função:** XMLs e PDFs de notas fiscais emitidas
- **Estrutura:** `/{tipo}/{ano}/{mes}/{chaveAcesso}.{xml|pdf}`
- **Lifecycle:** Permanente (compliance fiscal)
- **Versioning:** Enabled
- **Encryption:** SSE-S3
- **Access:** Presigned URLs (expire: 1h)

#### **3. wc-reports-bucket**
- **Função:** Relatórios gerados pelo sistema
- **Estrutura:** `/relatorios/{ano}/{mes}/{timestamp}_{tipo}.{csv|xlsx|zip}`
- **Lifecycle:** 365 dias → Delete
- **Versioning:** Disabled
- **Encryption:** SSE-S3

#### **4. wc-backups**
- **Função:** Backups automáticos do sistema
- **Estrutura:** `/backups/{service}/{date}/`
- **Lifecycle:** 
  - 30 dias → Glacier
  - Retenção: Permanente
- **Versioning:** Enabled
- **Encryption:** SSE-KMS (customer managed key)

### 3.7 AWS Secrets Manager

**Secrets Armazenados:**

| Secret Name | Conteúdo | Rotation | Uso |
|-------------|----------|----------|-----|
| **wc-sefaz-certificate** | Certificado A1 + chave privada | Manual (anual) | Assinatura digital NF-e |
| **wc-database-credentials** | Access keys DynamoDB | 90 dias | Acesso programático |
| **wc-redis-auth-token** | Token ElastiCache | 90 dias | Autenticação Redis |
| **wc-jwt-secret** | Chave para JWT | 180 dias | Assinatura de tokens |

**Acesso:**
- Serviços ECS têm IAM roles com permissão GetSecretValue
- Secrets são injetados como variáveis de ambiente nos containers

---

## 4. Microsserviços - Detalhamento

### 4.1 Auth Service

**Porta:** 8080  
**Bounded Context:** Identity & Access Management

#### **Responsabilidades:**
1. Autenticar usuários via email/senha
2. Gerar tokens JWT (access + refresh)
3. Validar tokens em requisições
4. Gerenciar sessões ativas no Redis
5. Controlar tentativas de login e bloqueio
6. Logout (invalidação de token)

#### **Dependências:**
- **DynamoDB:** Tabela `users`
- **Redis:** Session store
- **AWS Cognito:** Geração e validação de JWT
- **Secrets Manager:** Chave JWT

#### **Integrações:**
- **Inbound:** API Gateway (HTTPS)
- **Outbound:** Nenhuma (serviço leaf)

#### **Eventos Publicados:**
- Nenhum (serviço stateless)

#### **Eventos Consumidos:**
- Nenhum

---

### 4.2 Customer Service

**Porta:** 8081  
**Bounded Context:** Customer Management

#### **Responsabilidades:**
1. CRUD completo de clientes (PF e PJ)
2. Validar CPF/CNPJ
3. Garantir unicidade de documento e email
4. Busca de clientes (por documento, email, nome)
5. Soft delete (desativação) de clientes
6. Publicar eventos de mudança de cliente

#### **Dependências:**
- **DynamoDB:** Tabela `customers`
- **SQS:** Fila `customer-events-queue` (producer)

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:** 
  - SQS `customer-events-queue`

#### **Eventos Publicados:**
```json
{
  "eventType": "customer.created | customer.updated | customer.deactivated",
  "customerId": "uuid",
  "timestamp": "ISO8601",
  "data": {
    "id": "uuid",
    "nome": "string",
    "email": "string",
    "documento": "string"
  }
}
```

**Destino:** `customer-events-queue`  
**Consumidor:** Notification Service (envio de email de boas-vindas)

#### **Eventos Consumidos:**
- **Fila:** `sales-events-queue`
- **Tipo:** `sale.completed`
- **Ação:** Atualizar campos `totalCompras`, `valorTotalCompras`, `ultimaCompra`

---

### 4.3 Product Service

**Porta:** 8082  
**Bounded Context:** Catalog & Inventory Management

#### **Responsabilidades:**
1. CRUD de produtos
2. Validar NCM, códigos e dados fiscais
3. Publicar eventos de mudança no estoque
4. Fornecer estatísticas de estoque
5. Consultar histórico de movimentações

#### **Dependências:**
- **DynamoDB:** Tabelas `products`, `inventory`, `inventory_movements`
- **SQS:** Fila `inventory-updates-queue` (producer)
- **SNS:** Tópico `low-stock-alerts-topic` (via listener)
- **Redis:** Cache de produtos

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:**
  - SQS `inventory-updates-queue`

#### **Eventos Publicados:**
```json
{
  "type": "reserve | release | decrease | increase | adjust",
  "produtoId": "uuid",
  "quantidade": "number",
  "referenciaId": "uuid (vendaId ou entryId)",
  "timestamp": "ISO8601"
}
```

**Destino:** `inventory-updates-queue`  
**Processamento:** Assíncrono pelo próprio Product Service (listener interno)

#### **Eventos Consumidos:**
- **Fila:** `inventory-updates-queue`
- **Listener:** `InventoryUpdateListener`
- **Ação:**
  1. Atualizar tabela `inventory` (estoqueAtual, reservado, disponivel, status)
  2. Registrar em `inventory_movements`
  3. Calcular novo status (ok, atencao, baixo, critico)
  4. Se crítico: Publicar em `low-stock-alerts-topic`

**Regras de Status:**
```
critico  → estoqueAtual <= estoqueMinimo × 0.4
baixo    → estoqueAtual <  estoqueMinimo × 0.7
atencao  → estoqueAtual <  estoqueMinimo
ok       → estoqueAtual >= estoqueMinimo
```

---

### 4.4 Sales Service

**Porta:** 8083  
**Bounded Context:** Sales Management

#### **Responsabilidades:**
1. Criar vendas (status: orcamento ou finalizada)
2. Validar disponibilidade de estoque antes de finalizar
3. Calcular valores (subtotal, desconto, total)
4. Gerenciar itens da venda
5. Publicar eventos para reserva de estoque
6. Disparar emissão fiscal para vendas finalizadas

#### **Dependências:**
- **DynamoDB:** Tabelas `sales`, `sale_items`
- **SQS:** Filas `inventory-updates-queue` (producer), `sales-events-queue` (producer)

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:**
  - SQS `inventory-updates-queue` (reserva de estoque)
  - SQS `sales-events-queue` (disparo de emissão fiscal)

#### **Eventos Publicados:**

**1. Para Inventory (Reserva):**
```json
{
  "type": "reserve",
  "produtoId": "uuid",
  "quantidade": 5,
  "vendaId": "uuid",
  "timestamp": "ISO8601"
}
```
**Destino:** `inventory-updates-queue`

**2. Para Fiscal (Emissão):**
```json
{
  "eventType": "sale.created",
  "saleId": "uuid",
  "tipoDocumento": "nfe | nfce",
  "timestamp": "ISO8601"
}
```
**Destino:** `sales-events-queue`  
**Consumidor:** Fiscal Service

#### **Eventos Consumidos:**
- Nenhum (serviço produtor apenas)

---

### 4.5 Fiscal Service

**Porta:** 8084  
**Bounded Context:** Tax & Fiscal Management

#### **Responsabilidades:**
1. Receber solicitações de emissão de NF-e/NFC-e
2. Criar registro de nota fiscal (status: processando)
3. Enfileirar job de processamento fiscal
4. Fornecer status de notas
5. Gerenciar cancelamentos, inutilizações e devoluções
6. Armazenar XMLs e PDFs no S3

#### **Dependências:**
- **DynamoDB:** Tabelas `notas_fiscais`, `notas_items`, `fiscal_events`
- **S3:** Bucket `wc-documents-bucket`
- **SQS:** Filas `fiscal-jobs-queue` (producer), `sefaz-queue` (producer)

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:**
  - SQS `fiscal-jobs-queue`
  - SQS `sefaz-queue`
  - S3 `wc-documents-bucket`

#### **Sub-processos Internos:**

##### **4.5.1 Fiscal Processor (Listener)**
- **Consome:** `fiscal-jobs-queue`
- **Função:** Gerar XML da nota, validar, assinar digitalmente
- **Ações:**
  1. Buscar dados da nota e itens no DynamoDB
  2. Gerar XML NF-e/NFC-e (layout 4.00)
  3. Assinar digitalmente com certificado A1
  4. Validar contra schema XSD
  5. Salvar XML no S3
  6. Publicar na `sefaz-queue`

##### **4.5.2 SEFAZ Integration (Listener)**
- **Consome:** `sefaz-queue`
- **Função:** Enviar XML para webservices SEFAZ
- **Ações:**
  1. Buscar XML no S3
  2. Enviar para SEFAZ via SOAP
  3. Consultar recibo (se assíncrono)
  4. Atualizar status da nota (autorizada/rejeitada)
  5. Armazenar protocolo e chave de acesso
  6. Se erro: Mover para `fiscal-dlq` após 3 tentativas

**Retry Strategy:**
- 1ª tentativa: Imediata
- 2ª tentativa: 1 minuto depois
- 3ª tentativa: 5 minutos depois
- Após 3 falhas: Mover para DLQ

#### **Eventos Publicados:**

**Para Document Service:**
```json
{
  "eventType": "nota.autorizada",
  "notaId": "uuid",
  "chaveAcesso": "44 dígitos",
  "timestamp": "ISO8601"
}
```
**Destino:** Interno (dispara geração de PDF)

#### **Eventos Consumidos:**

**Fila:** `sales-events-queue`  
**Tipo:** `sale.created`  
**Ação:** Criar nota fiscal e iniciar processo de emissão

---

### 4.6 Entry Service

**Porta:** 8085  
**Bounded Context:** Inbound Logistics

#### **Responsabilidades:**
1. Registrar notas fiscais de entrada manualmente
2. Fazer upload de XMLs de fornecedores
3. Importar e parsear XMLs de entrada
4. Validar dados do XML
5. Atualizar estoque com entrada de mercadorias

#### **Dependências:**
- **DynamoDB:** Tabelas `entry_notes`
- **S3:** Bucket `wc-xml-storage`
- **SQS:** Fila `entry-events-queue` (producer), `inventory-updates-queue` (producer)

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:**
  - S3 `wc-xml-storage`
  - SQS `entry-events-queue`
  - SQS `inventory-updates-queue`

#### **Fluxo de Importação XML:**
1. Frontend faz upload do XML (multipart/form-data)
2. Entry Service salva no S3
3. Publica evento na `entry-events-queue`
4. Listener consome e processa:
   - Parse do XML
   - Extração de fornecedor e itens
   - Validação de schema
   - Criação de registro em `entry_notes`
5. Publica eventos de `increase` na `inventory-updates-queue` para cada item

#### **Eventos Publicados:**

**1. Para processamento interno:**
```json
{
  "eventType": "entry.uploaded",
  "entryId": "uuid",
  "s3Key": "fornecedor/12345678901234/2024/09/nota.xml",
  "timestamp": "ISO8601"
}
```
**Destino:** `entry-events-queue`

**2. Para atualização de estoque:**
```json
{
  "type": "increase",
  "produtoId": "uuid",
  "quantidade": 100,
  "entryId": "uuid",
  "timestamp": "ISO8601"
}
```
**Destino:** `inventory-updates-queue`

#### **Eventos Consumidos:**

**Fila:** `entry-events-queue`  
**Listener:** `XmlProcessorListener`  
**Ação:** Processar XML e atualizar estoque

---

### 4.7 Document Service

**Porta:** 8086  
**Bounded Context:** Document Management

#### **Responsabilidades:**
1. Gerar PDF (DANFE) de notas autorizadas
2. Fornecer download de XML/PDF
3. Gerar presigned URLs do S3
4. Criar ZIPs de lotes de notas para contador

#### **Dependências:**
- **S3:** Bucket `wc-documents-bucket`
- **DynamoDB:** Tabela `notas_fiscais` (leitura)

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:**
  - S3 `wc-documents-bucket`

#### **Listener Interno:**
- **Trigger:** Nota autorizada (via Fiscal Service)
- **Ação:** Gerar PDF (DANFE) e salvar no S3

**Tecnologias:**
- **iText 7:** Geração de PDF
- **QR Code:** Para NFC-e (biblioteca ZXing)

---

### 4.8 Reports Service

**Porta:** 8087  
**Bounded Context:** Reporting & Analytics

#### **Responsabilidades:**
1. Fornecer dados do dashboard
2. Gerar relatórios CSV/Excel
3. Agregar estatísticas de vendas, estoque e fiscal
4. Cache de estatísticas (Redis)

#### **Dependências:**
- **DynamoDB:** Leitura de múltiplas tabelas
- **Redis:** Cache de agregações
- **S3:** Bucket `wc-reports-bucket`

#### **Integrações:**
- **Inbound:** API Gateway via ALB
- **Outbound:**
  - DynamoDB (queries)
  - Redis (cache)
  - S3 (reports)

**Dashboard Aggregations (cached 5min):**
```json
{
  "vendas": {
    "hoje": 15,
    "valorHoje": 12500.00,
    "mes": 450,
    "valorMes": 250000.00
  },
  "estoque": {
    "total": 847,
    "baixo": 23,
    "critico": 8,
    "valorTotal": 145890.00
  },
  "fiscal": {
    "emitidasHoje": 12,
    "autorizadas": 10,
    "processando": 2,
    "valorHoje": 15000.00
  }
}
```

---

### 4.9 Notification Service

**Porta:** 8088  
**Bounded Context:** Notifications & Alerts

#### **Responsabilidades:**
1. Enviar emails via SES
2. Consumir eventos de outros serviços
3. Gerenciar templates de email
4. Enviar alertas para operação

#### **Dependências:**
- **SQS:** Fila `email-queue` (consumer)
- **SNS:** Múltiplos tópicos (subscriber)
- **SES:** Amazon Simple Email Service

#### **Integrações:**
- **Inbound:** Nenhuma (API não exposta)
- **Outbound:**
  - SES (envio de emails)

#### **Eventos Consumidos:**

**1. Email Queue:**
```json
{
  "to": "email@example.com",
  "subject": "string",
  "template": "welcome | sale-confirmation | low-stock-alert",
  "data": {}
}
```

**2. SNS Topics (via subscription):**
- `low-stock-alerts-topic`
- `sales-notifications-topic`
- `system-notifications-topic`

---

Continuo com as próximas seções? Quer ver:
- 5. APIs e Endpoints (detalhamento completo de cada endpoint)
- 6. Filas SQS (função de cada fila)
- 7. Tópicos SNS (subscribers e uso)
- 8. Listeners e Consumers (cada listener e sua função)

Qual seção quer ver agora? 🎯



## 5. APIs e Endpoints

### 5.1 Auth Service (Port 8080)

**Base Path:** `/api/auth`

#### **POST /api/auth/login**
**Função:** Autenticar usuário e gerar tokens JWT

**Request:**
```json
{
  "email": "admin@wcautopecas.com.br",
  "senha": "senha123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": {
    "id": "uuid",
    "nome": "Administrador",
    "email": "admin@wcautopecas.com.br",
    "papel": "ADMIN"
  },
  "expiresIn": 28800
}
```

**Validações:**
- Email válido e obrigatório
- Senha obrigatória
- Usuário deve estar ativo
- Verificar bloqueio temporário (após 5 tentativas falhas)

**Efeitos Colaterais:**
- Incrementa contador de tentativas se senha incorreta
- Bloqueia usuário por 15min após 5 tentativas
- Reseta contador se login bem-sucedido
- Cria sessão no Redis (TTL: 8h)
- Atualiza campo `ultimoLogin`

**Erros:**
- `401 Unauthorized`: Credenciais inválidas
- `423 Locked`: Usuário bloqueado temporariamente
- `400 Bad Request`: Dados de entrada inválidos

---

#### **POST /api/auth/logout**
**Função:** Invalidar token e encerrar sessão

**Headers:**
```
Authorization: Bearer {token}
```

**Response (204 No Content)**

**Efeitos Colaterais:**
- Remove sessão do Redis
- Adiciona token à blacklist (opcional)

**Erros:**
- `401 Unauthorized`: Token inválido ou expirado

---

#### **GET /api/auth/me**
**Função:** Obter dados do usuário autenticado

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "nome": "Administrador",
  "email": "admin@wcautopecas.com.br",
  "papel": "ADMIN"
}
```

**Validações:**
- Token deve ser válido
- Sessão deve existir no Redis
- Usuário deve estar ativo

**Erros:**
- `401 Unauthorized`: Token inválido ou sessão expirada

---

#### **POST /api/auth/refresh**
**Função:** Renovar access token usando refresh token

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": {...},
  "expiresIn": 28800
}
```

**Validações:**
- Refresh token deve ser válido e não expirado
- Usuário deve estar ativo

**Efeitos Colaterais:**
- Gera novo access token
- Mantém refresh token (ou gera novo)
- Cria nova sessão no Redis

**Erros:**
- `401 Unauthorized`: Refresh token inválido

---

### 5.2 Customer Service (Port 8081)

**Base Path:** `/api/clientes`

#### **GET /api/clientes**
**Função:** Listar/buscar clientes com filtros

**Query Parameters:**
- `tipo` (opcional): `PF` ou `PJ`
- `busca` (opcional): Texto para buscar em nome, email ou documento
- `cidade` (opcional): Filtro por cidade
- `uf` (opcional): Filtro por UF
- `ativo` (opcional): `true` ou `false`
- `limit` (opcional): Número de resultados (default: 20, max: 100)
- `lastKey` (opcional): Cursor para paginação

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": "uuid",
      "tipo": "PJ",
      "documento": "12345678901234",
      "nome": "Auto Peças Silva LTDA",
      "email": "contato@silva.com.br",
      "telefone": "11987654321",
      "cidade": "São Paulo",
      "uf": "SP",
      "ativo": true,
      "totalCompras": 150,
      "valorTotalCompras": 45000.00,
      "ultimaCompra": "2024-09-15T14:30:00Z"
    }
  ],
  "lastKey": "base64-encoded-key",
  "count": 20
}
```

**Comportamento:**
- Se `busca` contém apenas números: busca por documento (GSI)
- Caso contrário: scan com filtro em nome e email
- Cache de 5 minutos para queries comuns

**Erros:**
- `400 Bad Request`: Parâmetros inválidos

---

#### **GET /api/clientes/{id}**
**Função:** Obter detalhes de um cliente específico

**Response (200 OK):**
```json
{
  "id": "uuid",
  "tipo": "PJ",
  "documento": "12345678901234",
  "nome": "Auto Peças Silva LTDA",
  "nomeFantasia": "Silva Autopeças",
  "email": "contato@silva.com.br",
  "telefone": "11987654321",
  "cep": "01310100",
  "logradouro": "Av. Paulista",
  "numero": "1000",
  "complemento": "Sala 10",
  "bairro": "Bela Vista",
  "cidade": "São Paulo",
  "uf": "SP",
  "inscricaoEstadual": "123456789",
  "ativo": true,
  "dataCriacao": "2024-01-15T10:00:00Z",
  "dataAtualizacao": "2024-09-20T15:30:00Z",
  "totalCompras": 150,
  "valorTotalCompras": 45000.00,
  "ultimaCompra": "2024-09-15T14:30:00Z"
}
```

**Cache:** Redis 1 hora

**Erros:**
- `404 Not Found`: Cliente não encontrado

---

#### **POST /api/clientes**
**Função:** Criar novo cliente

**Request:**
```json
{
  "tipo": "PJ",
  "documento": "12345678901234",
  "nome": "Auto Peças Silva LTDA",
  "nomeFantasia": "Silva Autopeças",
  "email": "contato@silva.com.br",
  "telefone": "11987654321",
  "cep": "01310100",
  "logradouro": "Av. Paulista",
  "numero": "1000",
  "bairro": "Bela Vista",
  "cidade": "São Paulo",
  "uf": "SP",
  "inscricaoEstadual": "123456789"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  ...todos os campos...
}
```

**Validações:**
- CPF/CNPJ válido conforme algoritmo
- Documento único (não pode existir outro cliente com mesmo documento)
- Email único
- Telefone: 10 ou 11 dígitos
- CEP: 8 dígitos
- UF: 2 caracteres
- Nome fantasia obrigatório para PJ

**Efeitos Colaterais:**
- Publica evento `customer.created` na fila `customer-events-queue`
- Email de boas-vindas enviado (via Notification Service)

**Erros:**
- `400 Bad Request`: Dados inválidos
- `409 Conflict`: Documento ou email já cadastrado

---

#### **PUT /api/clientes/{id}**
**Função:** Atualizar dados de cliente existente

**Request:** Igual ao POST

**Response (200 OK):** Cliente atualizado

**Validações:** Iguais ao POST

**Efeitos Colaterais:**
- Publica evento `customer.updated` na fila `customer-events-queue`
- Invalida cache Redis do cliente

**Erros:**
- `404 Not Found`: Cliente não existe
- `409 Conflict`: Documento/email em uso por outro cliente

---

#### **DELETE /api/clientes/{id}**
**Função:** Desativar cliente (soft delete)

**Response (204 No Content)**

**Efeitos Colaterais:**
- Define `ativo = false`
- Publica evento `customer.deactivated`
- Invalida cache

**Erros:**
- `404 Not Found`: Cliente não existe

---

### 5.3 Product Service (Port 8082)

**Base Path:** `/api/produtos`

#### **GET /api/produtos**
**Função:** Listar produtos com busca

**Query Parameters:**
- `busca` (opcional): Busca em nome, código ou NCM
- `categoria` (opcional): Filtro por categoria
- `status` (opcional): `ok|atencao|baixo|critico`
- `ativo` (opcional): `true|false`
- `limit` (opcional): Default 20, max 100

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": "uuid",
      "codigo": "PF-001",
      "nome": "Pastilha de Freio Dianteira",
      "ncm": "87083010",
      "categoria": "Freios",
      "precoCusto": 45.50,
      "precoVenda": 89.90,
      "estoqueAtual": 45,
      "estoqueMinimo": 15,
      "disponivel": 40,
      "reservado": 5,
      "status": "ok",
      "ativo": true
    }
  ]
}
```

**Cache:** Produtos mais consultados (1 hora)

---

#### **GET /api/produtos/{id}**
**Função:** Detalhes do produto + estoque

**Response (200 OK):**
```json
{
  "id": "uuid",
  "codigo": "PF-001",
  "nome": "Pastilha de Freio Dianteira",
  "descricao": "Pastilha para veículos nacionais",
  "ncm": "87083010",
  "cest": "0100100",
  "categoria": "Freios",
  "fabricante": "Fras-le",
  "codigoFabricante": "PD123",
  "precoCusto": 45.50,
  "precoVenda": 89.90,
  "margemLucro": 97.58,
  "estoqueMinimo": 15,
  "ativo": true,
  "dataCriacao": "2024-01-10T10:00:00Z",
  "dataAtualizacao": "2024-09-20T14:00:00Z",
  "estoque": {
    "estoqueAtual": 45,
    "reservado": 5,
    "disponivel": 40,
    "status": "ok",
    "ultimaMovimentacao": "2024-09-20T14:00:00Z"
  }
}
```

**Erros:**
- `404 Not Found`: Produto não existe

---

#### **POST /api/produtos**
**Função:** Criar novo produto

**Request:**
```json
{
  "codigo": "PF-001",
  "nome": "Pastilha de Freio Dianteira",
  "descricao": "Pastilha para veículos nacionais",
  "ncm": "87083010",
  "categoria": "Freios",
  "precoCusto": 45.50,
  "precoVenda": 89.90,
  "estoqueMinimo": 15,
  "cfop": "5102",
  "origem": "0",
  "cstIcms": "00",
  "aliquotaIcms": 18.00
}
```

**Response (201 Created):** Produto criado

**Validações:**
- Código único
- NCM: 8 dígitos
- Preço venda > preço custo
- Estoque mínimo >= 0

**Efeitos Colaterais:**
- Cria registro em `inventory` com estoque inicial 0

**Erros:**
- `409 Conflict`: Código já existe

---

#### **PUT /api/produtos/{id}**
**Função:** Atualizar produto

**Efeitos Colaterais:**
- Invalida cache Redis
- Recalcula margem de lucro

---

#### **DELETE /api/produtos/{id}**
**Função:** Desativar produto (soft delete)

**Response (204 No Content)**

**Regras:**
- Não pode desativar se houver estoque reservado

---

#### **GET /api/produtos/resumo**
**Função:** Estatísticas de estoque

**Response (200 OK):**
```json
{
  "totalProdutos": 847,
  "produtosBaixo": 23,
  "produtosCriticos": 8,
  "valorTotalEstoque": 145890.00,
  "produtosAtivos": 820,
  "produtosInativos": 27
}
```

**Cache:** 5 minutos

---

#### **GET /api/produtos/{id}/movimentos**
**Função:** Histórico de movimentações de estoque

**Query Parameters:**
- `de` (opcional): Data inicial (ISO8601)
- `ate` (opcional): Data final
- `tipo` (opcional): `entrada|saida|ajuste|reserva|liberacao`
- `limit` (opcional): Default 50

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": "uuid",
      "produtoId": "uuid",
      "tipo": "saida",
      "quantidade": -5,
      "estoqueAnterior": 50,
      "estoqueNovo": 45,
      "referenciaId": "venda-uuid",
      "referenciaType": "sale",
      "usuarioId": "uuid",
      "data": "2024-09-20T14:30:00Z"
    }
  ]
}
```

---

#### **POST /api/produtos/{id}/ajuste**
**Função:** Ajuste manual de estoque

**Request:**
```json
{
  "quantidadeNova": 100,
  "motivo": "Inventário físico"
}
```

**Response (200 OK):** Estoque atualizado

**Efeitos Colaterais:**
- Publica evento `adjust` na `inventory-updates-queue`
- Registra movimentação com motivo

**Validações:**
- Motivo obrigatório (mín. 10 caracteres)
- Quantidade >= 0

---

### 5.4 Sales Service (Port 8083)

**Base Path:** `/api/vendas`

#### **POST /api/vendas**
**Função:** Criar nova venda ou orçamento

**Request:**
```json
{
  "tipoDocumento": "nfce",
  "modalidade": "fisica",
  "clienteId": null,
  "metodoPagamento": "credito",
  "bandeira": "visa",
  "destinoDinheiro": null,
  "desconto": 0,
  "status": "finalizada",
  "itens": [
    {
      "produtoId": "uuid",
      "quantidade": 2,
      "valorUnitario": 89.90
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "numero": "VD-000126",
  "tipoDocumento": "nfce",
  "subtotal": 179.80,
  "desconto": 0,
  "total": 179.80,
  "status": "finalizada",
  "criadaEm": "2024-09-20T15:00:00Z",
  "itens": [...]
}
```

**Validações:**
- NF-e exige `clienteId`
- NFC-e permite `clienteId` null
- Pelo menos 1 item
- Verificar estoque disponível para cada item
- Desconto não pode ser maior que subtotal
- `bandeira` obrigatória se método = crédito/débito
- `destinoDinheiro` obrigatório se método = dinheiro

**Efeitos Colaterais:**
1. Cria registro em `sales` e `sale_items`
2. Publica eventos `reserve` na `inventory-updates-queue` (um por item)
3. Se status = `finalizada`: Publica `sale.created` na `sales-events-queue`

**Erros:**
- `400 Bad Request`: Dados inválidos
- `409 Conflict`: Estoque insuficiente para algum item

---

#### **GET /api/vendas/{id}**
**Função:** Detalhes da venda

**Response (200 OK):**
```json
{
  "id": "uuid",
  "numero": "VD-000126",
  "tipoDocumento": "nfce",
  "modalidade": "fisica",
  "cliente": {
    "id": "uuid",
    "nome": "João Silva"
  },
  "metodoPagamento": "credito",
  "bandeira": "visa",
  "subtotal": 179.80,
  "desconto": 0,
  "total": 179.80,
  "status": "finalizada",
  "notaFiscalId": "uuid",
  "vendedorId": "uuid",
  "criadaEm": "2024-09-20T15:00:00Z",
  "finalizadaEm": "2024-09-20T15:00:00Z",
  "itens": [
    {
      "itemId": "uuid",
      "produtoId": "uuid",
      "codigo": "PF-001",
      "nome": "Pastilha de Freio",
      "quantidade": 2,
      "valorUnitario": 89.90,
      "valorTotal": 179.80
    }
  ]
}
```

---

#### **GET /api/vendas**
**Função:** Listar vendas

**Query Parameters:**
- `status` (opcional): `orcamento|finalizada|cancelada`
- `clienteId` (opcional)
- `vendedorId` (opcional)
- `de` (opcional): Data inicial
- `ate` (opcional): Data final
- `limit` (opcional)

**Response (200 OK):** Array de vendas

---

#### **PUT /api/vendas/{id}/status**
**Função:** Alterar status da venda

**Request:**
```json
{
  "status": "finalizada"
}
```

**Transições Permitidas:**
- `orcamento` → `finalizada`
- `orcamento` → `cancelada`

**Efeitos Colaterais (orcamento → finalizada):**
- Publica `sale.created` na `sales-events-queue`
- Dispara emissão fiscal

**Efeitos Colaterais (cancelada):**
- Publica eventos `release` na `inventory-updates-queue`
- Libera estoque reservado

---

#### **DELETE /api/vendas/{id}**
**Função:** Cancelar venda

**Regras:**
- Apenas vendas com status `orcamento` podem ser canceladas
- Vendas finalizadas devem usar cancelamento fiscal

**Response (204 No Content)**

---

### 5.5 Fiscal Service (Port 8084)

**Base Path:** `/api/notas`

#### **GET /api/notas**
**Função:** Listar notas fiscais

**Query Parameters:**
- `status` (opcional): `processando|autorizada|rejeitada|cancelada`
- `tipo` (opcional): `NFE|NFCE|DEVOLUCAO|ENTRADA`
- `de` (opcional): Data inicial
- `ate` (opcional): Data final
- `clienteId` (opcional)
- `limit` (opcional)

**Response (200 OK):**
```json
{
  "items": [
    {
      "id": "uuid",
      "tipo": "NFCE",
      "numero": "000126",
      "serie": "1",
      "chaveAcesso": "35240912345678901234550010001260001234567890",
      "valorTotal": 179.80,
      "emitidaEm": "2024-09-20T15:00:00Z",
      "status": "autorizada",
      "protocolo": "135240000012345",
      "cliente": "João Silva"
    }
  ]
}
```

---

#### **GET /api/notas/{id}**
**Função:** Detalhes da nota fiscal

**Response (200 OK):**
```json
{
  "id": "uuid",
  "tipo": "NFCE",
  "modelo": "65",
  "numero": "000126",
  "serie": "1",
  "chaveAcesso": "35240912345678901234550010001260001234567890",
  "vendaId": "uuid",
  "clienteId": null,
  "valorProdutos": 179.80,
  "valorDesconto": 0,
  "valorTotal": 179.80,
  "valorIcms": 32.36,
  "emitidaEm": "2024-09-20T15:00:00Z",
  "autorizadaEm": "2024-09-20T15:01:30Z",
  "status": "autorizada",
  "protocolo": "135240000012345",
  "codigoStatus": "100",
  "ambiente": "producao",
  "tentativasEnvio": 1,
  "xmlPath": "s3://wc-documents-bucket/NFCE/2024/09/35240912...xml",
  "pdfPath": "s3://wc-documents-bucket/NFCE/2024/09/35240912...pdf",
  "itens": [...]
}
```

---

#### **POST /api/notas/{id}/reenviar**
**Função:** Reenviar nota rejeitada para SEFAZ

**Response (200 OK):**
```json
{
  "message": "Nota reenviada para processamento",
  "notaId": "uuid"
}
```

**Regras:**
- Apenas notas com status `rejeitada` podem ser reenviadas

**Efeitos Colaterais:**
- Reseta `tentativasEnvio` para 0
- Publica novamente na `sefaz-queue`

---

#### **POST /api/notas/{id}/cancelamento**
**Função:** Cancelar nota fiscal autorizada

**Request:**
```json
{
  "justificativa": "Cliente desistiu da compra após emissão"
}
```

**Response (200 OK):**
```json
{
  "eventoId": "uuid",
  "status": "processando",
  "message": "Solicitação de cancelamento enviada à SEFAZ"
}
```

**Validações:**
- Nota deve estar `autorizada`
- Prazo máximo: 24 horas desde autorização
- Justificativa mínima: 15 caracteres

**Efeitos Colaterais:**
1. Cria registro em `fiscal_events` (tipo: CANCELAMENTO)
2. Publica na `fiscal-events-queue`
3. Listener processa e envia evento para SEFAZ
4. Após confirmação: Atualiza status para `cancelada`
5. Publica eventos `increase` na `inventory-updates-queue` (devolve estoque)

---

#### **POST /api/notas/inutilizacao**
**Função:** Inutilizar faixa de numeração

**Request:**
```json
{
  "serie": "1",
  "numeroInicial": 100,
  "numeroFinal": 105,
  "justificativa": "Numeração pulada por erro no sistema"
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "serie": "1",
  "numeroInicial": 100,
  "numeroFinal": 105,
  "quantidade": 6,
  "status": "processando"
}
```

**Validações:**
- `numeroFinal` >= `numeroInicial`
- Justificativa mínima: 15 caracteres
- Não pode haver notas autorizadas na faixa

**Efeitos Colaterais:**
- Cria registro em `fiscal_events`
- Publica na `fiscal-events-queue`
- Envia para SEFAZ

---

#### **POST /api/notas/devolucao**
**Função:** Emitir NF-e de devolução

**Request:**
```json
{
  "notaReferenciadaId": "uuid",
  "motivo": "Produto com defeito",
  "itens": [
    {
      "itemId": "uuid",
      "quantidadeDevolver": 1
    }
  ]
}
```

**Response (201 Created):** Nota de devolução criada

**Validações:**
- Nota referenciada deve estar `autorizada`
- Itens devem pertencer à nota original
- `quantidadeDevolver` <= quantidade original
- Pelo menos 1 item com quantidade > 0

**Efeitos Colaterais:**
1. Cria nova nota (tipo: DEVOLUCAO)
2. Dispara processo de emissão fiscal normal
3. Após autorização: Publica eventos `increase` na `inventory-updates-queue`

---

### 5.6 Entry Service (Port 8085)

**Base Path:** `/api/entrada`

#### **POST /api/entrada**
**Função:** Registrar nota de entrada manualmente

**Request:**
```json
{
  "fornecedor": {
    "cnpj": "12345678901234",
    "razaoSocial": "Fornecedor ABC LTDA",
    "ie": "123456789",
    "uf": "SP"
  },
  "nota": {
    "numero": "12345",
    "serie": "1",
    "dataEmissao": "2024-09-20",
    "dataEntrada": "2024-09-21",
    "chaveAcesso": "35240912345678901234550010001234501234567890",
    "naturezaOperacao": "Compra para comercialização"
  },
  "itens": [
    {
      "codigo": "PF-001",
      "descricao": "Pastilha de Freio",
      "ncm": "87083010",
      "quantidade": 100,
      "valorUnitario": 45.50,
      "cfop": "1102"
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": "uuid",
  "status": "registrada",
  "message": "Entrada registrada com sucesso. Estoque será atualizado."
}
```

**Validações:**
- CNPJ do fornecedor válido
- Chave de acesso válida (44 dígitos)
- Data de entrada >= data de emissão
- Pelo menos 1 item

**Efeitos Colaterais:**
1. Cria registro em `entry_notes`
2. Publica eventos `increase` na `inventory-updates-queue` (um por item)
3. Estoque é atualizado automaticamente

---

#### **POST /api/entrada/importar-xml**
**Função:** Importar XML de nota fiscal de entrada

**Request:** `multipart/form-data`
```
file: arquivo.xml (max 5MB)
```

**Response (202 Accepted):**
```json
{
  "uploadId": "uuid",
  "s3Key": "fornecedor/12345678901234/2024/09/nota.xml",
  "message": "XML enviado para processamento"
}
```

**Processo:**
1. Valida XML (formato, tamanho)
2. Upload para S3 bucket `wc-xml-storage`
3. Publica evento na `entry-events-queue`
4. Listener processa assincronamente:
   - Parse do XML
   - Extração de dados
   - Registro automático da entrada
   - Atualização de estoque

**Erros:**
- `400 Bad Request`: XML inválido
- `413 Payload Too Large`: Arquivo > 5MB

---

#### **GET /api/entrada**
**Função:** Listar notas de entrada

**Query Parameters:**
- `fornecedorCnpj` (opcional)
- `de` (opcional)
- `ate` (opcional)
- `limit` (opcional)

**Response (200 OK):** Array de entradas

---

#### **GET /api/entrada/{id}**
**Função:** Detalhes da entrada

**Response (200 OK):** Dados completos da entrada + itens

---

### 5.7 Document Service (Port 8086)

**Base Path:** `/api/documentos`

#### **GET /api/documentos/xml/{notaId}**
**Função:** Download do XML da nota fiscal

**Response (200 OK):**
- Content-Type: `application/xml`
- Content-Disposition: `attachment; filename="NFe35240912...xml"`
- Body: Conteúdo do XML

**Processo:**
1. Busca `xmlPath` na tabela `notas_fiscais`
2. Gera presigned URL do S3 (expire: 1h)
3. Retorna XML ou redirect para S3

---

#### **GET /api/documentos/pdf/{notaId}**
**Função:** Download do PDF (DANFE) da nota

**Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="DANFE_000126.pdf"`

**Processo:**
1. Verifica se PDF já existe no S3
2. Se não: Gera PDF (DANFE) e salva no S3
3. Retorna presigned URL ou redirect

---

#### **POST /api/documentos/lote-contador**
**Função:** Gerar lote de XMLs para envio ao contador

**Request:**
```json
{
  "notaIds": ["uuid1", "uuid2", "uuid3"],
  "emailContador": "contador@exemplo.com.br"
}
```

**Response (200 OK):**
```json
{
  "loteId": "uuid",
  "arquivos": 25,
  "tamanho": "2.5 MB",
  "downloadUrl": "https://s3.../lote-2024-09-20.zip",
  "expiresIn": 3600
}
```

**Processo:**
1. Busca XMLs autorizados no S3
2. Cria arquivo ZIP
3. Salva no bucket `wc-reports-bucket`
4. Envia email com link (via Notification Service)
5. Retorna presigned URL (expire: 1h)

---

### 5.8 Reports Service (Port 8087)

**Base Path:** `/api/dashboard` e `/api/relatorios`

#### **GET /api/dashboard**
**Função:** Dados agregados para dashboard

**Response (200 OK):**
```json
{
  "vendas": {
    "hoje": 15,
    "valorHoje": 12500.00,
    "mes": 450,
    "valorMes": 250000.00,
    "ticketMedio": 833.33
  },
  "estoque": {
    "totalProdutos": 847,
    "produtosBaixo": 23,
    "produtosCriticos": 8,
    "valorTotalEstoque": 145890.00
  },
  "fiscal": {
    "emitidasHoje": 12,
    "autorizadas": 10,
    "processando": 2,
    "rejeitadas": 0,
    "valorHoje": 15000.00
  },
  "clientes": {
    "total": 320,
    "ativos": 305,
    "novosNoMes": 12
  }
}
```

**Cache:** Redis 5 minutos

**Queries Executadas:**
- Vendas do dia/mês (GSI `data-index`)
- Produtos com estoque baixo (GSI `status-index`)
- Notas do dia (GSI `emitidaEm-index`)
- Clientes ativos (GSI `status-index`)

---

#### **GET /api/relatorios/vendas**
**Função:** Relatório de vendas em CSV/Excel

**Query Parameters:**
- `de` (obrigatório): Data inicial
- `ate` (obrigatório): Data final
- `formato` (opcional): `csv` ou `xlsx` (default: csv)
- `clienteId` (opcional)
- `vendedorId` (opcional)

**Response (200 OK):**
- Content-Type: `text/csv` ou `application/vnd.ms-excel`
- Content-Disposition: `attachment; filename="vendas_2024-09.csv"`

**Colunas CSV:**
```
Numero,Data,Cliente,Tipo_Documento,Metodo_Pagamento,Subtotal,Desconto,Total,Nota_Fiscal,Status
VD-000126,2024-09-20,João Silva,NFCE,Crédito,179.80,0,179.80,000126,Finalizada
...
```

---

#### **GET /api/relatorios/estoque**
**Função:** Relatório de estoque atual

**Response (200 OK):** CSV com todos produtos e situação do estoque

---

#### **GET /api/relatorios/fiscal**
**Função:** Relatório de notas fiscais

**Query Parameters:**
- `de` (obrigatório)
- `ate` (obrigatório)
- `tipo` (opcional)
- `status` (opcional)
- `formato` (opcional)

**Response (200 OK):** CSV com notas do período

---

## 6. Filas SQS - Mensageria

### 6.1 customer-events-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 30s  
**Message Retention:** 4 dias  
**Dead Letter Queue:** Nenhuma (eventos não críticos)

**Função:** Comunicar eventos de mudanças em clientes para outros serviços

**Produtores:**
- Customer Service

**Consumidores:**
- Notification Service (envio de emails)

**Formato de Mensagem:**
```json
{
  "eventType": "customer.created | customer.updated | customer.deactivated",
  "customerId": "uuid",
  "timestamp": "2024-09-20T15:00:00Z",
  "data": {
    "id": "uuid",
    "nome": "João Silva",
    "email": "joao@email.com",
    "documento": "12345678901"
  }
}
```

**Processamento:**
- `customer.created`: Envia email de boas-vindas
- `customer.updated`: Log de auditoria
- `customer.deactivated`: Notificação ao time comercial

**Taxa Estimada:** 50-100 mensagens/dia

---

### 6.2 inventory-updates-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 60s  
**Message Retention:** 4 dias  
**Max Receive Count:** 3  
**Dead Letter Queue:** fiscal-dlq

**Função:** Processar atualizações de estoque de forma assíncrona

**Produtores:**
- Sales Service (reservas e baixas)
- Entry Service (entradas)
- Product Service (ajustes manuais)

**Consumidores:**
- Product Service (listener interno: InventoryUpdateListener)

**Formato de Mensagem:**
```json
{
  "type": "reserve | release | decrease | increase | adjust",
  "produtoId": "uuid",
  "quantidade": 100,
  "referenciaId": "uuid",
  "referenciaType": "sale | entry | adjustment",
  "timestamp": "2024-09-20T15:00:00Z",
  "usuarioId": "uuid",
  "motivo": "string (apenas para adjust)"
}
```

**Tipos de Eventos:**
- `reserve`: Reservar estoque ao criar venda
- `release`: Liberar reserva (cancelamento)
- `decrease`: Baixa efetiva (venda finalizada)
- `increase`: Entrada de mercadoria
- `adjust`: Ajuste manual (inventário)

**Processamento:**
1. Buscar produto e inventory no DynamoDB
2. Aplicar operação conforme tipo
3. Recalcular status (ok, atenção, baixo, crítico)
4. Atualizar tabelas `inventory` e `inventory_movements`
5. Se status = crítico: Publicar em `low-stock-alerts-topic`

**Taxa Estimada:** 500-1000 mensagens/dia

**Retry Strategy:**
- 1ª tentativa: Imediata
- 2ª tentativa: 30s depois
- 3ª tentativa: 2min depois
- Após 3 falhas: Move para DLQ

---

### 6.3 sales-events-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 30s  
**Message Retention:** 4 dias  
**Dead Letter Queue:** fiscal-dlq

**Função:** Disparar processo de emissão fiscal para vendas finalizadas

**Produtores:**
- Sales Service

**Consumidores:**
- Fiscal Service

**Formato de Mensagem:**
```json
{
  "eventType": "sale.created | sale.completed",
  "saleId": "uuid",
  "tipoDocumento": "nfe | nfce",
  "clienteId": "uuid (nullable)",
  "valorTotal": 179.80,
  "timestamp": "2024-09-20T15:00:00Z"
}
```

**Processamento:**
1. Fiscal Service consome mensagem
2. Busca dados completos da venda (sale + sale_items)
3. Cria registro em `notas_fiscais` (status: processando)
4. Publica na `fiscal-jobs-queue` para processamento

**Taxa Estimada:** 200-500 mensagens/dia

---

### 6.4 fiscal-jobs-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 120s (2 minutos)  
**Message Retention:** 4 dias  
**Dead Letter Queue:** fiscal-dlq

**Função:** Processar geração de XML e assinatura digital

**Produtores:**
- Fiscal Service

**Consumidores:**
- Fiscal Service (listener: FiscalProcessorListener)

**Formato de Mensagem:**
```json
{
  "jobType": "generate_xml",
  "notaId": "uuid",
  "tipoNota": "NFE | NFCE | DEVOLUCAO",
  "timestamp": "2024-09-20T15:00:00Z"
}
```

**Processamento:**
1. Buscar dados da nota e itens
2. Gerar XML conforme layout 4.00 da NF-e
3. Calcular totalizadores e impostos
4. Assinar digitalmente com certificado A1
5. Validar contra schema XSD
6. Salvar XML no S3
7. Atualizar `xmlPath` no DynamoDB
8. Publicar na `sefaz-queue`

**Taxa Estimada:** 200-500 mensagens/dia

**Tempo Médio de Processamento:** 5-15 segundos

---

### 6.5 sefaz-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 300s (5 minutos)  
**Message Retention:** 4 dias  
**Max Receive Count:** 3  
**Dead Letter Queue:** fiscal-dlq

**Função:** Enviar XMLs para webservices da SEFAZ

**Produtores:**
- Fiscal Service (após gerar XML)

**Consumidores:**
- Fiscal Service (listener: SefazIntegrationListener)

**Formato de Mensagem:**
```json
{
  "notaId": "uuid",
  "xmlPath": "s3://wc-documents-bucket/NFE/2024/09/35240912...xml",
  "ambiente": "producao | homologacao",
  "tipoNota": "NFE | NFCE",
  "tentativa": 1,
  "timestamp": "2024-09-20T15:01:00Z"
}
```

**Processamento:**
1. Buscar XML no S3
2. Enviar para webservice SEFAZ (SOAP)
3. Se assíncrono: Consultar recibo após 5s
4. Processar retorno:
   - **100 (Autorizada)**: Atualizar status, protocolo, chave
   - **Rejeitada**: Atualizar com motivo da rejeição
   - **Erro de comunicação**: Retry
5. Se autorizada: Disparar geração de PDF

**Taxa Estimada:** 200-500 mensagens/dia

**Tempo Médio:** 10-30 segundos (depende da SEFAZ)

**Retry Strategy:**
- 1ª tentativa: Imediata
- 2ª tentativa: 1min depois
- 3ª tentativa: 5min depois
- Após 3 falhas: Move para DLQ

**Códigos SEFAZ:**
- `100`: Autorizado o uso da NF-e
- `101`: Cancelamento de NF-e homologado
- `135`: Evento registrado e vinculado à NF-e
- `2xx`: Rejeições (vários motivos)
- `5xx`: Erros de processamento

---

### 6.6 fiscal-events-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 300s  
**Message Retention:** 4 dias  
**Dead Letter Queue:** fiscal-dlq

**Função:** Processar eventos especiais (cancelamento, inutilização)

**Produtores:**
- Fiscal Service

**Consumidores:**
- Fiscal Service (listener: FiscalEventsListener)

**Formato de Mensagem:**
```json
{
  "eventType": "cancelamento | inutilizacao",
  "eventoId": "uuid",
  "notaId": "uuid (para cancelamento)",
  "chaveAcesso": "44 dígitos",
  "justificativa": "string",
  "serie": "1 (para inutilização)",
  "numeroInicial": 100,
  "numeroFinal": 105,
  "timestamp": "2024-09-20T16:00:00Z"
}
```

**Processamento Cancelamento:**
1. Gerar XML de evento (tpEvento: 110111)
2. Assinar digitalmente
3. Enviar para SEFAZ (webservice RecepcaoEvento)
4. Se aprovado (cStat 135):
   - Atualizar nota: status = cancelada
   - Publicar eventos `increase` na `inventory-updates-queue`

**Processamento Inutilização:**
1. Gerar XML de inutilização
2. Assinar digitalmente
3. Enviar para SEFAZ (webservice InutilizacaoNFe)
4. Se aprovado: Atualizar registro com protocolo

**Taxa Estimada:** 10-50 mensagens/dia

---

### 6.7 entry-events-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 60s  
**Message Retention:** 4 dias  
**Dead Letter Queue:** fiscal-dlq

**Função:** Processar XMLs de nota de entrada importados

**Produtores:**
- Entry Service

**Consumidores:**
- Entry Service (listener: XmlProcessorListener)

**Formato de Mensagem:**
```json
{
  "eventType": "entry.uploaded",
  "entryId": "uuid",
  "s3Key": "fornecedor/12345678901234/2024/09/nota.xml",
  "timestamp": "2024-09-20T14:00:00Z"
}
```

**Processamento:**
1. Download do XML do S3
2. Parse usando biblioteca XML
3. Extrair dados:
   - Fornecedor (CNPJ, Razão Social, IE, UF)
   - Nota (número, série, chave, datas)
   - Itens (código, descrição, NCM, quantidade, valor, CFOP)
4. Validar dados extraídos
5. Criar registro em `entry_notes`
6. Para cada item: Publicar evento `increase` na `inventory-updates-queue`

**Taxa Estimada:** 50-200 mensagens/dia

**Tempo Médio:** 5-10 segundos

---

### 6.8 email-queue

**Tipo:** Standard Queue  
**Visibility Timeout:** 30s  
**Message Retention:** 4 dias  
**Dead Letter Queue:** Nenhuma (emails não críticos)

**Função:** Enfileirar emails para envio via SES

**Produtores:**
- Notification Service (centraliza todos envios)

**Consumidores:**
- Notification Service (listener: EmailSenderListener)

**Formato de Mensagem:**
```json
{
  "to": "cliente@email.com",
  "cc": [],
  "subject": "Bem-vindo à WC Autopeças",
  "template": "welcome | sale-confirmation | low-stock-alert | fiscal-error",
  "data": {
    "nome": "João Silva",
    "customData": "..."
  },
  "priority": "normal | high",
  "timestamp": "2024-09-20T15:00:00Z"
}
```

**Templates Disponíveis:**
- `welcome`: Boas-vindas a novo cliente
- `sale-confirmation`: Confirmação de venda
- `low-stock-alert`: Alerta de estoque baixo (para gerentes)
- `fiscal-error`: Erro na emissão fiscal (para operação)
- `contador-lote`: Lote de XMLs pronto para download

**Processamento:**
1. Carregar template HTML (via MJML)
2. Renderizar com dados
3. Enviar via SES
4. Registrar log de envio

**Taxa Estimada:** 100-300 mensagens/dia

---

### 6.9 fiscal-dlq (Dead Letter Queue)

**Tipo:** Standard Queue  
**Message Retention:** 14 dias

**Função:** Armazenar mensagens que falharam após múltiplas tentativas

**Fontes:**
- inventory-updates-queue (após 3 falhas)
- sales-events-queue (após 3 falhas)
- fiscal-jobs-queue (após 3 falhas)
- sefaz-queue (após 3 falhas)
- fiscal-events-queue (após 3 falhas)
- entry-events-queue (após 3 falhas)

**Monitoramento:**
- CloudWatch Alarm se ApproximateNumberOfMessagesVisible > 5
- Notificação via SNS para time de operações

**Reprocessamento:**
- Manual via console AWS ou script
- Análise da causa raiz antes de reprocessar
- Correção do problema e republicação na fila original

**Motivos Comuns:**
- Timeout na SEFAZ
- Certificado digital expirado
- Produto não encontrado (inconsistência de dados)
- Schema validation failure

---

## 7. Tópicos SNS - Pub/Sub

### 7.1 low-stock-alerts-topic

**Função:** Notificar sobre produtos com estoque crítico

**Publisher:**
- Product Service (ao detectar status = crítico)

**Subscribers:**
```
1. email-queue (SQS) → Notification Service → SES
   - Destinatários: Gerentes e compradores
   
2. admin@wcautopecas.com.br (Email direto)
   - Notificação imediata
   
3. webhook-endpoint (HTTPS)
   - Integração com sistema de compras (futuro)
```

**Formato de Mensagem:**
```json
{
  "MessageType": "low_stock_alert",
  "produtoId": "uuid",
  "codigo": "PF-001",
  "nome": "Pastilha de Freio Dianteira",
  "estoqueAtual": 5,
  "estoqueMinimo": 15,
  "status": "critico",
  "timestamp": "2024-09-20T15:00:00Z"
}
```

**Filtros por Subscriber:**
- Email Queue: Apenas status = "critico"
- Webhook: Todos os alertas

**Taxa Estimada:** 10-30 mensagens/dia

---

### 7.2 sales-notifications-topic

**Função:** Notificar sobre vendas importantes

**Publisher:**
- Sales Service (vendas acima de threshold)

**Subscribers:**
```
1. email-queue (SQS) → Envio de confirmação ao cliente
2. contador@wcautopecas.com.br (Email)
3. slack-webhook (HTTPS) → Canal #vendas
```

**Formato de Mensagem:**
```json
{
  "MessageType": "sale_completed",
  "vendaId": "uuid",
  "numero": "VD-000126",
  "clienteNome": "João Silva",
  "clienteEmail": "joao@email.com",
  "valorTotal": 179.80,
  "notaFiscalId": "uuid",
  "timestamp": "2024-09-20T15:00:00Z"
}
```

**Uso:**
- Confirmação de venda enviada ao cliente
- Notificação em tempo real para equipe comercial
- Integração com CRM (futuro)

**Taxa Estimada:** 200-500 mensagens/dia

---

### 7.3 system-notifications-topic

**Função:** Alertas operacionais e erros críticos

**Publishers:**
- Todos os serviços (em caso de erro crítico)
- CloudWatch Alarms (via subscription)

**Subscribers:**
```
1. email-queue (SQS) → ops-team@wcautopecas.com.br
2. slack-webhook → Canal #ops-alerts
3. pagerduty-webhook (HTTPS) → On-call engineer
```

**Tipos de Notificações:**
```json
{
  "MessageType": "error | warning | info",
  "service": "fiscal-service",
  "severity": "critical | high | medium | low",
  "message": "Certificado digital expira em 7 dias",
  "details": {
    "certificateExpiry": "2024-10-01",
    "daysRemaining": 7
  },
  "timestamp": "2024-09-24T10:00:00Z"
}
```

**Casos de Uso:**
- Certificado digital próximo ao vencimento
- DLQ com muitas mensagens
- Serviço com alta taxa de erro
- Falha na integração SEFAZ
- Disco/memória alta (via CloudWatch)

**Filtros por Subscriber:**
- Email: severity = critical | high
- Slack: Todos
- PagerDuty: apenas critical

**Taxa Estimada:** 5-20 mensagens/dia

---

## 8. Listeners e Consumers

### 8.1 InventoryUpdateListener

**Serviço:** Product Service  
**Fila:** inventory-updates-queue  
**Concorrência:** 10 threads (max concurrent messages)

**Função:** Processar atualizações de estoque de forma assíncrona

**Implementação:**
```
@Component
@SqsListener(value = "inventory-updates-queue", maxConcurrentMessages = 10)
public class InventoryUpdateListener {
    
    @Transactional
    public void onMessage(InventoryUpdateEvent event) {
        // Lógica de processamento
    }
}
```

**Fluxo de Processamento:**
1. Receber mensagem da fila
2. Validar estrutura da mensagem
3. Buscar produto e inventory (DynamoDB)
4. Aplicar operação conforme tipo:
   - `reserve`: inventory.reservado += quantidade
   - `release`: inventory.reservado -= quantidade
   - `decrease`: inventory.estoqueAtual -= quantidade, reservado -= quantidade
   - `increase`: inventory.estoqueAtual += quantidade
   - `adjust`: inventory.estoqueAtual = quantidade (valor absoluto)
5. Calcular novo disponível: estoqueAtual - reservado
6. Calcular novo status usando regras:
   ```
   if (estoqueAtual <= estoqueMinimo * 0.4) → critico
   else if (estoqueAtual < estoqueMinimo * 0.7) → baixo
   else if (estoqueAtual < estoqueMinimo) → atencao
   else → ok
   ```
7. Atualizar `inventory` table
8. Registrar movimentação em `inventory_movements`
9. Se mudou para status "critico": Publicar em `low-stock-alerts-topic`
10. Invalidar cache Redis do produto
11. Deletar mensagem da fila (ACK)

**Tratamento de Erro:**
- Se produto não existe: Log + move para DLQ
- Se estoque insuficiente para reserve: Log + move para DLQ
- Timeout de processamento: Mensagem volta para fila
- Após 3 tentativas: Move automaticamente para DLQ

**Métricas:**
- Tempo médio de processamento: 50-200ms
- Taxa de sucesso: > 99%
- Mensagens na DLQ: Alarme se > 5

---

### 8.2 SalesEventListener

**Serviço:** Fiscal Service  
**Fila:** sales-events-queue  
**Concorrência:** 5 threads

**Função:** Disparar emissão fiscal para vendas finalizadas

**Fluxo:**
1. Receber evento `sale.created`
2. Buscar dados completos da venda (sale + sale_items + customer)
3. Gerar número sequencial da nota fiscal
4. Criar registro em `notas_fiscais`:
   ```
   status: processando
   tipo: NFE ou NFCE
   numero: próximo número da série
   valorTotal: total da venda
   emitidaEm: now()
   ```
5. Criar registros em `notas_items` (cópia de sale_items com dados fiscais)
6. Publicar na `fiscal-jobs-queue` para processamento
7. ACK na fila

**Tratamento de Erro:**
- Venda não encontrada: Log + DLQ
- Duplicação (nota já existe para venda): Log + ignore (idempotência)

**Tempo Médio:** 100-300ms

---

### 8.3 FiscalProcessorListener

**Serviço:** Fiscal Service  
**Fila:** fiscal-jobs-queue  
**Concorrência:** 3 threads (processamento pesado)

**Função:** Gerar XML da nota, assinar e preparar para envio

**Fluxo:**
1. Receber job de geração de XML
2. Buscar nota e itens do DynamoDB
3. Buscar dados do emitente (configuração)
4. Se NF-e: Buscar dados do destinatário (customer)
5. Gerar XML conforme layout 4.00:
   - Cabeçalho (IDE)
   - Emitente (EMIT)
   - Destinatário (DEST) - se houver
   - Detalhes dos produtos (DET)
   - Totalizadores (TOTAL)
   - Transporte (TRANSP)
   - Pagamento (PAG)
   - Informações adicionais (INFADIC)
6. Calcular dígito verificador da chave de acesso
7. Assinar XML digitalmente usando certificado A1:
   - Buscar certificado no Secrets Manager
   - Assinar bloco <infNFe>
   - Adicionar bloco <Signature>
8. Validar XML contra schema XSD
9. Salvar XML no S3:
   ```
   Bucket: wc-documents-bucket
   Key: {tipo}/{ano}/{mes}/{chaveAcesso}.xml
   ```
10. Atualizar nota:
    ```
    xmlPath: s3://...
    chaveAcesso: 44 dígitos
    ```
11. Publicar na `sefaz-queue`
12. ACK na fila

**Tratamento de Erro:**
- Certificado expirado: Log + notificação + DLQ
- Erro na assinatura: Log + DLQ
- Schema validation fail: Log + DLQ (nota com dados inválidos)

**Tempo Médio:** 5-15 segundos

---

### 8.4 SefazIntegrationListener

**Serviço:** Fiscal Service  
**Fila:** sefaz-queue  
**Concorrência:** 2 threads (limitar requisições simultâneas à SEFAZ)

**Função:** Enviar XML para SEFAZ e processar retorno

**Fluxo:**
1. Receber mensagem com notaId e xmlPath
2. Buscar XML no S3
3. Montar envelope SOAP para NFeAutorizacao
4. Enviar para webservice SEFAZ:
   ```
   URL (SP Homologação): 
   https://homologacao.nfe.fazenda.sp.gov.br/ws/nfeautorizacao4.asmx
   
   URL (SP Produção):
   https://nfe.fazenda.sp.gov.br/ws/nfeautorizacao4.asmx
   ```
5. Processar retorno:
   
   **Caso cStat = 100 (Autorizada):**
   - Extrair protocolo e dhRecbto
   - Atualizar nota:
     ```
     status: autorizada
     protocolo: nProt
     autorizadaEm: dhRecbto
     codigoStatus: 100
     ```
   - Disparar geração de PDF (Document Service)
   - Publicar eventos `decrease` na `inventory-updates-queue`
   
   **Caso cStat = 103 ou 105 (Lote em processamento):**
   - Aguardar 5 segundos
   - Consultar recibo via NFeRetAutorizacao
   - Processar resultado da consulta
   
   **Caso cStat = 2xx (Rejeitada):**
   - Atualizar nota:
     ```
     status: rejeitada
     codigoStatus: cStat
     motivoRejeicao: xMotivo
     ```
   - Log detalhado
   
   **Caso erro de comunicação:**
   - Incrementar tentativasEnvio
   - Se < 3: Lançar exceção (mensagem volta para fila)
   - Se >= 3: Move para DLQ + notificação

6. ACK na fila

**Retry Strategy:**
- Timeout SEFAZ: Retry após 1min
- Erro 500: Retry após 5min
- Erro de rede: Retry imediato

**Timeout:** 60 segundos por requisição

**Métricas:**
- Taxa de autorização: > 95%
- Tempo médio: 10-30s
- Taxa de rejeição: < 5%

---

### 8.5 FiscalEventsListener

**Serviço:** Fiscal Service  
**Fila:** fiscal-events-queue  
**Concorrência:** 2 threads

**Função:** Processar eventos de cancelamento e inutilização

**Fluxo Cancelamento:**
1. Receber evento de cancelamento
2. Buscar nota fiscal original
3. Gerar XML de evento (RecepcaoEvento):
   ```xml
   <evento versao="1.00">
     <infEvento>
       <tpEvento>110111</tpEvento>
       <nSeqEvento>1</nSeqEvento>
       <nProt>{protocolo da nota}</nProt>
       <xJust>{justificativa}</xJust>
     </infEvento>
   </evento>
   ```
4. Assinar digitalmente
5. Enviar para SEFAZ
6. Se cStat = 135 (Evento registrado):
   - Atualizar nota: status = cancelada
   - Atualizar fiscal_event: status = concluido, protocolo
   - Para cada item: Publicar evento `increase` na `inventory-updates-queue`
7. Se rejeitado: Atualizar com motivo

**Fluxo Inutilização:**
1. Receber evento de inutilização
2. Gerar XML de inutilização:
   ```xml
   <inutNFe versao="4.00">
     <infInut>
       <ano>{ano 2 dígitos}</ano>
       <CNPJ>{CNPJ emitente}</CNPJ>
       <mod>{modelo 55 ou 65}</mod>
       <serie>{série}</serie>
       <nNFIni>{número inicial}</nNFIni>
       <nNFFin>{número final}</nNFFin>
       <xJust>{justificativa}</xJust>
     </infInut>
   </inutNFe>
   ```
3. Assinar
4. Enviar para SEFAZ (NFeInutilizacao)
5. Se aprovado: Atualizar com protocolo

**Tempo Médio:** 15-45 segundos

---

### 8.6 XmlProcessorListener

**Serviço:** Entry Service  
**Fila:** entry-events-queue  
**Concorrência:** 3 threads

**Função:** Processar XMLs de entrada importados

**Fluxo:**
1. Receber evento com s3Key
2. Download do XML do S3
3. Parse do XML (biblioteca: XStream ou JAXB)
4. Extrair dados:
   ```
   Fornecedor:
   - CNPJ (ide.emit.CNPJ)
   - Razão Social (ide.emit.xNome)
   - IE (ide.emit.IE)
   - UF (ide.emit.enderEmit.UF)
   
   Nota:
   - Número (ide.nNF)
   - Série (ide.serie)
   - Chave (protNFe.infProt.chNFe)
   - Data Emissão (ide.dhEmi)
   - Data Entrada (configurada pelo usuário)
   - Natureza (ide.natOp)
   
   Itens:
   Para cada <det>:
   - Código (prod.cProd)
   - Descrição (prod.xProd)
   - NCM (prod.NCM)
   - Quantidade (prod.qCom)
   - Valor Unitário (prod.vUnCom)
   - CFOP (prod.CFOP)
   ```
5. Validar dados extraídos
6. Buscar ou criar fornecedor (por CNPJ)
7. Criar registro em `entry_notes`
8. Para cada item:
   - Buscar produto por código ou NCM
   - Se não existir: Criar produto automaticamente (opcional)
   - Publicar evento `increase` na `inventory-updates-queue`
9. ACK na fila

**Tratamento de Erro:**
- XML malformado: Log + DLQ
- Schema inválido: Log + DLQ
- Chave de acesso inválida: Log + DLQ

**Tempo Médio:** 5-15 segundos

---

### 8.7 EmailSenderListener

**Serviço:** Notification Service  
**Fila:** email-queue  
**Concorrência:** 5 threads

**Função:** Enviar emails via Amazon SES

**Fluxo:**
1. Receber mensagem com destinatário, template e dados
2. Carregar template HTML do template engine
3. Renderizar template com dados:
   ```
   Template Engine: Thymeleaf ou FreeMarker
   Templates em: /resources/email-templates/
   ```
4. Adicionar CSS inline (para compatibilidade com clients de email)
5. Enviar via SES:
   ```
   From: noreply@wcautopecas.com.br
   To: destinatário
   Subject: assunto
   Body: HTML renderizado
   ```
6. Registrar log de envio (para auditoria)
7. ACK na fila

**Templates Disponíveis:**
- `welcome.html`: Boas-vindas novo cliente
- `sale-confirmation.html`: Confirmação de venda
- `low-stock-alert.html`: Alerta de estoque baixo
- `fiscal-error.html`: Erro na emissão fiscal
- `contador-lote.html`: Lote de XMLs para contador

**Tratamento de Erro:**
- Email inválido: Log + ignore
- Erro SES: Retry 3x com backoff
- Bounce/Complaint: Marcar email como inválido

**Taxa de Envio:** Limitada por SES (default: 14 emails/segundo)

**Tempo Médio:** 200-500ms por email

---

### 8.8 CustomerEventsListener

**Serviço:** Notification Service  
**Fila:** customer-events-queue  
**Concorrência:** 3 threads

**Função:** Processar eventos de clientes e disparar ações

**Fluxo:**
1. Receber evento de cliente
2. Conforme eventType:
   
   **customer.created:**
   - Gerar mensagem de email de boas-vindas
   - Publicar na `email-queue`
   
   **customer.updated:**
   - Log de auditoria
   - Se mudou email: Enviar confirmação para novo email
   
   **customer.deactivated:**
   - Notificar gerente comercial
   - Registrar no CRM (futuro)

3. ACK na fila

**Tempo Médio:** 100-300ms

---

Quer que eu continue com as seções:
- **9. Banco de Dados DynamoDB** (estrutura detalhada de cada tabela, GSIs, queries)
- **10. Integração SEFAZ** (webservices, certificado, layout XML)
- **11. Fluxos de Negócio** (diagramas de sequência completos)
- **12. Segurança** (IAM, encryption, secrets)
- **13. Monitoramento** (CloudWatch, X-Ray, alarmes)
- **14. LocalStack** (docker-compose completo)

Qual seção quer ver agora? 🎯

