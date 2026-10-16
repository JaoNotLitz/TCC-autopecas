# HU-B07.1 — Criar nota a partir da venda

**Épico:** [EPB-07](README.md) · **Unidade:** `SalesEventListener` no `fiscal-service` · **Estimativa:** 8 pts · **Depende de:** HU-B06.4

## História

Como **vendedor**, quero **que a nota da venda seja criada automaticamente** para **o cliente sair com o documento fiscal sem eu emitir à mão**.

## Requisitos funcionais

- RF01 — Listener na `sales-events-queue` (5 concorrentes) processa `sale.created`.
- RF02 — Busca a venda e os itens (`GET http://sales-service:8083/api/vendas/{id}`), o cliente quando houver (`customer-service`) e os dados fiscais de cada produto (`product-service`: `ncm`, `cfop`, `cest`, `origem`, `cstIcms`, `aliquotaIcms`).
- RF03 — Gera o número com contador atômico por modelo e série (`counters`: `NFE-55-1`, `NFCE-65-1`).
- RF04 — Grava `notas_fiscais` com `status = processando`, `tipo` (`NFE`/`NFCE`), `modelo` (`55`/`65`), `serie`, `numero` com 6 dígitos, `vendaId`, `clienteId`, `clienteNome` (ou "Consumidor Final"), `valorProdutos`, `valorDesconto`, `valorTotal`, `emitidaEm`, `ambiente`, `tentativasEnvio = 0`.
- RF05 — Grava `notas_items` (cópia dos itens com dados fiscais, `itemId` próprio da nota e `quantidadeDevolvida = 0`).
- RF06 — Chama o endpoint interno do sales-service para vincular `notaFiscalId` (HU-B06.7).
- RF07 — Publica `{jobType: "generate_xml", notaId, tipoNota, timestamp}` na `fiscal-jobs-queue`.

## Regras e requisitos não funcionais

- Idempotência: antes de criar, consulta `venda-index`; se já existe nota para a venda, apenas republica o job se a nota ainda estiver sem XML e dá ACK.
- Venda não encontrada → DLQ após 3 tentativas.
- Número da nota nunca é reaproveitado (falhas depois de gerar o número deixam lacuna, que pode ser inutilizada — HU-B07.6).

## Critérios de aceite

```gherkin
Cenário: venda NFC-e vira nota
  Dado uma venda NFC-e finalizada com 2 itens
  Quando a mensagem sale.created é processada
  Então existe uma nota NFCE modelo 65 em "processando" com 2 itens e numero "000001"
  E a venda passa a ter notaFiscalId
  E um job generate_xml é publicado

Cenário: mensagem repetida
  Quando a mesma sale.created chega duas vezes
  Então existe apenas uma nota para a venda

Cenário: numeração independente por modelo
  Dado a última NFC-e 000010 e a última NF-e 000003
  Quando uma venda NF-e é processada
  Então a nova nota recebe numero "000004"
```

## Subtasks

- [ ] ST-B07.1.1 Entidades `NotaFiscal`, `NotaFiscalItem`
- [ ] ST-B07.1.2 Clientes HTTP de sales, customer e product
- [ ] ST-B07.1.3 `NumeradorNota` por modelo/série
- [ ] ST-B07.1.4 `CriarNotaDeVendaService` com idempotência
- [ ] ST-B07.1.5 Listener e publicação do job
- [ ] ST-B07.1.6 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.1.1 | Unitário | montagem da nota e itens a partir de venda + produtos | NFC-e |
| TC-B07.1.2 | Integração (LocalStack + WireMock) | processa a mensagem e confere tabelas e fila | NFC-e |
| TC-B07.1.3 | Integração | duplicada | repetida |
| TC-B07.1.4 | Integração | numeração por modelo com 20 vendas paralelas | numeração |
