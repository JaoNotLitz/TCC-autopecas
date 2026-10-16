# EPB-17 — Testes ponta a ponta do back

**Unidade entregue:** módulo Maven `e2e-tests` que sobe a stack completa (Docker Compose via Testcontainers) e exercita os fluxos de negócio pelo API Gateway.
**Tipo:** testes · **Sprint sugerida:** 8 · **Arquitetura:** seção 11

## Objetivo

Provar que as unidades dos épicos anteriores funcionam juntas, sem mocks, como evidência para o TCC.

## Critérios de aceite do épico

- `make e2e` roda os 4 fluxos em menos de 10 minutos e gera relatório HTML.
- O job roda no CI em PRs que mexem em `backend/**` ou `infra/**` (pode ser noturno se ficar lento).

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B17.1](HU-B17.1-e2e-venda-nfce.md) | Venda NFC-e até o DANFE | 5 |
| [HU-B17.2](HU-B17.2-e2e-cancelamento-devolucao.md) | Cancelamento e devolução | 5 |
| [HU-B17.3](HU-B17.3-e2e-entrada-xml.md) | Entrada por XML até o estoque | 3 |
| [HU-B17.4](HU-B17.4-e2e-falhas.md) | Falhas: SEFAZ fora, DLQ e redrive | 5 |
