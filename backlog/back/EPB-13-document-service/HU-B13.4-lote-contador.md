# HU-B13.4 — Lote de XMLs para o contador

**Épico:** [EPB-13](README.md) · **Unidade:** `document-service` · **Estimativa:** 5 pts · **Depende de:** HU-B13.2, HU-B15.1

## História

Como **dona da loja**, quero **mandar ao contador os XMLs do mês num arquivo só** para **fechar a contabilidade sem baixar nota por nota**.

## Requisitos funcionais

- RF01 — `POST /api/documentos/lote-contador` com `{notaIds?: [], de?, ate?, emailContador}`. Se `notaIds` vier vazio, usa todas as notas autorizadas e canceladas do período (`emitidaEm-index`).
- RF02 — Monta um ZIP com os XMLs (`nfeProc` e `procEventoNFe` de cancelamento) e um `resumo.csv` (número, série, chave, tipo, status, valor).
- RF03 — Salva em `wc-reports-bucket` (`lotes/{ano}/{mes}/{loteId}.zip`) e devolve `{loteId, arquivos, tamanho, downloadUrl (pré-assinada, 1 h), expiresIn: 3600}`.
- RF04 — Publica na `email-queue` o template `contador-lote` com o link para `emailContador`.

## Regras e requisitos não funcionais

- Máximo de 2.000 notas por lote → acima disso `422`.
- Notas não autorizadas/canceladas na lista são ignoradas e contadas em `ignoradas`.
- `emailContador` obrigatório e válido.

## Critérios de aceite

```gherkin
Cenário: lote do mês
  Dado 25 notas autorizadas em setembro
  Quando peço o lote de 01/09 a 30/09 para "contador@exemplo.com.br"
  Então recebo arquivos 25 e um downloadUrl que baixa um ZIP com 25 XMLs e o resumo.csv
  E existe um e-mail contador-lote na email-queue

Cenário: e-mail inválido
  Então recebo 400 com campos "emailContador"
```

## Subtasks

- [ ] ST-B13.4.1 Seleção de notas por ids ou período
- [ ] ST-B13.4.2 Geração do ZIP em streaming
- [ ] ST-B13.4.3 URL pré-assinada e publicação do e-mail

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B13.4.1 | Integração (LocalStack) | lote por período, abre o ZIP e confere | lote do mês |
| TC-B13.4.2 | API | e-mail inválido → 400 | inválido |
| TC-B13.4.3 | Unitário | notas ignoradas contadas | RNF |
