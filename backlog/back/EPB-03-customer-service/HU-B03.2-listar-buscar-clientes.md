# HU-B03.2 — Listar e buscar clientes

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 3 pts · **Depende de:** HU-B03.1

## História

Como **vendedor**, quero **buscar clientes por nome, e-mail ou documento e filtrar por tipo** para **encontrar rápido quem estou atendendo**.

## Requisitos funcionais

- RF01 — `GET /api/clientes` com os filtros do front (`tipo`, `busca`, `documento`), todos opcionais; responde `Cliente[]` (array puro, sem paginação), em ordem alfabética de `nome`.
- RF02 — `documento` informado → query exata no `documento-index` (array com 0 ou 1 item).
- RF03 — `busca` só com dígitos → query por documento; com texto → filtro `contains` em `nomeBusca` (nome normalizado: minúsculo, sem acento) e e-mail.
- RF04 — Lista ativos e inativos (a tela mostra o status e conta os ativos).
- RF05 — Cache Redis de 5 min por combinação de filtros, invalidado em qualquer escrita de cliente.

## Regras e requisitos não funcionais

- O serviço lê todas as páginas do DynamoDB (`ListagemDynamo`) antes de responder.
- `tipo` fora de `PF|PJ` → `400`.

## Critérios de aceite

```gherkin
Cenário: busca exata por documento (PDV)
  Dado um cliente com documento 12345678909
  Quando chamo GET /api/clientes?documento=12345678909
  Então recebo um array com 1 cliente

Cenário: documento não encontrado
  Quando chamo GET /api/clientes?documento=00000000191
  Então recebo 200 com array vazio

Cenário: busca por nome sem acento
  Dado um cliente "João Peças"
  Quando busco "joao"
  Então ele aparece no resultado

Cenário: lista completa
  Dado 1.500 clientes
  Quando chamo GET /api/clientes
  Então recebo um array com os 1.500, sem repetição
```

## Subtasks

- [ ] ST-B03.2.1 Atributo `nomeBusca` normalizado na gravação
- [ ] ST-B03.2.2 Estratégia de consulta por filtro (GSI × scan)
- [ ] ST-B03.2.3 Cache e invalidação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.2.1 | Integração | `documento=` encontra e não encontra | documento |
| TC-B03.2.2 | Integração | busca sem acento | nome |
| TC-B03.2.3 | Integração | 1.500 itens num único array, sem repetição | lista completa |
| TC-B03.2.4 | Unitário | escolha de estratégia por combinação de filtros | RF02–RF03 |
| TC-B03.2.5 | Integração (Redis) | escrita invalida cache | RF05 |
