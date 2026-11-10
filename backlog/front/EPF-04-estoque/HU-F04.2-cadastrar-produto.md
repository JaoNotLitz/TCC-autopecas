# HU-F04.2 — Cadastrar produto

**Épico:** [EPF-04](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B04.1

## História

Como **gerente**, quero **cadastrar uma peça com os dados fiscais e o estoque que já tenho** para **vendê-la**.

## Requisitos funcionais

- RF01 — "Novo produto" abre formulário com seções Produto (código, nome, descrição, categoria, fabricante, código do fabricante), Preço e estoque (custo, venda, margem calculada só para exibição, estoque mínimo, estoque inicial) e Fiscal (NCM, CEST, CFOP, origem, CST ICMS, alíquota ICMS).
- RF02 — Selects para origem (0–8) e CFOP de saída comuns (5102, 5405, 6102); NCM com máscara.
- RF03 — `POST /api/produtos`; `409` marca o código.

## Regras e requisitos não funcionais

- Validação local: venda > custo, NCM 8 dígitos, mínimo e estoque inicial ≥ 0.

## Critérios de aceite

```gherkin
Cenário: novo produto
  Quando cadastro PF-099 com estoque inicial 30 e mínimo 10
  Então ele aparece na lista com estoque 30 e status OK

Cenário: venda menor que custo
  Então o formulário bloqueia o envio e mostra o erro no preço de venda
```

## Subtasks

- [ ] ST-F04.2.1 `ProdutoFormComponent` (compartilhado com edição)
- [ ] ST-F04.2.2 Integração com POST

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F04.2.1 | Unitário | validações e margem exibida | venda menor |
| TC-F04.2.2 | Unitário | 409 no código | RF03 |
