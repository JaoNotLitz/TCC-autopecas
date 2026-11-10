# HU-B14.4 — Relatório de notas (Painel Fiscal)

**Épico:** [EPB-14](README.md) · **Unidade:** `reports-service` · **Estimativa:** 2 pts · **Depende de:** HU-B14.2

## História

Como **responsável fiscal**, quero **exportar as notas filtradas no Painel Fiscal** para **conferir com o contador**.

## Requisitos funcionais

- RF01 — `GET /api/notas/relatorio` com os mesmos filtros de `GET /api/notas` (`status`, `de`, `ate`), como o `notasService.relatorio()` do front; o gateway roteia este caminho para o reports-service.
- RF02 — Responde os bytes de um CSV (`text/csv; charset=utf-8`, `Content-Disposition: attachment; filename="notas_{de}_{ate}.csv"`) com as colunas `Tipo, Numero, Serie, Chave, Cliente, Documento, Valor, Emitida_Em, Status`.
- RF03 — Sem filtro de período, usa o mês corrente.

## Critérios de aceite

```gherkin
Cenário: notas canceladas
  Quando peço status=cancelada no período
  Então recebo um CSV só com as notas canceladas

Cenário: mesmos números da tela
  Dado os filtros status=autorizada, de e ate
  Então o CSV tem as mesmas notas que GET /api/notas com os mesmos filtros
```

## Subtasks

- [ ] ST-B14.4.1 Endpoint `/api/notas/relatorio` e geração do CSV

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B14.4.1 | Integração | filtros status e período | canceladas |
| TC-B14.4.2 | Integração | mesmas notas da listagem | mesmos números |
