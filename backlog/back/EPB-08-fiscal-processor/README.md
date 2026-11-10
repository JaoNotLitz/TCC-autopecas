# EPB-08 — FiscalProcessorListener

**Unidade entregue:** listener `FiscalProcessorListener` do `fiscal-service`, consumindo a `fiscal-jobs-queue` (perfil `worker-xml`, pode rodar em container separado).
**Tipo:** listener SQS · **Sprint sugerida:** 5 · **Arquitetura:** seções 4.5.1, 6.4 e 8.3

## Objetivo

Gerar o XML da NF-e (modelo 55) e NFC-e (modelo 65) no layout 4.00, calcular a chave de acesso, assinar com o certificado A1 de teste, validar contra o XSD oficial, guardar no S3 e enviar para a fila da SEFAZ.

## Dependências de infraestrutura

SQS `fiscal-jobs-queue` (consumidor) e `sefaz-queue` (produtor) · DynamoDB `notas_fiscais`, `notas_items` · S3 `wc-documents-bucket` · Secrets Manager `wc-sefaz-certificate` · emitente em `emitente.json` (HU-B00.4).

## Critérios de aceite do épico

- Para notas NFE, NFCE e DEVOLUCAO o XML gerado passa no XSD `nfe_v4.00.xsd` e a assinatura é válida.
- XML salvo em `{tipo}/{ano}/{mes}/{chaveAcesso}.xml` e mensagem publicada na `sefaz-queue`.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B08.1](HU-B08.1-gerar-xml.md) | Gerar XML layout 4.00 | 8 |
| [HU-B08.2](HU-B08.2-chave-acesso.md) | Calcular chave de acesso | 2 |
| [HU-B08.3](HU-B08.3-assinatura-digital.md) | Assinar com certificado A1 | 5 |
| [HU-B08.4](HU-B08.4-validacao-xsd.md) | Validar contra o XSD | 3 |
| [HU-B08.5](HU-B08.5-armazenar-enfileirar.md) | Salvar no S3 e enviar para a SEFAZ | 2 |
| [HU-B08.6](HU-B08.6-erros-processamento.md) | Tratar erros do processamento | 3 |
