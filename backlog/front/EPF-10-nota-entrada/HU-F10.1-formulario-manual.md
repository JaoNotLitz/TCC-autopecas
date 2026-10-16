# HU-F10.1 — Formulário de entrada manual

**Épico:** [EPF-10](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F00.4

## História

Como **estoquista**, quero **um formulário que valide os dados da nota do fornecedor** para **não registrar entrada errada**.

## Requisitos funcionais

- RF01 — Fornecedor: CNPJ (máscara + validação), razão social, **IE (campo novo)**, UF (select).
- RF02 — Nota: número, série, data de emissão, data de entrada (`yyyy-MM-dd`), chave de acesso (44 dígitos com DV, exibida em grupos), natureza da operação.
- RF03 — Itens (FormArray): código, descrição, NCM (máscara), quantidade, valor unitário, CFOP (select com 1102, 1202, 1403, 1411, 2102, 2202), subtotal da linha; adicionar e remover linha.
- RF04 — Total `Σ quantidade × valorUnitario` em `computed()`.
- RF05 — Validações iguais ao back: data de entrada ≥ emissão, ao menos 1 item, quantidade e valor > 0.
- RF06 — Itens não levam o `id` temporário (`Date.now()`) para o payload.

## Critérios de aceite

```gherkin
Cenário: chave inválida
  Quando digito uma chave com DV errado
  Então o campo mostra "Chave de acesso inválida"

Cenário: datas invertidas
  Quando a data de entrada é anterior à emissão
  Então o campo data de entrada mostra o erro
```

## Subtasks

- [ ] ST-F10.1.1 Reactive Form com FormArray de itens
- [ ] ST-F10.1.2 Campo IE e validações

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F10.1.1 | Unitário | validadores de chave e datas | cenários |
| TC-F10.1.2 | Unitário | total e payload sem id temporário | RF04/RF06 |
