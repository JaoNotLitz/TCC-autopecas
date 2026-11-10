# HU-F06.2 — Baixar XML e PDF

**Épico:** [EPF-06](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-F04.6, HU-B13.2, HU-B13.3

## História

Como **responsável fiscal**, quero **baixar o XML e o DANFE de uma nota** para **enviar ao cliente ou arquivar**.

## Requisitos funcionais

- RF01 — `DocumentosService.baixarXml(id)` e `.baixarPdf(id, inline?)` usando `requestBlob()` e `baixarArquivo()`.
- RF02 — Botões habilitados: XML para `autorizada` e `cancelada`; PDF para `autorizada` e `cancelada`. Desabilitados com tooltip nos demais.
- RF03 — `404`/`422` mostram a message do back em toast.

## Critérios de aceite

```gherkin
Cenário: baixar DANFE
  Quando clico em PDF numa nota autorizada
  Então baixo DANFE_{numero}.pdf
```

## Subtasks

- [ ] ST-F06.2.1 `DocumentosService`
- [ ] ST-F06.2.2 Botões por status

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F06.2.1 | Unitário | habilitação por status | RF02 |
| TC-F06.2.2 | Unitário | blob e nome do arquivo | baixar |
