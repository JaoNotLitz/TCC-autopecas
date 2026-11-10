# HU-F11.4 — CI do front

**Épico:** [EPF-11](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B00.6

## História

Como **desenvolvedora**, quero **que cada PR do front rode lint, testes e build** para **não quebrar a `main`**.

## Requisitos funcionais

- RF01 — Workflow em PRs que mexem no front: `npm ci`, `ng lint`, testes unitários com cobertura, `ng build`.
- RF02 — Job E2E (pode ser noturno) que sobe a stack do back pelo Compose, roda `make seed` e executa o Playwright, publicando o relatório HTML e os vídeos de falha.
- RF03 — Falha se cobertura dos serviços e componentes alterados < 80%.

## Critérios de aceite

```gherkin
Cenário: PR quebrando teste
  Dado um PR com teste unitário falhando
  Então o check "frontend" fica vermelho
```

## Subtasks

- [ ] ST-F11.4.1 Workflow de lint, teste e build
- [ ] ST-F11.4.2 Job E2E com a stack local

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F11.4.1 | CI | PR com falha proposital | quebrando |
