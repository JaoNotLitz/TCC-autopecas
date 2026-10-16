# HU-B17.3 — E2E: entrada por XML até o estoque

**Épico:** [EPB-17](README.md) · **Unidade:** `e2e-tests` · **Estimativa:** 3 pts · **Depende de:** EPB-11, EPB-12

## História

Como **banca do TCC**, quero **ver o XML do fornecedor virar estoque** para **confirmar o fluxo de entrada**.

## Requisitos funcionais

- RF01 — Prévia do XML válido, confirmação por `POST /api/entrada` e checagem do estoque.
- RF02 — Importação assíncrona do XML com item desconhecido → entrada `com_pendencias` e estoque só dos itens vinculados.
- RF03 — Importação do XML malformado → entrada `erro` e mensagem na DLQ.

## Critérios de aceite

```gherkin
Cenário: prévia e confirmação
  Quando o teste envia o XML para prévia e confirma
  Então o estoque dos itens aumenta pelas quantidades do XML
```

## Subtasks

- [ ] ST-B17.3.1 Cenários de prévia, assíncrono e erro

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B17.3.1 | E2E | prévia + confirmação | prévia |
| TC-B17.3.2 | E2E | pendências | RF02 |
| TC-B17.3.3 | E2E | malformado | RF03 |
