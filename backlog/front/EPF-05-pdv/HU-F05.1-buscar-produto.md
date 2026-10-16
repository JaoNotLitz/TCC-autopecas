# HU-F05.1 — Buscar e adicionar produto

**Épico:** [EPF-05](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B04.2

## História

Como **vendedor no balcão**, quero **achar a peça pelo código ou nome e colocá-la no carrinho** para **montar a venda rápido**.

## Requisitos funcionais

- RF01 — Campo de busca com autocomplete (`GET /api/produtos?busca=…&ativo=true&limit=10`, debounce 300 ms).
- RF02 — Cada sugestão mostra código, nome, preço de venda e disponível; produtos com `disponivel = 0` aparecem desabilitados com "Sem estoque".
- RF03 — Enter com código exato adiciona direto (fluxo de leitor de código de barras).
- RF04 — Adicionar produto já no carrinho soma 1 na quantidade.
- RF05 — Item do carrinho guarda `produtoId`, `codigo`, `nome`, `valorUnitario = precoVenda`, `disponivel`.
- RF06 — Remover o carrinho inicial mockado.

## Critérios de aceite

```gherkin
Cenário: leitor de código
  Quando digito "PF-001" e aperto Enter
  Então o produto entra no carrinho com quantidade 1

Cenário: produto sem estoque
  Então a sugestão aparece desabilitada
```

## Subtasks

- [ ] ST-F05.1.1 Autocomplete acessível (setas, Enter, Esc)
- [ ] ST-F05.1.2 Adição ao carrinho (signal)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.1.1 | Unitário | Enter com código exato | leitor |
| TC-F05.1.2 | Unitário | soma quantidade de item repetido | RF04 |
| TC-F05.1.3 | Unitário | sem estoque desabilitado | sem estoque |
