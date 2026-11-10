# HU-F06.6 — Botão Emitir NF-e

**Épico:** [EPF-06](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-F05.3

## História

Como **responsável fiscal**, quero **que "Emitir NF-e" me leve ao PDV já em modo NF-e** para **emitir uma nota avulsa**.

## Requisitos funcionais

- RF01 — Navega para `/pdv?tipo=nfe`; o PDV lê o parâmetro e seleciona NF-e.
- RF02 — Não há emissão de nota sem venda no back; o botão sempre passa pelo PDV.

## Critérios de aceite

```gherkin
Cenário: emitir NF-e
  Quando clico em Emitir NF-e
  Então o PDV abre com NF-e selecionada e o campo de cliente obrigatório
```

## Subtasks

- [ ] ST-F06.6.1 Navegação e leitura do parâmetro no PDV

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F06.6.1 | Unitário | PDV com `tipo=nfe` | emitir |
