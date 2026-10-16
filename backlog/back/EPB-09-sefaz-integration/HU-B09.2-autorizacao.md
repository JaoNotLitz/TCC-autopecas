# HU-B09.2 — Enviar para autorização e tratar nota autorizada

**Épico:** [EPB-09](README.md) · **Unidade:** `SefazIntegrationListener` · **Estimativa:** 5 pts · **Depende de:** HU-B08.5, HU-B09.1

## História

Como **vendedor**, quero **que a nota seja autorizada e o estoque baixado sem eu fazer nada** para **entregar o cupom ao cliente em segundos**.

## Requisitos funcionais

- RF01 — Listener (2 concorrentes, visibility 300 s) lê `{notaId, xmlPath, tentativa}` e baixa o XML do S3.
- RF02 — Monta o `enviNFe` (lote com 1 nota, `indSinc = 1`) e envia por SOAP (Apache CXF) para `NFeAutorizacao4`, com TLS mútuo usando o certificado quando o endpoint é HTTPS (desligado no mock).
- RF03 — `cStat 100`: grava `status = autorizada`, `protocolo`, `autorizadaEm`, `codigoStatus`; anexa o `protNFe` ao XML (`nfeProc`) e salva por cima no S3.
- RF04 — Após autorizar **nota de venda** (NFE/NFCE): publica `decrease` por item na `inventory-updates-queue` (`referenciaType = sale`, `referenciaId = vendaId`).
- RF05 — Após autorizar **devolução**: publica `increase` por item (`referenciaType = return`) e incrementa `quantidadeDevolvida` (e decrementa `quantidadeEmDevolucao`) nos itens da nota original.
- RF06 — Publica `{eventType: "nota.autorizada", notaId, chaveAcesso, tipo}` na `document-events-queue` (decisão A3).

## Regras e requisitos não funcionais

- Atualização da nota condicional a `status = processando` (evita processar duas vezes).
- Timeout de 60 s por requisição.

## Critérios de aceite

```gherkin
Cenário: NFC-e autorizada
  Dado uma nota NFCE com XML na sefaz-queue
  Quando o mock responde cStat 100
  Então a nota fica "autorizada" com protocolo
  E o XML no S3 passa a ser um nfeProc com protNFe
  E 1 decrease por item é publicado
  E 1 nota.autorizada é publicada na document-events-queue

Cenário: devolução autorizada
  Dado uma nota DEVOLUCAO de 1 unidade do item A
  Quando ela é autorizada
  Então 1 increase é publicado e o item A da nota original fica com quantidadeDevolvida 1
```

## Subtasks

- [ ] ST-B09.2.1 Cliente SOAP (CXF) gerado do WSDL
- [ ] ST-B09.2.2 Montagem do `enviNFe` e parse do `retEnviNFe`
- [ ] ST-B09.2.3 `nfeProc` e regravação no S3
- [ ] ST-B09.2.4 Efeitos pós-autorização (estoque, devolução, documento)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B09.2.1 | Integração (LocalStack + WireMock) | fluxo NFC-e autorizada | NFC-e |
| TC-B09.2.2 | Integração | devolução autorizada | devolução |
| TC-B09.2.3 | Unitário | parse do `retEnviNFe` 100 | RF03 |
| TC-B09.2.4 | Integração | mensagem repetida não publica decrease duas vezes | RNF |
