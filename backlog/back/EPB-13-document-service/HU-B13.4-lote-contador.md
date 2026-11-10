# HU-B13.4 — Lote de XMLs para o contador

**Épico:** [EPB-13](README.md) · **Unidade:** `document-service` · **Estimativa:** 5 pts · **Depende de:** HU-B13.2, HU-B15.1

## História

Como **dona da loja**, quero **mandar ao contador os XMLs do mês com um clique** para **fechar a contabilidade sem baixar nota por nota**.

## Requisitos funcionais

- RF01 — `POST /api/notas/lote-contador`, **sem corpo** (como o `notasService.enviarLoteContador()` do front); responde `204`.
- RF02 — Seleciona as notas `autorizada` e `cancelada` do mês corrente (fuso `America/Sao_Paulo`, `emitidaEm-index`) ainda não enviadas (`enviadaContadorEm` vazio).
- RF03 — Monta um ZIP com os XMLs (`nfeProc` e `procEventoNFe` de cancelamento) e um `resumo.csv` (número, série, chave, tipo, status, valor) e salva em `wc-reports-bucket` (`lotes/{ano}/{mes}/{loteId}.zip`).
- RF04 — Publica na `email-queue` o template `contador-lote` com link pré-assinado (7 dias) para o e-mail do contador, lido da configuração `CONTADOR_EMAIL`.
- RF05 — Marca as notas do lote com `enviadaContadorEm`.

## Regras e requisitos não funcionais

- Nenhuma nota pendente no mês → `409` "Nenhuma nota pendente de envio neste mês".
- `CONTADOR_EMAIL` ausente → `500` com message "E-mail do contador não configurado" (erro de configuração, logado).
- Máximo de 2.000 notas por lote; acima disso o lote é dividido em partes e um e-mail é enviado por parte.

## Critérios de aceite

```gherkin
Cenário: lote do mês
  Dado 25 notas autorizadas no mês corrente e CONTADOR_EMAIL configurado
  Quando chamo POST /api/notas/lote-contador sem corpo
  Então recebo 204
  E existe um e-mail contador-lote na email-queue com link para um ZIP de 25 XMLs e o resumo.csv

Cenário: segundo envio no mesmo mês
  Dado que o lote do mês já foi enviado e não houve nota nova
  Quando chamo de novo
  Então recebo 409
```

## Subtasks

- [ ] ST-B13.4.1 Seleção das notas pendentes do mês
- [ ] ST-B13.4.2 Geração do ZIP em streaming
- [ ] ST-B13.4.3 URL pré-assinada, e-mail e marcação de envio

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B13.4.1 | Integração (LocalStack) | lote do mês, abre o ZIP e confere | lote do mês |
| TC-B13.4.2 | Integração | segundo envio sem nota nova → 409 | segundo envio |
| TC-B13.4.3 | Unitário | divisão acima de 2.000 notas | RNF |
