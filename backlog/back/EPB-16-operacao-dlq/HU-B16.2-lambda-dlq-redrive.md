# HU-B16.2 — Lambda de reprocessamento da DLQ

**Épico:** [EPB-16](README.md) · **Unidade:** Lambda `wc-dlq-redrive` · **Estimativa:** 5 pts · **Depende de:** HU-B16.1

## História

Como **operação**, quero **devolver mensagens da DLQ para a fila de origem depois de corrigir a causa** para **as notas e estoques pendentes serem concluídos**.

## Requisitos funcionais

- RF01 — Lambda invocada por `make redrive ARGS='{"origem":"sefaz-queue","max":10,"dryRun":true}'`.
- RF02 — Identifica a origem de cada mensagem pelo atributo `origemFila` (adicionado pelo `EventPublisher` do `wc-commons`) ou pelo formato do corpo.
- RF03 — `dryRun = true` só lista (id, origem, motivo/último erro, recebimentos); `false` reenvia para a fila de origem com atributo `redrive=true` e apaga da DLQ.
- RF04 — Filtro por `origem` e limite `max`.

## Regras e requisitos não funcionais

- Os listeners zeram `tentativasEnvio` ao receber `redrive=true` (para o SEFAZ voltar a ter 3 tentativas).

## Critérios de aceite

```gherkin
Cenário: listar sem reenviar
  Dado 3 mensagens da sefaz-queue na DLQ
  Quando rodo o redrive com dryRun true
  Então vejo as 3 e elas continuam na DLQ

Cenário: reprocessar após SEFAZ voltar
  Dado o sefaz-mock de volta
  Quando rodo o redrive com dryRun false
  Então as 3 notas terminam autorizadas e a DLQ fica vazia
```

## Subtasks

- [ ] ST-B16.2.1 Atributo `origemFila` no `wc-commons`
- [ ] ST-B16.2.2 Handler com dry run e filtro
- [ ] ST-B16.2.3 Alvo `make redrive`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B16.2.1 | Unitário | identificação de origem | RF02 |
| TC-B16.2.2 | Integração | dry run não altera a DLQ | listar |
| TC-B16.2.3 | E2E (EPB-17) | fluxo SEFAZ fora → DLQ → volta → redrive → autorizada | reprocessar |
