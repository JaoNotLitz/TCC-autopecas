# HU-B05.6 — Concorrência, idempotência e DLQ

**Épico:** [EPB-05](README.md) · **Unidade:** `InventoryUpdateListener` · **Estimativa:** 5 pts · **Depende de:** HU-B05.4

## História

Como **responsável pelo sistema**, quero **que mensagens repetidas, simultâneas ou inválidas não corrompam o estoque** para **o número na tela ser confiável**.

## Requisitos funcionais

- RF01 — Controle otimista: atributo `versao` em `inventory`; toda escrita usa `ConditionExpression versao = :v`. Conflito → relê e tenta de novo até 5 vezes; depois devolve a mensagem para a fila.
- RF02 — Idempotência: chave `inv:{messageId-de-negócio}` (ex.: `{tipo}:{referenciaId}:{produtoId}`) no Redis com TTL de 7 dias. Mensagem já aplicada → ACK sem efeito.
- RF03 — Mensagem com JSON inválido, `type` desconhecido ou produto inexistente → log de erro com motivo e deixa ir para a `fiscal-dlq` (sem retry infinito).
- RF04 — Visibility timeout 60 s, `maxReceiveCount` 3.

## Critérios de aceite

```gherkin
Cenário: 10 reservas simultâneas
  Dado estoqueAtual 100
  Quando 10 mensagens reserve de 1 unidade chegam ao mesmo tempo
  Então reservado fica exatamente 10

Cenário: mensagem duplicada
  Quando a mesma mensagem increase chega duas vezes
  Então o estoque soma uma vez e há uma única movimentação

Cenário: produto inexistente
  Quando chega uma mensagem para um produtoId que não existe
  Então após 3 tentativas ela está na fiscal-dlq
```

## Subtasks

- [ ] ST-B05.6.1 Atributo `versao` e retry otimista
- [ ] ST-B05.6.2 Chave de idempotência por evento
- [ ] ST-B05.6.3 Classificação de erros (retentável × definitivo)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B05.6.1 | Integração (LocalStack) | 10 mensagens paralelas no mesmo produto | simultâneas |
| TC-B05.6.2 | Integração | mensagem duplicada | duplicada |
| TC-B05.6.3 | Integração | produto inexistente → DLQ | inexistente |
| TC-B05.6.4 | Unitário | JSON inválido e type desconhecido | RF03 |
