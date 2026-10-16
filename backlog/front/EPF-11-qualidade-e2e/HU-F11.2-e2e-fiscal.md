# HU-F11.2 — E2E dos fluxos fiscais e de entrada

**Épico:** [EPF-11](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F11.1, EPF-06 a EPF-10

## História

Como **banca do TCC**, quero **ver cancelamento, devolução, inutilização e entrada funcionando pela tela** para **avaliar a parte fiscal completa**.

## Requisitos funcionais

- RF01 — Painel fiscal: filtro, download de PDF e XML (verifica o arquivo baixado), reenvio de rejeitada (usando o gatilho de rejeição do `sefaz-mock`).
- RF02 — Cancelamento de nota recém-autorizada até "Nota cancelada".
- RF03 — Devolução parcial até autorizada.
- RF04 — Inutilização de faixa alta até protocolo.
- RF05 — Entrada por XML com prévia até "Entrada registrada" e estoque atualizado na tela de estoque.
- RF06 — Vendedor não vê Cancelamento e Inutilização.

## Critérios de aceite

```gherkin
Cenário: entrada por XML
  Quando o teste importa o XML do seed, preenche a data de entrada e registra
  Então a tela de estoque mostra a quantidade somada
```

## Subtasks

- [ ] ST-F11.2.1 Cenários fiscais
- [ ] ST-F11.2.2 Cenário de entrada
- [ ] ST-F11.2.3 Cenário de papel

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F11.2.1 | E2E | painel e downloads | RF01 |
| TC-F11.2.2 | E2E | cancelamento | RF02 |
| TC-F11.2.3 | E2E | devolução | RF03 |
| TC-F11.2.4 | E2E | inutilização | RF04 |
| TC-F11.2.5 | E2E | entrada por XML | entrada |
| TC-F11.2.6 | E2E | vendedor | RF06 |
