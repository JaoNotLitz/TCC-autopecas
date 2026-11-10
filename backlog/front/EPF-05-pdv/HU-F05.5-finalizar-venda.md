# HU-F05.5 — Finalizar venda

**Épico:** [EPF-05](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F05.1 a HU-F05.4, HU-B06.1 a HU-B06.4

## História

Como **vendedor**, quero **finalizar a venda e saber na hora se algo impede** para **resolver com o cliente ainda no balcão**.

## Requisitos funcionais

- RF01 — "Finalizar venda" monta o `VendaInput` com `status: "finalizada"` usando `itemCarrinhoParaVenda` e envia `POST /api/vendas` (botão com `appBotaoOcupado`).
- RF02 — `201`: mostra resumo (número da venda, total) e passa para o acompanhamento da nota (HU-F05.7); limpa o carrinho.
- RF03 — `409` (estoque): marca as linhas do carrinho indicadas em `campos` com "Disponível: N" e oferece "Ajustar para o disponível".
- RF04 — `422` (NF-e sem endereço): lista os campos faltantes e oferece "Completar cadastro" (abre edição do cliente com foco no endereço, HU-F03.3); ao voltar, permite reenviar.
- RF05 — `400`: mostra os erros nos campos de pagamento/desconto.

## Critérios de aceite

```gherkin
Cenário: venda NFC-e feliz
  Dado um carrinho com 2 itens e pagamento em pix
  Quando finalizo
  Então vejo "Venda VD-… registrada" e o acompanhamento da nota

Cenário: estoque acabou no meio
  Dado que outro caixa vendeu a última unidade
  Quando finalizo
  Então a linha do item mostra "Disponível: 0" e a venda não é criada

Cenário: NF-e sem endereço
  Quando finalizo uma NF-e para cliente sem bairro
  Então vejo os campos faltantes e o atalho para completar
```

## Subtasks

- [ ] ST-F05.5.1 Montagem do payload e envio
- [ ] ST-F05.5.2 Tratamento de 409, 422 e 400 no carrinho
- [ ] ST-F05.5.3 Tela de sucesso

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.5.1 | Unitário | payload exato enviado | feliz |
| TC-F05.5.2 | Unitário | 409 marca linhas | estoque |
| TC-F05.5.3 | Unitário | 422 lista campos | NF-e |
| TC-F05.5.4 | E2E | venda real NFC-e | feliz |
