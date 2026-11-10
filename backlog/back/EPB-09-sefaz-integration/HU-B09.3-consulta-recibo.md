# HU-B09.3 — Consultar recibo de lote em processamento

**Épico:** [EPB-09](README.md) · **Unidade:** `SefazIntegrationListener` · **Estimativa:** 3 pts · **Depende de:** HU-B09.2

## História

Como **sistema fiscal**, quero **consultar o resultado quando a SEFAZ responde que o lote está em processamento** para **não deixar a nota parada**.

## Requisitos funcionais

- RF01 — `cStat 103` ou `105`: guarda `nRec`, aguarda 5 s e consulta `NFeRetAutorizacao4`.
- RF02 — Até 3 consultas com 5, 10 e 20 s de espera; o resultado segue o mesmo tratamento de 100 ou 2xx.
- RF03 — Ainda em processamento após 3 consultas → devolve a mensagem para a fila com `tentativa + 1` (atraso de 60 s via `DelaySeconds`/visibility).

## Critérios de aceite

```gherkin
Cenário: lote assíncrono
  Dado uma nota com total terminado em ,13
  Quando o mock responde 103 e depois 100 na consulta
  Então a nota fica autorizada
```

## Subtasks

- [ ] ST-B09.3.1 Cliente `NFeRetAutorizacao4`
- [ ] ST-B09.3.2 Laço de consulta com espera

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B09.3.1 | Integração (WireMock) | 103 → 100 | assíncrono |
| TC-B09.3.2 | Unitário | 3 consultas em processamento → reenfileira | RF03 |
