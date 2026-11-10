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
│              React 18 + Vite SPA (repo dinastiadoge/tcc)         │
│              (Executado no navegador do usuário)                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS (TLS 1.3)
                           │ REST/JSON
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  AWS API GATEWAY (REST API)                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ • Lambda Authorizer (JWT do Auth Service)                 │  │
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
│  │ Path-based Routing (mais específica primeiro):            │  │
│  │ /api/notas/entrada*      → entry-service:8085             │  │
│  │ /api/notas/*/xml|pdf     → document-service:8086          │  │
│  │ /api/notas/lote-contador → document-service:8086          │  │
│  │ /api/notas/relatorio     → reports-service:8087           │  │
│  │ /api/auth/*       → auth-service:8080                     │  │
│  │ /api/clientes/*   → customer-service:8081                 │  │
│  │ /api/produtos/*   → product-service:8082                  │  │
│  │ /api/vendas/*     → sales-service:8083                    │  │
│  │ /api/notas/*, /api/inutilizacoes → fiscal-service:8084    │  │
│  │ /api/dashboard, /api/relatorios/* → reports-service:8087  │  │
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
# Regras mais específicas têm prioridade menor (avaliadas antes)
/api/notas/entrada, /api/notas/entrada/*      → tg-entry-service
/api/notas/*/xml, /api/notas/*/pdf            → tg-document-service
/api/notas/lote-contador                      → tg-document-service
/api/notas/relatorio                          → tg-reports-service
/api/auth/*                                   → tg-auth-service
/api/clientes, /api/clientes/*                → tg-customer-service
/api/produtos, /api/produtos/*                → tg-product-service
/api/vendas, /api/vendas/*                    → tg-sales-service
/api/notas, /api/notas/*, /api/inutilizacoes  → tg-fiscal-service
/api/dashboard, /api/relatorios/*             → tg-reports-service
```

### 3.3 API Gateway

**Função:** Camada adicional de segurança, rate limiting e validação

**Tipo:** HTTP API (REST API alternativo)

**Features:**
- **Authorizer:** Lambda Authorizer que valida o JWT emitido pelo Auth Service
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
Type: REQUEST (Lambda Authorizer)
Identity Source: $request.header.Authorization
Validação: assinatura HS512 com a chave wc-jwt-secret (Secrets Manager) + expiração
Rotas públicas (sem authorizer): POST /api/auth/login
Contexto repassado aos serviços: usuarioId, papel
```

> O token é emitido pelo próprio Auth Service (não há Cognito). O authorizer só confere assinatura e expiração; a checagem de sessão no Redis (logout/blacklist) continua no Auth Service via `GET /api/auth/me` e no filtro de segurança de cada serviço.

### 3.4 Amazon DynamoDB

**Tabelas e Função:**

| Tabela | Partition Key | Sort Key | GSI | Streams | Função |
|--------|--------------|----------|-----|---------|---------|
| **counters** | recurso | - | - | Não | Último id numérico de cada recurso (`clientes`, `produtos`, `vendas`, `notas`, `inutilizacoes`, `fornecedores`, `usuarios`, `movimentos`, `entradas`); incrementado com `UpdateItem ADD valor :1` |
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
| **entry_notes** | id | - | fornecedor-index, data-index, chave-index | Sim | Notas de entrada |
| **suppliers** | cnpj | - | - | Não | Fornecedores (com `id` numérico), gravados a partir das notas de entrada |

> Todas as chaves `id` são `number` (inteiro sequencial do `counters`), como o front espera. A partir daqui, os exemplos de eventos usam ids numéricos.

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
2. Gerar o token JWT de acesso (8h; sem refresh token, o front volta ao login no `401`)
3. Validar tokens em requisições
4. Gerenciar sessões ativas no Redis
5. Controlar tentativas de login e bloqueio
6. Logout (invalidação de token)

#### **Dependências:**
- **DynamoDB:** Tabela `users`
- **Redis:** Session store
- **Secrets Manager:** Chave JWT (`wc-jwt-secret`), usada para assinar e validar os tokens (HS512)

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
  "customerId": 42,
  "timestamp": "ISO8601",
  "data": {
    "id": 42,
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
  "produtoId": 42,
  "quantidade": "number",
  "referenciaId": 42,
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
  "produtoId": 42,
  "quantidade": 5,
  "vendaId": 42,
  "timestamp": "ISO8601"
}
```
**Destino:** `inventory-updates-queue`

**2. Para Fiscal (Emissão):**
```json
{
  "eventType": "sale.created",
  "saleId": 42,
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
  "notaId": 42,
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
1. Registrar notas fiscais de entrada (`POST /api/notas/entrada`), vindas do formulário da tela
2. Ler o XML do fornecedor e devolver os dados para a tela conferir (`POST /api/notas/entrada/importar-xml`, síncrono, sem gravar)
3. Validar dados da nota e casar os itens com o catálogo pelo `codigo`
4. Manter o cadastro de fornecedores (`suppliers`) a partir das notas
5. Atualizar estoque com entrada de mercadorias
6. *(Extensão, sem uso no front atual)* importação assíncrona em lote via S3 + `entry-events-queue`

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

#### **Fluxo usado pelo front:**
1. A tela envia o XML (`multipart/form-data`, campo `arquivo`) para `POST /api/notas/entrada/importar-xml`
2. O serviço faz o parse e devolve um `EntradaInput` preenchido, sem gravar nada
3. O usuário confere e confirma com `POST /api/notas/entrada`
4. O serviço grava `entry_notes`, atualiza `suppliers`, publica `increase` por item na `inventory-updates-queue` e chama `POST /internal/notas/entrada` no Fiscal Service, que grava a nota `ENTRADA` (listada em `GET /api/notas`) e devolve a `NotaFiscal` da resposta

#### **Fluxo de importação assíncrona (extensão):**
1. Upload do XML para importação em lote
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
  "entryId": 42,
  "s3Key": "fornecedor/12345678901234/2024/09/nota.xml",
  "timestamp": "ISO8601"
}
```
**Destino:** `entry-events-queue`

**2. Para atualização de estoque:**
```json
{
  "type": "increase",
  "produtoId": 42,
  "quantidade": 100,
  "entryId": 42,
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
2. Servir os bytes do XML e do PDF em `GET /api/notas/{id}/xml` e `/pdf` (sem redirect para o S3, porque o front baixa com header `Authorization`)
3. Criar ZIPs de lotes de notas para contador (`POST /api/notas/lote-contador`) e enviar o link por e-mail

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
2. Gerar relatórios CSV, incluindo o relatório de notas do Painel Fiscal (`GET /api/notas/relatorio`)
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

> **O back segue o front.** Esta seção é o contrato que o front React (`dinastiadoge/tcc`, pastas `src/app/types/index.ts` e `src/app/services/`) já consome. Nomes de campos, tipos, caminhos e formatos de resposta vêm de lá. Quando o back precisar de algo que o front ainda não tem, isso aparece como **extra** (campo ou endpoint a mais, que o front ignora) ou na seção 5.10 (pontos que dependem de um PR no front).

### 5.0 Convenções gerais (valem para todos os serviços)

**URL base.** Todos os caminhos desta seção são relativos a `/api`. O front monta a URL como `VITE_API_URL + caminho`, então em ambiente local `VITE_API_URL` aponta para o stage do API Gateway **já com `/api`** (ex.: `http://wcapi.execute-api.localhost.localstack.cloud:4566/local/api`). Sem a variável, o front usa `/api` relativo (proxy do Vite ou mesmo domínio).

**IDs:** `number` inteiro, sequencial por recurso, gerado pela tabela `counters` do DynamoDB (`UpdateItem` com `ADD valor :1`, atômico). `numero` e `serie` de nota fiscal continuam `string`.

**Datas:** ISO 8601 (`2026-09-20T15:00:00Z`). Filtros `de`/`ate` e datas sem hora em `yyyy-MM-dd`.

**Dinheiro:** `number` em reais (`89.9`). **Documento, telefone e NCM:** só dígitos. A formatação é da tela.

**Enums:** `tipoDocumento` da venda é minúsculo (`nfe|nfce`); `tipo` da nota é maiúsculo (`NFE|NFCE|DEVOLUCAO|ENTRADA`). Demais enums minúsculos. Valores exatos na seção 5.10.

**Listagens:** todo `GET` de coleção responde **um array JSON puro** (`[ {...}, {...} ]`), sem envelope e sem paginação. O volume de uma loja cabe numa resposta; o serviço pagina o DynamoDB internamente até o fim e aplica os filtros. Ordem padrão: mais recente primeiro (notas, inutilizações) ou alfabética (clientes, produtos).

**Respostas sem corpo:** `204 No Content` (o front devolve `undefined`).

**Downloads** (XML, PDF, relatório): o serviço devolve **os bytes do arquivo** com `Content-Type` e `Content-Disposition`. Não usar redirect para URL pré-assinada do S3: o front baixa com `fetch` e header `Authorization`, e o S3 recusa esse header.

**Formato de erro** (todo 4xx/5xx), JSON:
```json
{
  "message": "Documento já cadastrado",
  "status": 409,
  "erro": "CONFLICT",
  "campos": [ { "campo": "documento", "message": "Já existe cliente com este documento" } ]
}
```
- `message` é obrigatório e é o que o front mostra (`ApiError.message`). Os demais campos são extras.
- `401` = token ausente, inválido ou expirado: o front apaga o token e volta ao login.
- `403` = papel sem permissão: o front mostra a `message` sem deslogar.

**Autenticação:** `Authorization: Bearer <token>` em toda rota, exceto `POST /auth/login`. O token vive só no `sessionStorage` do navegador.

---

### 5.1 Auth Service (Port 8080)

#### **POST /auth/login**
**Request:**
```json
{ "email": "admin@wcautopecas.com.br", "senha": "senha123" }
```

**Response (200 OK)** — `LoginResponse`:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": { "id": 1, "nome": "Administrador", "email": "admin@wcautopecas.com.br", "papel": "ADMIN" }
}
```
- `papel` é **extra** (o tipo `Usuario` do front tem só `id`, `nome`, `email`); fica pronto para o guard por papel (história H13 do front).
- Token válido por 8h. Não há refresh token: expirou, a próxima chamada recebe `401` e o front volta ao login.

**Validações e efeitos:** e-mail e senha obrigatórios; usuário ativo; bloqueio de 15 min após 5 tentativas erradas; contador zera no sucesso; cria sessão no Redis (TTL 8h); atualiza `ultimoLogin`.

**Erros:** `400` dados inválidos · `401` credenciais inválidas · `423` usuário bloqueado.

#### **GET /auth/me**
**Response (200 OK)** — `Usuario`: `{ "id": 1, "nome": "Administrador", "email": "...", "papel": "ADMIN" }`

Chamado pelo front ao recarregar a página para validar o token salvo. Exige token válido, sessão no Redis e usuário ativo; senão `401`.

#### **POST /auth/logout** *(extra)*
Remove a sessão do Redis e responde `204`. O front atual só apaga o token local; o endpoint fica disponível para quando ele passar a chamá-lo.

---

### 5.2 Customer Service (Port 8081)

**Modelo `Cliente`:**
```json
{
  "id": 12,
  "tipo": "PJ",
  "documento": "12345678000190",
  "nome": "Auto Peças Silva LTDA",
  "email": "contato@silva.com.br",
  "telefone": "11987654321",
  "cidade": "São Paulo",
  "uf": "SP",
  "ativo": true,
  "totalCompras": 42,
  "ultimaCompra": "2026-04-23T14:30:00Z"
}
```
- `totalCompras` = quantidade de vendas finalizadas do cliente; `ultimaCompra` = data da última, ou `null`. Mantidos pelo próprio Customer Service a partir dos eventos de venda.

**`ClienteInput`** (corpo de POST e PUT) = `Cliente` sem `id`, `totalCompras` e `ultimaCompra`: `tipo`, `documento`, `nome`, `email`, `telefone`, `cidade`, `uf`, `ativo`.

#### **GET /clientes**
**Query:** `tipo` (`PF|PJ`), `busca` (texto em nome, e-mail ou documento), `documento` (CPF/CNPJ exato, só dígitos; usa o GSI `documento-index` e devolve 0 ou 1 item). Todos opcionais.

**Response (200 OK):** `Cliente[]`

#### **POST /clientes**
**Response (201 Created):** `Cliente`

**Validações:** `tipo`, `documento` e `nome` obrigatórios; CPF (11 dígitos) ou CNPJ (14) válido e coerente com `tipo`; documento único; e-mail único quando informado; telefone com 10 ou 11 dígitos quando informado; `uf` entre as 27 siglas.

**Efeitos:** publica `customer.created`; e-mail de boas-vindas se houver e-mail.

**Erros:** `400` · `409` documento ou e-mail já cadastrado.

#### **PUT /clientes/{id}**
Corpo `ClienteInput`; mesmas validações. `ativo: true` reativa um cliente desativado. **Response (200 OK):** `Cliente`. Publica `customer.updated`. **Erros:** `400` · `404` · `409`.

#### **DELETE /clientes/{id}**
Soft delete (`ativo = false`). **Response:** `204`. Publica `customer.deactivated`. Exige papel `ADMIN`. **Erros:** `403` · `404`.

#### **GET /clientes/{id}** *(extra)*
Devolve um `Cliente`. Usado pelas chamadas internas (Sales).

---

### 5.3 Product Service (Port 8082)

**Modelo `Produto`:**
```json
{
  "id": 7,
  "codigo": "PF-001",
  "nome": "Pastilha de Freio Dianteira",
  "ncm": "87083010",
  "estoqueAtual": 45,
  "estoqueMinimo": 15,
  "precoCusto": 45.5,
  "precoVenda": 89.9,
  "status": "ok"
}
```
- `status` é **calculado pelo back** (`ok|atencao|baixo|critico`, regra da seção 4.3). O front não manda.

**`ProdutoInput`** (POST e PUT) = `Produto` sem `id` e `status`: `codigo`, `nome`, `ncm`, `estoqueAtual`, `estoqueMinimo`, `precoCusto`, `precoVenda`.

Dados fiscais do produto que a tela não tem (CFOP, origem, CST, alíquota de ICMS) usam os valores padrão de configuração do serviço (`5102`, `0`, `00`, 18%).

#### **GET /produtos**
**Query:** `busca` (nome, código ou NCM), `status` (um valor). Opcionais. Só produtos ativos.

**Response (200 OK):** `Produto[]`

#### **GET /produtos/resumo**
**Response (200 OK)** — `ResumoEstoque`:
```json
{ "totalProdutos": 847, "estoqueBaixo": 23, "valorEmEstoque": 145890.0, "itensCriticos": 8 }
```
- `estoqueBaixo` = produtos com status `baixo` ou `critico`; `itensCriticos` = só `critico`; `valorEmEstoque` = Σ `estoqueAtual × precoCusto`. Cache de 5 min.

#### **POST /produtos**
**Response (201 Created):** `Produto`

**Validações:** código único; NCM com 8 dígitos; `precoVenda` > `precoCusto`; `estoqueMinimo` ≥ 0; `estoqueAtual` ≥ 0.

**Efeitos:** cria o registro em `inventory` com `estoqueAtual`; se > 0, registra movimentação `ajuste` com motivo "Estoque inicial".

**Erros:** `400` · `409` código já existe.

#### **PUT /produtos/{id}**
Corpo `ProdutoInput`. **Response (200 OK):** `Produto` com o `status` recalculado.

Se `estoqueAtual` vier diferente do atual, o serviço registra uma movimentação `ajuste` com motivo "Ajuste pela tela de estoque" e o `usuarioId` do token, publica `adjust` na `inventory-updates-queue` e responde já com o valor novo. Alterar quantidade exige papel `ADMIN` (`403` para VENDEDOR; os demais campos o VENDEDOR pode editar).

**Erros:** `400` · `403` · `404` · `409` código em uso.

#### **DELETE /produtos/{id}**
Soft delete. **Response:** `204`. Exige `ADMIN`. **Erros:** `403` · `404` · `409` produto com estoque reservado.

#### Extras (não usados pelo front atual)
- `GET /produtos/{id}` → `Produto`.
- `GET /produtos/{id}/movimentos?de&ate&tipo` → array de movimentações `{ id, produtoId, tipo, quantidade, estoqueAnterior, estoqueNovo, motivo, referenciaId, usuarioId, data }`.

---

### 5.4 Sales Service (Port 8083)

#### **POST /vendas**
**Request** — `VendaInput`:
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
  "itens": [ { "produtoId": 7, "quantidade": 2, "valorUnitario": 89.9 } ]
}
```
- `status: "orcamento"` salva o orçamento e reserva o estoque (a reserva é liberada se o orçamento for cancelado). `"finalizada"` também reserva e dispara a emissão do documento fiscal.

**Response (201 Created)** — `Venda`:
```json
{
  "id": 126,
  "tipoDocumento": "nfce",
  "modalidade": "fisica",
  "clienteId": null,
  "metodoPagamento": "credito",
  "bandeira": "visa",
  "destinoDinheiro": null,
  "desconto": 0,
  "subtotal": 179.8,
  "total": 179.8,
  "status": "finalizada",
  "itens": [ { "produtoId": 7, "quantidade": 2, "valorUnitario": 89.9 } ]
}
```

**Validações:**
- NF-e exige `clienteId` de um cliente ativo; NFC-e aceita `null`.
- Pelo menos 1 item; `quantidade` ≥ 1; `valorUnitario` > 0 (o PDV permite editar o preço).
- `desconto` entre 0 e `subtotal`.
- `bandeira` obrigatória com `credito`/`debito` e `null` nos demais; `destinoDinheiro` obrigatório com `dinheiro` e `null` nos demais.
- Estoque disponível para cada item, somando linhas do mesmo produto (chamada síncrona ao product-service).

**Efeitos:** reserva o estoque (`reserve` na `inventory-updates-queue`, um por item). Se `finalizada`, publica `sale.created` no `sales-events-topic`. A emissão é **assíncrona**: a nota aparece em `GET /notas` com `vendaId` = id da venda e `status: "processando"` até a SEFAZ responder.

**Erros:** `400` dados inválidos ou desconto acima do subtotal · `404` cliente ou produto inexistente · `409` estoque insuficiente (`message` cita o produto).

#### Extras (não usados pelo front atual)
- `GET /vendas?status&clienteId&de&ate` → `Venda[]`.
- `GET /vendas/{id}` → `Venda` + `notaFiscalId` (extra).
- `PUT /vendas/{id}/status` com `{ "status": "finalizada" }` finaliza um orçamento.

---

### 5.5 Fiscal Service (Port 8084)

**Modelo `NotaFiscal`:**
```json
{
  "id": 310,
  "tipo": "NFCE",
  "numero": "000126",
  "serie": "1",
  "chaveAcesso": "35260912345678000190650010001260001234567890",
  "clienteId": null,
  "clienteNome": null,
  "clienteDocumento": null,
  "fornecedorId": null,
  "valorTotal": 179.8,
  "emitidaEm": "2026-09-20T15:00:00Z",
  "status": "autorizada",
  "vendaId": 126,
  "notaReferenciadaId": null
}
```
- `clienteNome`/`clienteDocumento` são copiados para a nota na emissão (não exigem chamada extra). Em nota de entrada, `fornecedorId` vem preenchido e os campos de cliente ficam `null`.
- `chaveAcesso` é `null` enquanto a nota não foi gerada.
- `status`: `processando|autorizada|cancelada` (contrato do front) e `rejeitada` (ver seção 5.10).

**Modelo `NotaFiscalItem`:** `{ "produtoId": 7, "codigo": "PF-001", "descricao": "Pastilha de Freio Dianteira", "ncm": "87083010", "cfop": "5102", "quantidade": 2, "valorUnitario": 89.9 }`

#### **GET /notas**
**Query:** `status`, `de`, `ate` (`yyyy-MM-dd`, sobre `emitidaEm`). Extras opcionais: `tipo`, `vendaId`.

**Response (200 OK):** `NotaFiscal[]`, mais recente primeiro. Inclui notas de venda, devolução e entrada.

#### **GET /notas/{id}/itens**
**Response (200 OK):** `NotaFiscalItem[]`. **Erros:** `404`.

#### **POST /notas/{id}/cancelamento**
**Request:** `{ "justificativa": "Cliente desistiu da compra após emissão" }`

**Response (200 OK):** `NotaFiscal` com `status: "processando"`. Vira `cancelada` quando a SEFAZ homologa o evento; se a SEFAZ recusar, volta para `autorizada`.

**Validações (feitas no back; a tela não confere o prazo):** nota `autorizada`; até 24h após a autorização; justificativa com ≥ 15 caracteres após `trim`. Exige `ADMIN`.

**Efeitos:** grava em `fiscal_events` (CANCELAMENTO) e publica na `fiscal-events-queue`. Após homologação, devolve o estoque (`increase`).

**Erros:** `400` justificativa curta · `403` · `404` · `409` já existe cancelamento em processamento · `422` nota não autorizada ou prazo de 24h vencido (`message`: "Prazo de cancelamento expirado").

#### **POST /notas/devolucao**
**Request** — `DevolucaoInput`:
```json
{
  "notaReferenciadaId": 310,
  "motivo": "Produto com defeito",
  "itens": [ { "produtoId": 7, "quantidade": 1, "valorUnitario": 89.9 } ]
}
```

**Response (201 Created):** `NotaFiscal` (`tipo: "DEVOLUCAO"`, `status: "processando"`, `notaReferenciadaId` preenchido).

**Validações:**
- Nota referenciada `autorizada` e de venda (`NFE` ou `NFCE`).
- Cada `produtoId` precisa estar entre os itens da nota referenciada (não dá para devolver item que não estava na nota).
- `quantidade` > 0 e ≤ quantidade vendida daquele produto menos o que já foi devolvido em devoluções não rejeitadas.
- O `valorUnitario` usado é o da nota original; o enviado pelo front é ignorado.
- `motivo` opcional; vazio vira "Devolução de mercadoria".

**Efeitos:** cria a nota de devolução e segue o fluxo normal de emissão (fila `fiscal-jobs-queue`). Após autorização, devolve o estoque (`increase`).

**Erros:** `400` nenhum item com quantidade · `404` · `422` nota referenciada não autorizada, produto fora da nota ou quantidade acima do saldo.

#### **POST /notas/{id}/reenviar** *(extra)*
Reenvia uma nota `rejeitada` à SEFAZ. Responde `NotaFiscal` com `status: "processando"`.

---

### 5.6 Inutilização (Fiscal Service, Port 8084)

**Modelo `Inutilizacao`:**
```json
{
  "id": 4,
  "serie": "1",
  "numeroInicial": 100,
  "numeroFinal": 105,
  "justificativa": "Numeração pulada por erro no sistema",
  "protocolo": null,
  "registradaEm": "2026-09-20T15:00:00Z"
}
```
`protocolo` fica `null` até a SEFAZ homologar.

#### **GET /inutilizacoes**
**Response (200 OK):** `Inutilizacao[]`, mais recente primeiro.

#### **POST /inutilizacoes**
**Request** — `InutilizacaoInput`: `{ "serie": "1", "numeroInicial": 100, "numeroFinal": 105, "justificativa": "..." }`

**Response (201 Created):** `Inutilizacao`

**Validações:** `numeroFinal` ≥ `numeroInicial`; justificativa ≥ 15 caracteres; nenhuma nota emitida na faixa; faixa não inutilizada antes. Exige `ADMIN`.

**Efeitos:** grava em `fiscal_events` (INUTILIZACAO) e publica na `fiscal-events-queue`; o listener preenche `protocolo` após a homologação.

**Erros:** `400` · `403` · `409` faixa sobrepõe outra inutilização · `422` faixa com nota emitida (a `message` lista os números).

---

### 5.7 Entry Service (Port 8085) — nota de entrada

Os caminhos ficam sob `/notas/entrada` porque é assim que o front chama; o gateway roteia esse prefixo para o entry-service.

**`EntradaInput`:**
```json
{
  "fornecedor": { "cnpj": "12345678000190", "razaoSocial": "Fornecedor ABC LTDA", "ie": "123456789", "uf": "SP" },
  "numero": "12345",
  "serie": "1",
  "dataEmissao": "2026-09-20",
  "dataEntrada": "2026-09-21",
  "chaveAcesso": "35260912345678000190550010000123451234567890",
  "naturezaOperacao": "Compra para comercialização",
  "itens": [
    { "produtoId": null, "codigo": "PF-001", "descricao": "Pastilha de Freio", "ncm": "87083010", "cfop": "1102", "quantidade": 100, "valorUnitario": 45.5 }
  ]
}
```

#### **POST /notas/entrada**
**Response (201 Created):** `NotaFiscal` com `tipo: "ENTRADA"`, `status: "autorizada"` (é nota de terceiro, já autorizada), `fornecedorId` preenchido.

**Validações:** CNPJ válido; chave de acesso com 44 dígitos e ainda não registrada; `dataEntrada` ≥ `dataEmissao`; pelo menos 1 item; cada item com `produtoId` informado **ou** `codigo` existente no catálogo (decisão A5 do backlog: código desconhecido → `422` listando os códigos em `campos`).

**Efeitos:** grava em `entry_notes`; cria ou atualiza o fornecedor (tabela `suppliers`, chave `cnpj`, id numérico); publica `increase` por item na `inventory-updates-queue`; chama o Fiscal Service (`POST /internal/notas/entrada`, rede interna, decisão A4 do backlog), que grava a nota `ENTRADA` em `notas_fiscais` e devolve a `NotaFiscal` usada como resposta. Assim a entrada aparece em `GET /notas`.

**Erros:** `400` · `409` chave já registrada · `422` código de produto desconhecido.

#### **POST /notas/entrada/importar-xml**
**Request:** `multipart/form-data` com o campo **`arquivo`** (XML da NF-e, máx. 5 MB).

**Response (200 OK):** `EntradaInput` preenchido a partir do XML, **sem registrar nada**. Os itens já vêm com `produtoId` quando o `codigo` existe no catálogo, senão `null`. `dataEntrada` vem com a data de hoje. O usuário confere na tela e confirma com `POST /notas/entrada`.

**Processo:** parse síncrono; não grava no S3, não publica evento, não mexe em estoque.

**Erros:** `400` arquivo não é XML de NF-e · `409` chave já registrada · `413` arquivo acima de 5 MB.

> A importação assíncrona em lote (`POST /notas/entrada/importar-xml/lote`: upload no S3 + `entry-events-queue` + `XmlProcessorListener`) não é usada pelo front atual e fica como extensão (épico EPB-12). Também são extras `GET /notas/entrada` (array de entradas) e `GET /notas/entrada/{id}`.

---

### 5.8 Document Service (Port 8086) — arquivos da nota

Também ficam sob `/notas` porque o front os chama assim; o gateway roteia estes caminhos específicos para o document-service.

#### **GET /notas/{id}/xml**
Bytes do XML (`nfeProc`). `Content-Type: application/xml`, `Content-Disposition: attachment; filename="NFe<chave>.xml"`. **Erros:** `404` nota inexistente ou ainda sem XML.

#### **GET /notas/{id}/pdf**
Bytes do DANFE (NF-e) ou DANFE NFC-e (cupom). `Content-Type: application/pdf`. Se o PDF ainda não existir no S3, é gerado na hora e salvo. Nota cancelada sai com o carimbo "CANCELADA". Usado também pelo botão "Imprimir Cupom" do PDV. **Erros:** `404` nota inexistente · `422` nota ainda não autorizada (processando ou rejeitada).

#### **POST /notas/lote-contador**
Sem corpo. **Response:** `204`.

**Processo:** junta os XMLs das notas `autorizada` e `cancelada` do mês corrente ainda não enviadas, gera um ZIP no bucket `wc-reports-bucket` e envia ao e-mail do contador configurado (`CONTADOR_EMAIL`) via Notification Service, com link pré-assinado de 7 dias. Marca as notas como enviadas.

**Erros:** `409` nenhuma nota pendente de envio no mês · `500` falha ao gerar o ZIP.

---

### 5.9 Reports Service (Port 8087)

#### **GET /notas/relatorio**
**Query:** `status`, `de`, `ate` (os mesmos filtros de `GET /notas`).

**Response (200 OK):** CSV com as notas filtradas. `Content-Type: text/csv; charset=utf-8`, `Content-Disposition: attachment; filename="notas_<de>_<ate>.csv"`.

Colunas: `Tipo,Numero,Serie,Chave,Cliente,Documento,Valor,Emitida_Em,Status`.

#### Extras (não usados pelo front atual)
- `GET /dashboard` → contadores agregados de vendas, estoque, fiscal e clientes (cache 5 min). O front hoje deriva os números das listas e de `/produtos/resumo`.
- `GET /relatorios/vendas?de&ate` e `GET /relatorios/estoque` → CSV.

---

### 5.10 Enums do contrato e pontos que dependem do front

| Enum | Valores |
|---|---|
| `TipoPessoa` | `PF`, `PJ` |
| `TipoDocumento` | `nfe`, `nfce` |
| `Modalidade` | `fisica`, `virtual` |
| `MetodoPagamento` | `dinheiro`, `credito`, `debito`, `pix`, `boleto` |
| `BandeiraCartao` | `mastercard`, `visa`, `elo`, `outros` |
| `DestinoDinheiro` | `banco`, `cofre` |
| `StatusEstoque` | `ok`, `atencao`, `baixo`, `critico` |
| `StatusNota` | `processando`, `autorizada`, `cancelada` (+ `rejeitada`, ver abaixo) |
| `TipoNota` | `NFE`, `NFCE`, `DEVOLUCAO`, `ENTRADA` |
| `StatusVenda` | `orcamento`, `finalizada` |
| `Papel` (extra) | `ADMIN`, `VENDEDOR` |

**Pontos que o back não consegue resolver sozinho** (pedem um PR pequeno no repositório do front quando chegar a hora):

1. **Nota rejeitada pela SEFAZ.** O back precisa do status `rejeitada`; o tipo `StatusNota` do front ainda não o tem. Até lá, a tela mostra o texto cru. Proposta: acrescentar `"rejeitada"` ao tipo e um botão "Reenviar" (`POST /notas/{id}/reenviar`).
2. **Endereço do destinatário na NF-e.** O cadastro do front só tem `cidade` e `uf`. O simulador da SEFAZ aceita assim; uma emissão real de NF-e exige logradouro, número, bairro e CEP do destinatário.
3. **Guard por papel (H13 do front).** O back já devolve `papel` e responde `403`; falta o front usar.

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
  "customerId": 42,
  "timestamp": "2024-09-20T15:00:00Z",
  "data": {
    "id": 42,
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
  "produtoId": 42,
  "quantidade": 100,
  "referenciaId": 42,
  "referenciaType": "sale | entry | adjustment",
  "timestamp": "2024-09-20T15:00:00Z",
  "usuarioId": 42,
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
  "saleId": 42,
  "tipoDocumento": "nfe | nfce",
  "clienteId": null,
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
  "notaId": 42,
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
  "notaId": 42,
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
  "eventoId": 42,
  "notaId": 42,
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
  "entryId": 42,
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
  "produtoId": 42,
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
  "vendaId": 42,
  "numero": "VD-000126",
  "clienteNome": "João Silva",
  "clienteEmail": "joao@email.com",
  "valorTotal": 179.80,
  "notaFiscalId": 42,
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

