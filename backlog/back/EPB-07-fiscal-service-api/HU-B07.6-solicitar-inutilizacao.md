# HU-B07.6 — Solicitar inutilização de faixa

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 3 pts · **Depende de:** HU-B07.1, HU-B01.6

## História

Como **administradora**, quero **inutilizar números de nota que foram pulados** para **ficar em dia com a SEFAZ**.

## Requisitos funcionais

- RF01 — `POST /api/notas/inutilizacao` (ADMIN) com `{serie, numeroInicial, numeroFinal, justificativa, modelo?}` (`modelo` default `55`).
- RF02 — Cria `fiscal_events` (`tipo = INUTILIZACAO`, `status = processando`, `quantidade = final − inicial + 1`).
- RF03 — Publica na `fiscal-events-queue` com `eventType = inutilizacao`.
- RF04 — Responde `201` com `{id, serie, numeroInicial, numeroFinal, quantidade, status}`.

## Regras e requisitos não funcionais

- `numeroFinal ≥ numeroInicial`, ambos inteiros > 0 → `400`.
- Justificativa ≥ 15 caracteres → `400`.
- Nenhuma nota `autorizada`, `cancelada` ou `processando` com número na faixa (mesmo modelo e série) → senão `422` listando os números.
- Faixa não pode sobrepor outra inutilização já registrada → `409`.

## Critérios de aceite

```gherkin
Cenário: inutilizar faixa livre
  Dado que os números 100 a 105 da série 1 não foram usados
  Quando solicito a inutilização
  Então recebo 201 com quantidade 6 e status "processando"

Cenário: faixa com nota autorizada
  Dado a nota 102 autorizada
  Quando solicito 100 a 105
  Então recebo 422 citando o número 102

Cenário: faixa invertida
  Quando envio numeroInicial 10 e numeroFinal 5
  Então recebo 400
```

## Subtasks

- [ ] ST-B07.6.1 Validação de faixa contra notas existentes
- [ ] ST-B07.6.2 Endpoint e publicação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.6.1 | Integração | faixa livre | livre |
| TC-B07.6.2 | Integração | faixa com nota → 422 | com nota |
| TC-B07.6.3 | Unitário | faixa invertida e justificativa curta | invertida |
| TC-B07.6.4 | Integração | sobreposição → 409 | RNF |
