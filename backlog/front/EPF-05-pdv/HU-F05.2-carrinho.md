# HU-F05.2 — Carrinho, preço e desconto

**Épico:** [EPF-05](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F05.1

## História

Como **vendedor**, quero **ajustar quantidade, preço e desconto e ver o total na hora** para **negociar com o cliente**.

## Requisitos funcionais

- RF01 — Quantidade com +/− e digitação; mínimo 1; máximo = `disponivel` (aviso se passar).
- RF02 — Preço editável aceitando vírgula; valor inválido mantém o anterior (regra atual do PDV).
- RF03 — Remover item.
- RF04 — `subtotal`, `desconto` e `total` como `computed()`; desconto em R$, ≥ 0 e **≤ subtotal** (o back recusa total negativo).
- RF05 — Finalizar/salvar desabilitados com carrinho vazio.

## Critérios de aceite

```gherkin
Cenário: desconto maior que o subtotal
  Dado subtotal 100,00
  Quando digito desconto 150
  Então o campo mostra erro e o total não fica negativo

Cenário: quantidade acima do disponível
  Dado disponível 3
  Quando coloco 5
  Então vejo "Disponível: 3" e a quantidade volta para 3
```

## Subtasks

- [ ] ST-F05.2.1 Estado do carrinho com signals
- [ ] ST-F05.2.2 Edição de preço e quantidade

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.2.1 | Unitário | cálculos de subtotal e total | RF04 |
| TC-F05.2.2 | Unitário | limites de desconto e quantidade | cenários |
| TC-F05.2.3 | Unitário | preço com vírgula e inválido | RF02 |
