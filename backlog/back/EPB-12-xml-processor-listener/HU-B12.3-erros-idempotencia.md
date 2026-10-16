# HU-B12.3 — Erros e idempotência

**Épico:** [EPB-12](README.md) · **Unidade:** `XmlProcessorListener` · **Estimativa:** 3 pts · **Depende de:** HU-B12.1

## História

Como **operação**, quero **que XMLs ruins não travem a fila e que reentregas não dupliquem estoque** para **o estoque não inflar sozinho**.

## Requisitos funcionais

- RF01 — XML malformado, não-NF-e ou com chave inválida: entrada `status = erro` com `erro` descritivo, log e DLQ (conforme seção 8.6).
- RF02 — Reentrega da mesma mensagem com a entrada já `registrada`/`com_pendencias` → ACK sem republicar `increase`.
- RF03 — Chave de acesso já registrada por outra entrada → `status = erro` "Chave já registrada", sem DLQ.

## Critérios de aceite

```gherkin
Cenário: XML malformado
  Quando o XML malformado do seed é processado
  Então a entrada fica "erro" com a descrição
  E a mensagem termina na fiscal-dlq

Cenário: reentrega
  Quando a mesma mensagem é entregue duas vezes
  Então o estoque soma uma vez
```

## Subtasks

- [ ] ST-B12.3.1 Classificação de erros
- [ ] ST-B12.3.2 Checagem de status antes de publicar

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B12.3.1 | Integração | malformado → erro + DLQ | malformado |
| TC-B12.3.2 | Integração | reentrega | reentrega |
| TC-B12.3.3 | Integração | chave repetida | RF03 |
