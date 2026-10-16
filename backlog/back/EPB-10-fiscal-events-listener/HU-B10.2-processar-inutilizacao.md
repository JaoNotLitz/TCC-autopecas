# HU-B10.2 — Processar inutilização

**Épico:** [EPB-10](README.md) · **Unidade:** `FiscalEventsListener` · **Estimativa:** 3 pts · **Depende de:** HU-B07.6, HU-B08.3, HU-B09.1

## História

Como **administradora**, quero **que a faixa pedida seja inutilizada na SEFAZ** para **fechar os buracos na numeração**.

## Requisitos funcionais

- RF01 — Para `eventType = inutilizacao`, monta o `inutNFe` 4.00 (`ano` 2 dígitos, `CNPJ` do emitente, `mod`, `serie`, `nNFIni`, `nNFFin`, `xJust`), `Id = "ID{cUF}{ano}{CNPJ}{mod}{serie 3}{ini 9}{fin 9}"`.
- RF02 — Assina `infInut` e envia para `NFeInutilizacao4`.
- RF03 — `cStat 102`: evento `status = concluido` (`inutilizada`), `protocolo`, XML de retorno salvo em `INUTILIZACAO/{ano}/{mes}/{id}.xml`.
- RF04 — Avança o contador de numeração se a faixa inutilizada estiver acima do último número usado.

## Critérios de aceite

```gherkin
Cenário: inutilização homologada
  Dado um pedido de inutilização da faixa 100 a 105
  Quando o mock responde 102
  Então o evento fica concluido com protocolo
```

## Subtasks

- [ ] ST-B10.2.1 Builder do `inutNFe` e do `Id`
- [ ] ST-B10.2.2 Cliente `NFeInutilizacao4`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B10.2.1 | Unitário | formação do `Id` com zeros à esquerda | RF01 |
| TC-B10.2.2 | Integração | inutilização ponta a ponta | homologada |
| TC-B10.2.3 | Unitário | contador avança só quando necessário | RF04 |
