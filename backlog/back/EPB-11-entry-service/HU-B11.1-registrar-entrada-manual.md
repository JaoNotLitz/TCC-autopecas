# HU-B11.1 — Registrar entrada manual

**Épico:** [EPB-11](README.md) · **Unidade:** `entry-service` · **Estimativa:** 5 pts · **Depende de:** HU-B00.3, HU-B04.2

## História

Como **estoquista**, quero **lançar a nota do fornecedor com seus itens** para **as peças entrarem no estoque**.

## Requisitos funcionais

- RF01 — `POST /api/entrada` com `{fornecedor:{cnpj, razaoSocial, ie, uf}, nota:{numero, serie, dataEmissao, dataEntrada, chaveAcesso, naturezaOperacao}, itens:[{codigo, descricao, ncm, quantidade, valorUnitario, cfop}]}`.
- RF02 — Resolve cada `codigo` no product-service (`codigo-index`). Códigos desconhecidos → `422` com `campos[{campo: "itens[i].codigo", message: "Produto não cadastrado"}]` (decisão A5).
- RF03 — Grava `entry_notes` com `status = registrada`, `valorTotal = Σ quantidade × valorUnitario`, `origem = manual`, `usuarioId`.
- RF04 — Publica um `increase` por item (`referenciaType = entry`, `referenciaId = entryId`).
- RF05 — Responde `201` com `{id, status, message: "Entrada registrada com sucesso. Estoque será atualizado."}`.

## Regras e requisitos não funcionais

- CNPJ válido; chave de acesso 44 dígitos com DV válido (validador `@ChaveAcesso`); `dataEntrada ≥ dataEmissao`; ao menos 1 item; quantidade > 0; valorUnitario > 0; NCM 8 dígitos; CFOP de entrada (`1xxx`/`2xxx`) → `400`.
- Chave de acesso já registrada → `409` (GSI `chave-index`).

## Critérios de aceite

```gherkin
Cenário: entrada válida
  Dado o produto PF-001 com estoqueAtual 10
  Quando registro uma entrada com 100 unidades de PF-001
  Então recebo 201
  E em até 5 segundos o PF-001 tem estoqueAtual 110

Cenário: produto não cadastrado
  Quando registro um item com codigo "XYZ-999" inexistente
  Então recebo 422 com campos "itens[0].codigo"

Cenário: data de entrada anterior à emissão
  Então recebo 400 com campos "nota.dataEntrada"

Cenário: chave repetida
  Dado uma entrada já registrada com a mesma chave
  Então recebo 409
```

## Subtasks

- [ ] ST-B11.1.1 Entidade `NotaEntrada` e repositório
- [ ] ST-B11.1.2 DTO `EntradaInput` com validações
- [ ] ST-B11.1.3 Resolução de produtos por código
- [ ] ST-B11.1.4 Serviço, publicação e controller
- [ ] ST-B11.1.5 Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B11.1.1 | Unitário | validações de CNPJ, chave, datas, CFOP | inválidos |
| TC-B11.1.2 | Integração (Compose) | entrada válida reflete no estoque | válida |
| TC-B11.1.3 | Unitário (WireMock) | código desconhecido → 422 | não cadastrado |
| TC-B11.1.4 | Integração | chave repetida → 409 | repetida |
