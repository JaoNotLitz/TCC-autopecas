# EPB-09 — SefazIntegrationListener e simulador da SEFAZ

**Unidade entregue:** listener `SefazIntegrationListener` do `fiscal-service` (consumindo a `sefaz-queue`) e o container `sefaz-mock` (WireMock) que simula os webservices da SEFAZ localmente.
**Tipo:** listener SQS + simulador · **Sprint sugerida:** 5 · **Arquitetura:** seções 4.5.2, 6.5, 8.4 e 10

## Objetivo

Enviar o XML assinado para autorização, tratar todos os retornos (autorizada, em processamento, rejeitada, erro de comunicação) e, quando autorizada, baixar o estoque e pedir o DANFE. Como o projeto roda 100% local, a SEFAZ é um simulador com respostas determinísticas para testes (decisão A8).

## Dependências de infraestrutura

SQS `sefaz-queue` (consumidor, DLQ), `inventory-updates-queue`, `document-events-queue` (produtor) · S3 `wc-documents-bucket` · DynamoDB `notas_fiscais`, `notas_items` · container `sefaz-mock`.

## Critérios de aceite do épico

- Uma venda NFC-e do seed termina `autorizada` com protocolo, e o estoque é baixado.
- Cada cenário do simulador (100, 103/105, 2xx, timeout, 5xx) tem teste automatizado.
- Trocar para a SEFAZ de homologação real exige só variáveis de ambiente.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B09.1](HU-B09.1-simulador-sefaz.md) | Simulador da SEFAZ (WireMock) | 5 |
| [HU-B09.2](HU-B09.2-autorizacao.md) | Enviar para autorização e tratar nota autorizada | 5 |
| [HU-B09.3](HU-B09.3-consulta-recibo.md) | Consultar recibo de lote em processamento | 3 |
| [HU-B09.4](HU-B09.4-rejeicao.md) | Tratar rejeição | 2 |
| [HU-B09.5](HU-B09.5-retry-dlq.md) | Retentativas e DLQ | 3 |
| [HU-B09.6](HU-B09.6-ambiente-configuravel.md) | Ambiente e endpoints configuráveis | 2 |
