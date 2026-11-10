# HU-F04.5 — Histórico de movimentações

**Épico:** [EPF-04](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B04.8

## História

Como **gerente**, quero **ver as entradas, saídas, reservas e ajustes de um produto** para **entender diferenças no estoque**.

## Requisitos funcionais

- RF01 — Ação "Movimentações" abre painel lateral com `GET /api/produtos/{id}/movimentos`, paginado.
- RF02 — Filtros: período (`de`, `ate`) e tipo.
- RF03 — Colunas: data/hora, tipo (com ícone), quantidade com sinal e cor, estoque anterior → novo, referência (link para a venda ou nota quando houver), usuário, motivo.

## Critérios de aceite

```gherkin
Cenário: ver histórico
  Dado um produto com estoque inicial, uma venda e um ajuste
  Quando abro Movimentações
  Então vejo as 3 linhas da mais recente para a mais antiga
```

## Subtasks

- [ ] ST-F04.5.1 Painel lateral com lista paginada e filtros

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F04.5.1 | Unitário | fixture e filtros | histórico |
