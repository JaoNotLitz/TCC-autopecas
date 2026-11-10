# HU-F06.5 — Exportar relatório fiscal

**Épico:** [EPF-06](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-B14.4, HU-F04.6

## História

Como **responsável fiscal**, quero **exportar as notas filtradas em planilha** para **conferir com o contador**.

## Requisitos funcionais

- RF01 — "Exportar" (CSV/Excel) chama `GET /api/relatorios/fiscal` com os filtros atuais (`de`, `ate`, `tipo`, `status`).

## Critérios de aceite

```gherkin
Cenário: exportar canceladas do mês
  Dado o filtro Cancelada no mês
  Quando exporto em CSV
  Então o arquivo baixado tem só as canceladas
```

## Subtasks

- [ ] ST-F06.5.1 Botão de exportação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F06.5.1 | Unitário | parâmetros repassados | exportar |
