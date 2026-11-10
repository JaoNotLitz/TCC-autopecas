# HU-B05.1 — Reservar e liberar estoque

**Épico:** [EPB-05](README.md) · **Unidade:** `InventoryUpdateListener` · **Estimativa:** 3 pts · **Depende de:** HU-B04.1

## História

Como **vendedor**, quero **que os itens de uma venda fiquem reservados** para **que outro caixa não venda a mesma peça antes da nota sair**.

## Requisitos funcionais

- RF01 — `reserve`: `reservado += quantidade`, `disponivel = estoqueAtual − reservado`.
- RF02 — `release`: `reservado −= quantidade` (nunca abaixo de 0).
- RF03 — Movimentação `reserva` / `liberacao` com `referenciaId = vendaId`.

## Regras e requisitos não funcionais

- `reserve` só aplica se `disponivel ≥ quantidade` (`ConditionExpression`); caso contrário a mensagem vai para a DLQ com motivo `ESTOQUE_INSUFICIENTE` e é publicado aviso no `system-notifications-topic` (severity `high`). A validação síncrona do sales-service (HU-B06.3) torna isso raro.
- `quantidade` inteira > 0.

## Critérios de aceite

```gherkin
Cenário: reserva
  Dado estoqueAtual 10 e reservado 0
  Quando chega reserve de 3
  Então reservado fica 3 e disponivel 7

Cenário: liberação
  Dado reservado 3
  Quando chega release de 3
  Então reservado fica 0 e disponivel volta a 10

Cenário: reserva maior que o disponível
  Dado disponivel 2
  Quando chega reserve de 5
  Então nada muda e a mensagem termina na fiscal-dlq
```

## Subtasks

- [ ] ST-B05.1.1 Estrutura do listener (`@SqsListener`, 10 mensagens concorrentes)
- [ ] ST-B05.1.2 Operações reserve/release com `UpdateItem` condicional

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B05.1.1 | Unitário | cálculo de reservado/disponivel | reserva / liberação |
| TC-B05.1.2 | Integração (LocalStack) | publica reserve e confere a tabela | reserva |
| TC-B05.1.3 | Integração | reserve acima do disponível vai para DLQ | maior que disponível |
