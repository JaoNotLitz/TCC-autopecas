# EPB-13 — document-service

**Unidade entregue:** microsserviço `document-service` (API REST, porta 8086) com o listener `DanfeGeneratorListener` na `document-events-queue`.
**Tipo:** API + listener · **Sprint sugerida:** 7 · **Arquitetura:** seções 4.7 e 5.7

## Objetivo

Gerar o DANFE (PDF) das notas autorizadas, entregar XML e PDF para download e montar o lote de XMLs para o contador.

## Dependências de infraestrutura

S3 `wc-documents-bucket` (leitura/escrita), `wc-reports-bucket` (lotes) · DynamoDB `notas_fiscais` (leitura, atualização de `pdfPath`) · SQS `document-events-queue` (consumidor, decisão A3), `email-queue` (produtor).

## Critérios de aceite do épico

- Toda nota autorizada tem PDF em até 30 s.
- Downloads funcionam no navegador pelo gateway (ou por URL pré-assinada do LocalStack).

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B13.1](HU-B13.1-gerar-danfe.md) | Gerar DANFE ao autorizar | 8 |
| [HU-B13.2](HU-B13.2-download-xml.md) | Download do XML | 2 |
| [HU-B13.3](HU-B13.3-download-pdf.md) | Download do PDF | 2 |
| [HU-B13.4](HU-B13.4-lote-contador.md) | Lote de XMLs para o contador | 5 |
