# HU-B06.1 — Criar venda ou orçamento

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 5 pts · **Depende de:** HU-B00.3

## História

Como **vendedor no PDV**, quero **registrar a venda com itens, desconto e forma de pagamento, ou salvar como orçamento**, para **concluir o atendimento ou retomá-lo depois**.

## Requisitos funcionais

- RF01 — `POST /api/vendas` com o corpo da seção 5.4 (`tipoDocumento`, `modalidade`, `clienteId`, `metodoPagamento`, `bandeira`, `destinoDinheiro`, `desconto`, `status`, `itens[{produtoId, quantidade, valorUnitario}]`).
- RF02 — Calcula `subtotal = Σ quantidade × valorUnitario`, `total = subtotal − desconto` (BigDecimal, 2 casas, HALF_EVEN).
- RF03 — Gera `numero` sequencial `VD-000001` com contador atômico na tabela `counters` (`UpdateItem ADD`).
- RF04 — Grava `sales` (com `vendedorId` do token, `criadaEm`, e `finalizadaEm` se `status = finalizada`) e `sale_items` (com `codigo` e `nome` copiados do produto) numa transação.
- RF05 — Responde `201` com a venda e os itens.

## Regras e requisitos não funcionais

- Pelo menos 1 item; `quantidade` inteira ≥ 1; `valorUnitario > 0`.
- `desconto ≥ 0` e `≤ subtotal` (o front original permitia total negativo; o back passa a barrar) → `400`.
- `metodoPagamento` em `dinheiro|credito|debito|pix|boleto`; `bandeira` obrigatória se crédito/débito e proibida nos outros; `destinoDinheiro` (`banco|cofre`) obrigatório se dinheiro.
- `status` inicial só `orcamento` ou `finalizada`.
- Produto inativo ou inexistente → `422` com `campos[].campo = "itens[i].produtoId"`.

## Critérios de aceite

```gherkin
Cenário: venda NFC-e à vista
  Quando envio uma venda nfce, crédito visa, 2 × 89,90, desconto 0, status finalizada
  Então recebo 201 com numero "VD-…", subtotal 179,80 e total 179,80

Cenário: desconto maior que o subtotal
  Quando envio desconto 200 para subtotal 179,80
  Então recebo 400 com campos "desconto"

Cenário: cartão sem bandeira
  Quando envio metodoPagamento credito sem bandeira
  Então recebo 400 com campos "bandeira"

Cenário: dinheiro sem destino
  Quando envio metodoPagamento dinheiro sem destinoDinheiro
  Então recebo 400 com campos "destinoDinheiro"

Cenário: orçamento
  Quando envio status orcamento
  Então recebo 201 com status "orcamento" e sem finalizadaEm
```

## Subtasks

- [ ] ST-B06.1.1 Entidades `Venda`, `VendaItem`
- [ ] ST-B06.1.2 `CalculadoraVenda`
- [ ] ST-B06.1.3 Contador sequencial
- [ ] ST-B06.1.4 Validações de pagamento (validator de classe)
- [ ] ST-B06.1.5 `VendaService.criar` e controller
- [ ] ST-B06.1.6 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.1.1 | Unitário | cálculos com arredondamento | NFC-e |
| TC-B06.1.2 | Unitário (parametrizado) | combinações de método × bandeira × destino | pagamento |
| TC-B06.1.3 | Unitário | desconto > subtotal | desconto |
| TC-B06.1.4 | Integração | 20 criações paralelas geram números únicos e sequenciais | RF03 |
| TC-B06.1.5 | Contrato | resposta valida contra o schema `Venda` do front | RF05 |
