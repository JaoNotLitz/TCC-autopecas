# HU-F08.2 — Histórico de inutilizações

**Épico:** [EPF-08](README.md) · **Estimativa:** 2 pts · **Depende de:** endpoint inexistente (ponto A7 do backlog)

> **Bloqueada:** não há `GET /api/notas/inutilizacao` na arquitetura. Ativar quando o back criar o endpoint.

## História

Como **administradora**, quero **ver as faixas já inutilizadas** para **não pedir a mesma faixa duas vezes**.

## Requisitos funcionais

- RF01 — Tabela com série, faixa, quantidade, justificativa, status, protocolo e data, substituindo `inutilizacoes` mockadas.
- RF02 — Enquanto bloqueada, a tela mostra só o formulário e remove o mock.

## Critérios de aceite

```gherkin
Cenário: histórico
  Dado duas inutilizações concluídas
  Então a tabela lista as duas com protocolo
```

## Subtasks

- [ ] ST-F08.2.1 Remover mock agora
- [ ] ST-F08.2.2 Tabela quando o endpoint existir

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F08.2.1 | Unitário | tabela com fixture | histórico |
