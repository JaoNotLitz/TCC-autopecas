# HU-B07.6 — Inutilização de faixa (solicitar e listar)

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 3 pts · **Depende de:** HU-B07.1, HU-B01.6

## História

Como **administradora**, quero **inutilizar números de nota que foram pulados e ver o histórico** para **ficar em dia com a SEFAZ**.

## Requisitos funcionais

- RF01 — `POST /api/inutilizacoes` (ADMIN) com o `InutilizacaoInput` do front: `{serie, numeroInicial, numeroFinal, justificativa}` (modelo `55`).
- RF02 — Cria `fiscal_events` (`tipo = INUTILIZACAO`, `id` numérico do recurso `inutilizacoes`, `status = processando`, `registradaEm`).
- RF03 — Publica na `fiscal-events-queue` com `eventType = inutilizacao`.
- RF04 — Responde `201` com a `Inutilizacao` do front: `{id, serie, numeroInicial, numeroFinal, justificativa, protocolo: null, registradaEm}`. O `protocolo` é preenchido quando a SEFAZ homologa (HU-B10.2).
- RF05 — `GET /api/inutilizacoes` responde `Inutilizacao[]` (array puro), mais recente primeiro, via `tipo-index` de `fiscal_events`.

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
  Então recebo 201 com id numérico e protocolo null

Cenário: histórico
  Dado 2 inutilizações registradas
  Quando chamo GET /api/inutilizacoes
  Então recebo um array com as 2, a mais recente primeiro

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
- [ ] ST-B07.6.2 Endpoints POST e GET e publicação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.6.1 | Integração | faixa livre | livre |
| TC-B07.6.2 | Integração | faixa com nota → 422 | com nota |
| TC-B07.6.3 | Unitário | faixa invertida e justificativa curta | invertida |
| TC-B07.6.4 | Integração | sobreposição → 409 | RNF |
| TC-B07.6.5 | Contrato | POST e GET validam contra `Inutilizacao` do front | histórico |
