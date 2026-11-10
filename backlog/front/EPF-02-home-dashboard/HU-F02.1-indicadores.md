# HU-F02.1 — Indicadores do dashboard

**Épico:** [EPF-02](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B14.1, HU-F00.4

## História

Como **dona da loja**, quero **ver vendas, estoque, notas e clientes do dia e do mês ao abrir o sistema** para **acompanhar a loja**.

## Requisitos funcionais

- RF01 — `ReportsService.dashboard()` chama `GET /api/dashboard` uma vez ao abrir a tela.
- RF02 — Linha de indicadores: vendas hoje (quantidade e R$), vendas no mês, ticket médio, produtos críticos, notas autorizadas/processando/rejeitadas hoje, clientes ativos.
- RF03 — Botão "Atualizar" (só ADMIN) chama com `?atualizar=true`.
- RF04 — Indicador de rejeitadas > 0 em destaque, levando a `/fiscal?status=rejeitada`.

## Critérios de aceite

```gherkin
Cenário: indicadores reais
  Dado 3 vendas finalizadas hoje
  Quando abro o início
  Então vejo "3" em vendas hoje e o valor em reais

Cenário: back fora
  Quando o dashboard falha
  Então os cards mostram "Não foi possível carregar" com botão de tentar de novo, e o restante da tela funciona
```

## Subtasks

- [ ] ST-F02.1.1 Serviço e signal do dashboard
- [ ] ST-F02.1.2 Componente de indicadores

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F02.1.1 | Unitário (componente) | renderiza fixture do dashboard | reais |
| TC-F02.1.2 | Unitário | erro mostra estado de falha | back fora |
