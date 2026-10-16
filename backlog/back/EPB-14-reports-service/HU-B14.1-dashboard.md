# HU-B14.1 — Dashboard agregado

**Épico:** [EPB-14](README.md) · **Unidade:** `reports-service` · **Estimativa:** 5 pts · **Depende de:** HU-B03.1, HU-B04.1, HU-B06.1, HU-B07.1

## História

Como **dona da loja**, quero **ver na tela inicial vendas do dia e do mês, estoque crítico, notas e clientes** para **saber como está a loja num olhar**.

## Requisitos funcionais

- RF01 — `GET /api/dashboard` no formato da seção 5.8: `vendas {hoje, valorHoje, mes, valorMes, ticketMedio}`, `estoque {totalProdutos, produtosBaixo, produtosCriticos, valorTotalEstoque}`, `fiscal {emitidasHoje, autorizadas, processando, rejeitadas, valorHoje}`, `clientes {total, ativos, novosNoMes}`.
- RF02 — Inclui `estoque.criticos[]` com até 5 produtos `{id, codigo, nome, ncm, estoqueAtual, estoqueMinimo}` (melhoria da seção 7.2.2 do mapeamento).
- RF03 — Só vendas `finalizada` entram em vendas; `ticketMedio = valorMes / mes` (0 se não houver vendas).
- RF04 — Cache Redis `dashboard:stats` 5 min; `?atualizar=true` força recálculo (ADMIN).
- RF05 — "Hoje" e "mês" no fuso `America/Sao_Paulo`.

## Critérios de aceite

```gherkin
Cenário: números do dia
  Dado 3 vendas finalizadas hoje somando 500,00 e 1 orçamento
  Quando chamo GET /api/dashboard?atualizar=true
  Então vendas.hoje é 3 e vendas.valorHoje é 500,00

Cenário: virada do dia no fuso
  Dado uma venda às 22h de ontem em São Paulo (01h UTC de hoje)
  Então ela não conta em vendas.hoje
```

## Subtasks

- [ ] ST-B14.1.1 Consultas por GSI de data e status
- [ ] ST-B14.1.2 Montagem e cache
- [ ] ST-B14.1.3 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B14.1.1 | Integração | números com dados controlados | dia |
| TC-B14.1.2 | Unitário (Clock) | fuso horário | virada |
| TC-B14.1.3 | Contrato | schema `Dashboard` do front | RF01 |
