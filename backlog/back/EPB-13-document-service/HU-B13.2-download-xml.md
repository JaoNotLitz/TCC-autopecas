# HU-B13.2 — Download do XML

**Épico:** [EPB-13](README.md) · **Unidade:** `document-service` · **Estimativa:** 2 pts · **Depende de:** HU-B08.5

## História

Como **responsável fiscal**, quero **baixar o XML de uma nota** para **guardar ou mandar ao cliente**.

## Requisitos funcionais

- RF01 — `GET /api/documentos/xml/{notaId}` responde `200` com `Content-Type: application/xml` e `Content-Disposition: attachment; filename="NFe{chave}.xml"` (streaming do S3 pelo serviço).
- RF02 — Com `?redirect=true`, responde `302` para uma URL pré-assinada (1 h) usando o host público do LocalStack.
- RF03 — Nota sem XML (`processando` antes do processor) → `404` "XML ainda não disponível".

## Critérios de aceite

```gherkin
Cenário: baixar XML autorizado
  Dado uma nota autorizada
  Quando chamo GET /api/documentos/xml/{id}
  Então recebo o nfeProc com o nome NFe{chave}.xml
```

## Subtasks

- [ ] ST-B13.2.1 Endpoint de streaming e modo redirect

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B13.2.1 | API | conteúdo e headers | baixar |
| TC-B13.2.2 | API | nota sem XML → 404 | RF03 |
