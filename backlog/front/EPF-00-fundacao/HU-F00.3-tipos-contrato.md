# HU-F00.3 — Tipos do contrato e conversores

**Épico:** [EPF-00](README.md) · **Estimativa:** 3 pts · **Depende de:** —

## História

Como **desenvolvedora do front**, quero **tipos iguais ao contrato do back e conversores prontos** para **o compilador apontar campos errados antes de chegar no navegador**.

## Requisitos funcionais

- RF01 — Revisar `types/index.ts` com as entidades e enums das seções 4.1 e 4.2 do mapeamento: `Cliente`, `Produto` (com `estoque` aninhado), `Venda`, `VendaItem`, `NotaResumo`, `Nota`, `NotaItem`, `EventoFiscal`, `Inutilizacao`, `Entrada`, `Dashboard`, `Pagina<T>`, `ApiError`, e os inputs (`ClienteInput`, `ProdutoInput`, `AjusteInput`, `VendaInput`, `DevolucaoInput`, `EntradaInput`, `InutilizacaoInput`, `LoteContadorInput`).
- RF02 — Incluir os campos novos do backlog: `Cliente.enderecoCompleto`, `NotaResumo.clienteDocumento`, `NotaResumo.motivoRejeicao`, `Nota.eventos[]`, `Dashboard.estoque.criticos[]`, `EntradaPrevia.itens[].cadastrado`.
- RF03 — Conversores puros em `shared/conversores.ts`: `itemCarrinhoParaVenda`, `itensDevolucaoParaPayload`, `clienteFormParaInput` (limpa máscaras), `produtoFormParaInput`, `entradaFormParaInput` (sem `id` temporário).
- RF04 — Remover campos legados (`minimo`, `custo`, `venda`, `estoque: number`, `status: "ativo"`).

## Critérios de aceite

```gherkin
Cenário: payload da venda só com o contrato
  Dado um ItemCarrinho com editandoPreco e precoTemp
  Quando converto para VendaInput
  Então o item tem apenas produtoId, quantidade e valorUnitario

Cenário: tipos batem com o back
  Quando valido as respostas gravadas (fixtures) do back contra os tipos
  Então não há divergência de campo
```

## Subtasks

- [ ] ST-F00.3.1 Revisar `types/index.ts`
- [ ] ST-F00.3.2 Conversores e testes
- [ ] ST-F00.3.3 Fixtures JSON copiadas dos testes de contrato do back

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F00.3.1 | Unitário | cada conversor | payload |
| TC-F00.3.2 | Unitário | fixtures do back tipadas sem erro (`tsc` no teste) | tipos batem |
