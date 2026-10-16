# Backlog — WC Autopeças

Épicos, histórias de usuário, subtasks e testes do TCC, separados em **back** e **front**. Cada épico fica numa pasta própria com um `README.md` (visão do épico) e um arquivo por história.

**Totais:** 18 épicos de back com 92 histórias, 12 épicos de front com 53 histórias. Cada história traz requisitos funcionais, critérios de aceite, subtasks e casos de teste.

Fontes: [`ARQUITETURA-BACKEND-DETALHADA.md`](../ARQUITETURA-BACKEND-DETALHADA.md) e [`MAPEAMENTO-CLASSES.md`](../MAPEAMENTO-CLASSES.md). Quando o backlog e esses documentos divergirem, os documentos valem, exceto nos pontos listados em [Decisões e pontos em aberto](#decisões-e-pontos-em-aberto).

## Princípio do back: um épico, uma unidade de microsserviço

Cada épico de back entrega **uma unidade executável** (uma API Spring Boot, um listener SQS, uma Lambda ou a própria stack de infraestrutura). O épico só termina quando essa unidade sobe com `docker compose up`, fala com o LocalStack e passa nos testes da própria pasta. Tudo roda **100% local**: AWS emulada pelo LocalStack, demais dependências (Redis, simulador da SEFAZ) em containers Docker.

## Estrutura

```
backlog/
├── README.md                  ← este arquivo (convenções, ordem, decisões)
├── back/
│   ├── EPB-00-ambiente-local/
│   │   ├── README.md          ← épico
│   │   ├── HU-B00.1-....md    ← histórias
│   │   └── ...
│   └── ...
└── front/
    ├── EPF-00-fundacao/
    └── ...
```

## Épicos de back

| Épico | Unidade entregue | Tipo | Histórias |
|---|---|---|---|
| [EPB-00](back/EPB-00-ambiente-local/README.md) | Stack local (LocalStack + Docker Compose) e biblioteca `wc-commons` | Infra | 6 |
| [EPB-01](back/EPB-01-auth-service/README.md) | `auth-service` :8080 | API | 6 |
| [EPB-02](back/EPB-02-api-gateway-authorizer/README.md) | Lambda `wc-jwt-authorizer` + API Gateway REST | Lambda + Gateway | 4 |
| [EPB-03](back/EPB-03-customer-service/README.md) | `customer-service` :8081 | API + listener | 7 |
| [EPB-04](back/EPB-04-product-service/README.md) | `product-service` :8082 | API | 8 |
| [EPB-05](back/EPB-05-inventory-listener/README.md) | `InventoryUpdateListener` (product-service) | Listener SQS | 6 |
| [EPB-06](back/EPB-06-sales-service/README.md) | `sales-service` :8083 | API | 7 |
| [EPB-07](back/EPB-07-fiscal-service-api/README.md) | `fiscal-service` :8084 (API + `SalesEventListener`) | API + listener | 7 |
| [EPB-08](back/EPB-08-fiscal-processor/README.md) | `FiscalProcessorListener` (XML, assinatura, XSD) | Listener SQS | 6 |
| [EPB-09](back/EPB-09-sefaz-integration/README.md) | `SefazIntegrationListener` + container `sefaz-mock` | Listener SQS + simulador | 6 |
| [EPB-10](back/EPB-10-fiscal-events-listener/README.md) | `FiscalEventsListener` (cancelamento, inutilização) | Listener SQS | 3 |
| [EPB-11](back/EPB-11-entry-service/README.md) | `entry-service` :8085 | API | 4 |
| [EPB-12](back/EPB-12-xml-processor-listener/README.md) | `XmlProcessorListener` (entry-service) | Listener SQS | 3 |
| [EPB-13](back/EPB-13-document-service/README.md) | `document-service` :8086 + listener de DANFE | API + listener | 4 |
| [EPB-14](back/EPB-14-reports-service/README.md) | `reports-service` :8087 | API | 4 |
| [EPB-15](back/EPB-15-notification-service/README.md) | `notification-service` :8088 (sem API) | Listeners SQS/SNS | 3 |
| [EPB-16](back/EPB-16-operacao-dlq/README.md) | Lambdas `wc-dlq-monitor` e `wc-dlq-redrive` + observabilidade | Lambda | 4 |
| [EPB-17](back/EPB-17-testes-ponta-a-ponta/README.md) | Módulo `e2e-tests` (fluxos completos na stack local) | Testes | 4 |

## Épicos de front (Angular 22)

| Épico | Tela / área | Rota | Histórias |
|---|---|---|---|
| [EPF-00](front/EPF-00-fundacao/README.md) | Fundação de integração (HTTP, erros, tipos, formatadores, paginação, feedback) | — | 6 |
| [EPF-01](front/EPF-01-autenticacao-navegacao/README.md) | Login, sessão, layout e menu | `/login` | 4 |
| [EPF-02](front/EPF-02-home-dashboard/README.md) | Início e cards | `/` | 4 |
| [EPF-03](front/EPF-03-clientes/README.md) | Clientes | `/clientes` | 5 |
| [EPF-04](front/EPF-04-estoque/README.md) | Estoque e produtos | `/estoque` | 6 |
| [EPF-05](front/EPF-05-pdv/README.md) | PDV / vendas | `/pdv` | 7 |
| [EPF-06](front/EPF-06-painel-fiscal/README.md) | Painel fiscal | `/fiscal` | 6 |
| [EPF-07](front/EPF-07-cancelamento/README.md) | Cancelamento de nota | `/cancelamento` | 2 |
| [EPF-08](front/EPF-08-inutilizacao/README.md) | Inutilização de faixa | `/inutilizacao` | 2 |
| [EPF-09](front/EPF-09-devolucao/README.md) | Nota de devolução | `/devolucao` | 3 |
| [EPF-10](front/EPF-10-nota-entrada/README.md) | Nota de entrada | `/entrada` | 4 |
| [EPF-11](front/EPF-11-qualidade-e2e/README.md) | Testes E2E, acessibilidade e CI do front | — | 4 |

## Convenções

**IDs.** Épicos `EPB-nn` (back) e `EPF-nn` (front). Histórias `HU-Bnn.m` e `HU-Fnn.m`. Requisitos funcionais `RFnn` (numerados dentro da história). Subtasks `ST-Bnn.m.k` / `ST-Fnn.m.k`. Casos de teste `TC-Bnn.m.k` / `TC-Fnn.m.k`.

**Formato de cada história:**
1. Cabeçalho com épico, unidade, estimativa (pontos Fibonacci) e dependências.
2. História no formato *Como / quero / para*.
3. Requisitos funcionais (o que o sistema faz).
4. Regras e requisitos não funcionais (validações, desempenho, segurança).
5. Critérios de aceite em Gherkin (Dado / Quando / Então).
6. Subtasks técnicas (checklist).
7. Testes (unitário, integração, contrato, E2E), cada um ligado a um critério.

**Stack de testes.**
- Back: JUnit 5 + Mockito (unitário), Testcontainers com o módulo LocalStack e Redis (integração), RestAssured (API), Spring Cloud Contract ou JSON Schema (contrato com o front).
- Front: runner unitário do Angular CLI (Vitest) com `HttpTestingController`, Playwright para E2E contra a stack local.

## Definition of Ready (DoR)

- Critérios de aceite escritos e revisados.
- Endpoint, fila ou tópico envolvido descrito na arquitetura ou nesta pasta.
- Dependências da história já entregues ou com mock combinado.

## Definition of Done (DoD)

- Código revisado em PR e mergeado na `main`.
- Testes da seção *Testes* implementados e passando no CI.
- Cobertura de linhas ≥ 80% nas classes de domínio e serviço (back) e nos serviços e componentes alterados (front).
- Roda local com `docker compose up` (back) ou `ng serve` apontando para a stack local (front), sem nenhuma conta AWS real.
- Formato de erro, paginação e nomes de campos conforme a seção 5.0 da arquitetura.
- Logs estruturados em JSON com `correlationId` (back).

## Ordem sugerida (sprints de 2 semanas)

| Sprint | Back | Front |
|---|---|---|
| 1 | EPB-00, EPB-01 | EPF-00 |
| 2 | EPB-02, EPB-03 | EPF-01, EPF-03 |
| 3 | EPB-04, EPB-05 | EPF-04, EPF-02 (parcial) |
| 4 | EPB-06, EPB-07 | EPF-05 |
| 5 | EPB-08, EPB-09 | EPF-06 |
| 6 | EPB-10, EPB-11, EPB-12 | EPF-07, EPF-08, EPF-09, EPF-10 |
| 7 | EPB-13, EPB-14, EPB-15 | EPF-02 (completo) |
| 8 | EPB-16, EPB-17 | EPF-11 |

Mapa de dependências entre unidades:

```
EPB-00 ─┬─> EPB-01 ──> EPB-02 (gateway na frente de tudo)
        ├─> EPB-03 ──────────────┐
        ├─> EPB-04 ──> EPB-05 ───┤
        │                        ▼
        └────────────────────> EPB-06 ──> EPB-07 ──> EPB-08 ──> EPB-09 ──> EPB-13
                                            │                     │
                                            └──> EPB-10 <─────────┘
        EPB-11 ──> EPB-12 ──> (EPB-05)
        EPB-14 lê de 03/04/06/07     EPB-15 consome 03/05/06/13
        EPB-16 e EPB-17 dependem de todos
```

## Decisões e pontos em aberto

Pontos em que a arquitetura não fecha ou não roda no LocalStack gratuito. Cada um tem um **default** já usado nas histórias; se a decisão mudar, ajustar as histórias citadas.

| # | Ponto | Default adotado no backlog | Histórias afetadas |
|---|---|---|---|
| A1 | ECS Fargate, ALB, ElastiCache e API Gateway HTTP API (v2) são recursos pagos do LocalStack | Serviços rodam como containers do Docker Compose; Redis em container próprio; API Gateway **REST (v1)** do LocalStack com integração `HTTP_PROXY` direto para cada container (substitui o ALB) | HU-B00.1, HU-B02.2 |
| A2 | `sales-events-queue` tem dois consumidores (Fiscal e Customer). Em SQS, cada mensagem vai para só um deles | Novo tópico SNS `sales-events-topic` com duas filas assinantes: `sales-events-queue` (fiscal) e `customer-sales-events-queue` (customer). O Sales Service publica no tópico | HU-B00.2, HU-B03.7, HU-B06.4, HU-B07.1 |
| A3 | O aviso "nota autorizada" para o Document Service é descrito como "interno", sem fila | Nova fila `document-events-queue`; o PDF também é gerado sob demanda no download se ainda não existir | HU-B00.2, HU-B09.2, HU-B13.1 |
| A4 | Sales precisa checar estoque e endereço do cliente de forma síncrona; a venda precisa saber qual nota foi gerada | Chamadas HTTP internas na rede do Compose, repassando o token: sales → `product-service` e `customer-service`; fiscal → `sales-service` (`PUT /internal/vendas/{id}/nota-fiscal`). Rotas `/internal` não passam pelo gateway | HU-B04.3, HU-B06.2, HU-B06.3, HU-B06.7, HU-B07.1 |
| A5 | Item de entrada cujo `codigo` não existe no catálogo | Entrada manual: `422` listando os códigos desconhecidos. Importação assíncrona: entrada registrada com status `com_pendencias`, item sem `produtoId` e sem movimentar estoque | HU-B11.1, HU-B12.1 |
| A6 | Botão "Adicionar item" na devolução não tem como gerar `itemId` | Botão removido; só itens da nota referenciada podem ser devolvidos | HU-F09.2 |
| A7 | Histórico de inutilizações, CRUD de fornecedores e de usuários não têm endpoint | Fora do escopo do TCC; HU-F08.2 fica bloqueada até existir `GET /api/notas/inutilizacao` | HU-F08.2 |
| A8 | Ambiente SEFAZ | Local sempre aponta para o container `sefaz-mock` (WireMock), ambiente `homologacao`. URL real só por variável de ambiente | HU-B09.1, HU-B09.6 |
| A9 | Linguagem da Lambda | Java 21 (mesma stack dos serviços), empacotada como zip e publicada no LocalStack pelo script de init | HU-B02.1, HU-B16.1 |
| A10 | A arquitetura só cita `papel`, sem matriz de permissões | Papéis `ADMIN` e `VENDEDOR`. Só ADMIN cancela e inutiliza notas, ajusta estoque e desativa clientes e produtos | HU-B01.6, HU-F01.4 |
| A11 | Campos e filtros que o front precisa e a arquitetura não tem | Acrescentados: `vendaId` e `clienteDocumento` em `GET /api/notas`, `eventos[]` no detalhe da nota, `enderecoCompleto` no cliente, `criticos[]` no dashboard, `cadastrado` nos itens da prévia de XML. `POST /api/produtos/{id}/ajuste` responde `202` porque o efeito é assíncrono | HU-B03.3, HU-B04.6, HU-B07.2, HU-B10.3, HU-B11.2, HU-B14.1 |
