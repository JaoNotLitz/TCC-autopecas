# HU-B09.5 — Retentativas e DLQ

**Épico:** [EPB-09](README.md) · **Unidade:** `SefazIntegrationListener` · **Estimativa:** 3 pts · **Depende de:** HU-B09.2

## História

Como **operação**, quero **que quedas da SEFAZ sejam retentadas com intervalo e, se persistirem, fiquem na DLQ com alerta** para **nenhuma nota se perder**.

## Requisitos funcionais

- RF01 — Erro de comunicação (timeout, conexão recusada, HTTP 5xx): incrementa `tentativasEnvio` na nota e lança exceção para a mensagem voltar.
- RF02 — Espera entre tentativas: 1ª imediata, 2ª após 1 min, 3ª após 5 min (alterando a visibility da mensagem com `ChangeMessageVisibility`).
- RF03 — Após 3 falhas a mensagem vai para a `fiscal-dlq` e publica `fiscal-error` severity `high`. A nota continua `processando` com `tentativasEnvio = 3` (o reprocessamento pela DLQ é do EPB-16).

## Critérios de aceite

```gherkin
Cenário: SEFAZ volta na segunda tentativa
  Dado o mock respondendo 500 uma vez e depois 100
  Então a nota fica autorizada com tentativasEnvio 1

Cenário: SEFAZ fora do ar
  Dado o mock respondendo 500 sempre
  Então após 3 entregas a mensagem está na fiscal-dlq
  E chega um alerta high no system-notifications-topic
```

## Subtasks

- [ ] ST-B09.5.1 Classificação de erros de comunicação
- [ ] ST-B09.5.2 Backoff por visibility
- [ ] ST-B09.5.3 Alerta na terceira falha

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B09.5.1 | Integração (WireMock com cenário) | 500 → 100 | volta |
| TC-B09.5.2 | Integração (tempos reduzidos no perfil test) | 500 sempre → DLQ + alerta | fora do ar |
| TC-B09.5.3 | Unitário | cálculo do backoff | RF02 |
