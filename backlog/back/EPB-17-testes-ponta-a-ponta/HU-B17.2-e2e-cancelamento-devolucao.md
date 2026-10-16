# HU-B17.2 — E2E: cancelamento e devolução

**Épico:** [EPB-17](README.md) · **Unidade:** `e2e-tests` · **Estimativa:** 5 pts · **Depende de:** HU-B17.1, EPB-10

## História

Como **banca do TCC**, quero **ver cancelamento e devolução ajustando nota e estoque** para **confirmar os fluxos fiscais secundários**.

## Requisitos funcionais

- RF01 — Cancelamento: a partir de uma NFC-e autorizada, pede cancelamento, aguarda `cancelada` e confere estoque devolvido e evento concluído.
- RF02 — Devolução: a partir de uma NF-e autorizada de 3 unidades, devolve 1, aguarda a devolução `autorizada`, confere `quantidadeDevolvida = 1` e estoque +1; tenta devolver 3 e recebe `422`.
- RF03 — Inutilização: inutiliza uma faixa acima do último número e confere evento concluído.

## Critérios de aceite

```gherkin
Cenário: cancelamento devolve estoque
  Dado uma NFC-e autorizada de 2 unidades
  Quando o teste a cancela
  Então a nota fica cancelada e o estoque volta 2 unidades
```

## Subtasks

- [ ] ST-B17.2.1 Cenário de cancelamento
- [ ] ST-B17.2.2 Cenário de devolução
- [ ] ST-B17.2.3 Cenário de inutilização

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B17.2.1 | E2E | cancelamento | devolve estoque |
| TC-B17.2.2 | E2E | devolução parcial e excesso | RF02 |
| TC-B17.2.3 | E2E | inutilização | RF03 |
