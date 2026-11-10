# HU-F02.3 — Card de notas do dia

**Épico:** [EPF-02](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B07.2, HU-B13.2

## História

Como **vendedor**, quero **ver as últimas notas do dia e baixar XML ou PDF dali** para **atender pedidos de segunda via rápido**.

## Requisitos funcionais

- RF01 — `GET /api/notas?de={hoje}&limit=4`.
- RF02 — Linha: número, cliente, valor (`valorTotal` formatado), hora (`emitidaEm`), selo de status.
- RF03 — Ícones XML e PDF (habilitados só para autorizadas) usam `DocumentosService` (HU-F06.2).
- RF04 — Link "Ver painel fiscal".

## Critérios de aceite

```gherkin
Cenário: notas do dia
  Dado 6 notas hoje
  Então o card mostra as 4 mais recentes com hora em HH:mm
```

## Subtasks

- [ ] ST-F02.3.1 Integração do card

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F02.3.1 | Unitário | fixture com 4 notas, botões habilitados por status | notas do dia |
