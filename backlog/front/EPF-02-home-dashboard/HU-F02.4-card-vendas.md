# HU-F02.4 — Card de vendas: buscar cliente e ir ao PDV

**Épico:** [EPF-02](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B03.2, HU-F05.3

## História

Como **vendedor**, quero **digitar o CPF/CNPJ do cliente no início e cair no PDV com ele selecionado** para **começar a venda mais rápido**.

## Requisitos funcionais

- RF01 — Campo com máscara e validação de CPF/CNPJ; "Buscar" chama `GET /api/clientes?documento=`.
- RF02 — Encontrado: mostra nome e botão "Nova venda" que navega para `/pdv?clienteId={id}`.
- RF03 — Não encontrado: oferece "Cadastrar e vender" (abre o cadastro rápido do PDV com o documento preenchido).
- RF04 — "Novo pedido/Nota fiscal" sem cliente navega para `/pdv`.

## Critérios de aceite

```gherkin
Cenário: cliente encontrado
  Quando busco um CNPJ cadastrado e clico em Nova venda
  Então o PDV abre com o cliente identificado
```

## Subtasks

- [ ] ST-F02.4.1 Busca e navegação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F02.4.1 | Unitário | encontrado e não encontrado | RF02/RF03 |
| TC-F02.4.2 | E2E | busca → PDV com cliente | encontrado |
