# HU-B11.3 — Importar XML para processamento assíncrono

**Épico:** [EPB-11](README.md) · **Unidade:** `entry-service` · **Estimativa:** 3 pts · **Depende de:** HU-B11.2

## História

Como **estoquista com muitas notas**, quero **enviar XMLs para registro direto** para **lançar várias entradas sem conferir uma a uma**.

## Requisitos funcionais

- RF01 — `POST /api/entrada/importar-xml` (`multipart`) valida formato e tamanho, lê o CNPJ do emitente e a data, e salva em `wc-xml-storage` na chave `fornecedor/{cnpj}/{ano}/{mes}/{chave}.xml`.
- RF02 — Cria `entry_notes` com `status = recebida` e `origem = xml`, e publica `{eventType: "entry.uploaded", entryId, s3Key, timestamp}` na `entry-events-queue`.
- RF03 — Responde `202` com `{uploadId (= entryId), s3Key, message}`.

## Regras e requisitos não funcionais

- Mesmas regras de tamanho, XXE e chave duplicada da HU-B11.2.

## Critérios de aceite

```gherkin
Cenário: upload aceito
  Quando envio um XML válido
  Então recebo 202, o arquivo está no S3 e há uma mensagem entry.uploaded na fila
```

## Subtasks

- [ ] ST-B11.3.1 Upload no S3 com chave por fornecedor
- [ ] ST-B11.3.2 Registro `recebida` e publicação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B11.3.1 | Integração (LocalStack) | S3 + fila após upload | aceito |
| TC-B11.3.2 | API | XML inválido → 400; grande → 413 | RNF |
