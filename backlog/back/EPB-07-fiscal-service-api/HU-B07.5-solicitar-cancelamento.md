# HU-B07.5 — Solicitar cancelamento

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 3 pts · **Depende de:** HU-B07.3, HU-B01.6

## História

Como **administradora**, quero **cancelar uma nota autorizada dentro do prazo** para **desfazer uma venda que não aconteceu**.

## Requisitos funcionais

- RF01 — `POST /api/notas/{id}/cancelamento` (ADMIN) com `{justificativa}`.
- RF02 — Cria `fiscal_events` (`tipo = CANCELAMENTO`, `status = processando`, `notaId`, `justificativa`, `usuarioId`, `registradoEm`).
- RF03 — Publica na `fiscal-events-queue` `{eventType: "cancelamento", eventoId, notaId, chaveAcesso, justificativa, timestamp}`.
- RF04 — Responde `200` com `{eventoId, status: "processando", message}`.

## Regras e requisitos não funcionais

- Nota `autorizada` (senão `422`); `agora − autorizadaEm ≤ 24 h` (senão `422` "Prazo de cancelamento expirado"); justificativa ≥ 15 caracteres após `trim` (senão `400`).
- Já existe cancelamento `processando` para a nota → `409`.
- O prazo usa `Clock` injetável para teste.

## Critérios de aceite

```gherkin
Cenário: cancelamento no prazo
  Dado uma nota autorizada há 2 horas
  Quando um ADMIN solicita cancelamento com justificativa válida
  Então recebo 200 com status "processando" e o evento é publicado

Cenário: fora do prazo
  Dado uma nota autorizada há 25 horas
  Então recebo 422 com message "Prazo de cancelamento expirado"

Cenário: justificativa curta
  Quando envio justificativa "erro"
  Então recebo 400 com campos "justificativa"

Cenário: pedido duplicado
  Dado um cancelamento em processamento para a nota
  Quando peço de novo
  Então recebo 409
```

## Subtasks

- [ ] ST-B07.5.1 Entidade `EventoFiscal`
- [ ] ST-B07.5.2 Validações e endpoint
- [ ] ST-B07.5.3 Publicação na `fiscal-events-queue`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.5.1 | Unitário (Clock) | 23h59 aceita, 24h01 recusa | prazo |
| TC-B07.5.2 | Integração | evento gravado e publicado | no prazo |
| TC-B07.5.3 | Unitário | justificativa com espaços nas pontas | curta |
| TC-B07.5.4 | Integração | duplicado → 409 | duplicado |
