# HU-B10.1 — Processar cancelamento

**Épico:** [EPB-10](README.md) · **Unidade:** `FiscalEventsListener` · **Estimativa:** 5 pts · **Depende de:** HU-B07.5, HU-B08.3, HU-B09.1

## História

Como **administradora**, quero **que o cancelamento pedido seja registrado na SEFAZ e devolva as peças ao estoque** para **a venda desfeita não deixar rastro errado**.

## Requisitos funcionais

- RF01 — Para `eventType = cancelamento`, monta o `envEvento` com `infEvento` (`tpEvento 110111`, `nSeqEvento 1`, `chNFe`, `detEvento` com `nProt` da nota e `xJust`), `Id = "ID110111{chave}01"`.
- RF02 — Assina `infEvento` e envia para `NFeRecepcaoEvento4`.
- RF03 — `cStat 135` (ou `155`, fora do prazo mas aceito): nota `status = cancelada`; evento `status = concluido`, `protocolo`; `procEventoNFe` salvo em `{tipo}/{ano}/{mes}/{chave}-cancelamento.xml`.
- RF04 — Publica `increase` por item na `inventory-updates-queue` (`referenciaType = cancellation`, `referenciaId = notaId`).
- RF05 — Nota de **devolução** cancelada: em vez de `increase`, publica `decrease` (as peças voltam a sair) e reverte `quantidadeDevolvida` na nota original.

## Critérios de aceite

```gherkin
Cenário: cancelamento homologado
  Dado um pedido de cancelamento em processamento
  Quando o mock responde cStat 135
  Então a nota fica "cancelada", o evento "concluido" com protocolo
  E um increase por item é publicado

Cenário: cancelamento de devolução
  Dado uma nota DEVOLUCAO autorizada sendo cancelada
  Quando o mock responde 135
  Então decrease é publicado e a quantidadeDevolvida da nota original volta ao valor anterior
```

## Subtasks

- [ ] ST-B10.1.1 Builder do `envEvento`
- [ ] ST-B10.1.2 Cliente `NFeRecepcaoEvento4`
- [ ] ST-B10.1.3 Efeitos pós-homologação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B10.1.1 | Unitário (snapshot XML) | evento 110111 | RF01 |
| TC-B10.1.2 | Integração (LocalStack + WireMock) | cancelamento ponta a ponta | homologado |
| TC-B10.1.3 | Integração | cancelamento de devolução | devolução |
