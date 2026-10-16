# HU-B14.3 — Relatório de estoque

**Épico:** [EPB-14](README.md) · **Unidade:** `reports-service` · **Estimativa:** 2 pts · **Depende de:** HU-B14.2

## História

Como **gerente**, quero **exportar a posição atual do estoque** para **planejar compras**.

## Requisitos funcionais

- RF01 — `GET /api/relatorios/estoque?formato=csv|xlsx&status&categoria`.
- RF02 — Colunas: `Codigo, Nome, NCM, Categoria, Estoque_Atual, Reservado, Disponivel, Estoque_Minimo, Status, Preco_Custo, Preco_Venda, Valor_Estoque`.

## Critérios de aceite

```gherkin
Cenário: estoque crítico
  Quando peço status=critico em xlsx
  Então recebo só os produtos críticos
```

## Subtasks

- [ ] ST-B14.3.1 Endpoint de estoque

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B14.3.1 | Integração | filtro por status | crítico |
