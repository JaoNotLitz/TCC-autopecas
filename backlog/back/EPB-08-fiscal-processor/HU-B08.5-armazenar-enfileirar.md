# HU-B08.5 — Salvar no S3 e enviar para a SEFAZ

**Épico:** [EPB-08](README.md) · **Unidade:** `FiscalProcessorListener` · **Estimativa:** 2 pts · **Depende de:** HU-B08.4

## História

Como **sistema fiscal**, quero **guardar o XML assinado e enfileirar o envio** para **ter o documento arquivado e seguir o fluxo de autorização**.

## Requisitos funcionais

- RF01 — Salva em `wc-documents-bucket` com chave `{tipo}/{ano}/{mes}/{chaveAcesso}.xml`, `Content-Type: application/xml`.
- RF02 — Atualiza a nota com `xmlPath` (`s3://…`) e `chaveAcesso`.
- RF03 — Publica na `sefaz-queue` `{notaId, xmlPath, ambiente, tipoNota, tentativa: 1, timestamp}`.
- RF04 — ACK do job só depois dos três passos.

## Regras e requisitos não funcionais

- Reprocessar o mesmo job sobrescreve o objeto (bucket versionado guarda o anterior) e não duplica mensagem na `sefaz-queue` se a nota já tiver saído de `processando`.

## Critérios de aceite

```gherkin
Cenário: fluxo completo do job
  Dado uma nota em processando
  Quando o job é processado
  Então o XML está no S3 no caminho esperado
  E a nota tem xmlPath e chaveAcesso
  E existe uma mensagem na sefaz-queue para a nota
```

## Subtasks

- [ ] ST-B08.5.1 `XmlStorage` (S3)
- [ ] ST-B08.5.2 Atualização da nota e publicação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B08.5.1 | Integração (LocalStack) | job de ponta a ponta | fluxo completo |
| TC-B08.5.2 | Integração | job repetido não duplica mensagem | RNF |
