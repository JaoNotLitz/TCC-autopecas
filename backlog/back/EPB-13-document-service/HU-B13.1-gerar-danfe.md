# HU-B13.1 — Gerar DANFE ao autorizar

**Épico:** [EPB-13](README.md) · **Unidade:** `DanfeGeneratorListener` · **Estimativa:** 8 pts · **Depende de:** HU-B09.2

## História

Como **vendedor**, quero **o DANFE pronto assim que a nota é autorizada** para **imprimir o cupom ou enviar o PDF ao cliente**.

## Requisitos funcionais

- RF01 — Listener na `document-events-queue` processa `nota.autorizada`.
- RF02 — Lê o `nfeProc` do S3 e gera o PDF com iText 7:
  - NF-e (modelo 55): DANFE retrato A4 com cabeçalho do emitente, chave com código de barras CODE-128C, protocolo, destinatário, itens, totais e informações complementares;
  - NFC-e (modelo 65): DANFE NFC-e em bobina 80 mm com itens, total, forma de pagamento, consulta pela chave e QR Code (ZXing) do `infNFeSupl`.
- RF03 — Homologação: marca d'água "SEM VALOR FISCAL".
- RF04 — Salva em `{tipo}/{ano}/{mes}/{chave}.pdf` e grava `pdfPath` na nota.
- RF05 — Cancelada depois: o PDF existente não muda; o download mostra a nota como cancelada (carimbo gerado sob demanda na HU-B13.3).

## Regras e requisitos não funcionais

- Geração ≤ 3 s por nota com até 50 itens.
- Idempotente: se `pdfPath` já existe, ACK sem gerar de novo.

## Critérios de aceite

```gherkin
Cenário: DANFE NFC-e
  Quando uma NFC-e é autorizada
  Então em até 30 segundos existe o PDF no S3 com o QR Code legível
  E a nota tem pdfPath

Cenário: DANFE NF-e
  Quando uma NF-e é autorizada
  Então o PDF A4 tem a chave de acesso em código de barras e o protocolo
```

## Subtasks

- [ ] ST-B13.1.1 Leitura do `nfeProc` (classes JAXB compartilhadas com o fiscal)
- [ ] ST-B13.1.2 Layout DANFE NF-e (iText)
- [ ] ST-B13.1.3 Layout DANFE NFC-e com QR Code
- [ ] ST-B13.1.4 Listener e gravação no S3
- [ ] ST-B13.1.5 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B13.1.1 | Unitário | PDF NFC-e: extrai texto (PDFBox) e lê QR (ZXing) do PDF gerado | NFC-e |
| TC-B13.1.2 | Unitário | PDF NF-e: chave e protocolo presentes | NF-e |
| TC-B13.1.3 | Integração (LocalStack) | mensagem → PDF no S3 e `pdfPath` | NFC-e |
| TC-B13.1.4 | Unitário | 50 itens em ≤ 3 s | desempenho |
