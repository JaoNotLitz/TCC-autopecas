# HU-B08.4 — Validar contra o XSD

**Épico:** [EPB-08](README.md) · **Unidade:** `FiscalProcessorListener` · **Estimativa:** 3 pts · **Depende de:** HU-B08.3

## História

Como **responsável fiscal**, quero **que o XML seja conferido com o schema oficial antes do envio** para **descobrir erros de dados sem gastar uma rejeição da SEFAZ**.

## Requisitos funcionais

- RF01 — Valida o XML assinado contra `nfe_v4.00.xsd` (schemas oficiais do pacote PL vigente versionados em `src/main/resources/xsd`).
- RF02 — Erro de schema → nota `rejeitada` com `codigoStatus = "XSD"` e `motivoRejeicao` com a primeira mensagem do validador (campo e motivo), sem publicar na `sefaz-queue`.

## Regras e requisitos não funcionais

- `Schema` compilado uma vez e reutilizado (thread-safe via `Validator` por thread).

## Critérios de aceite

```gherkin
Cenário: XML válido
  Então o XML das notas do seed passa na validação

Cenário: NCM com 7 dígitos
  Dado um item com NCM "8708301"
  Quando o job é processado
  Então a nota fica rejeitada com motivo citando o elemento NCM
```

## Subtasks

- [ ] ST-B08.4.1 Versionar XSDs
- [ ] ST-B08.4.2 `XsdValidator`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B08.4.1 | Unitário | XML válido | válido |
| TC-B08.4.2 | Integração | NCM inválido → rejeitada com motivo | NCM |
