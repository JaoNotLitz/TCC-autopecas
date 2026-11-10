# HU-B11.1 — Registrar nota de entrada

**Épico:** [EPB-11](README.md) · **Unidade:** `entry-service` (+ rota interna no `fiscal-service`) · **Estimativa:** 5 pts · **Depende de:** HU-B00.3, HU-B04.2, HU-B07.1

## História

Como **estoquista**, quero **lançar a nota do fornecedor com seus itens** para **as peças entrarem no estoque**.

## Requisitos funcionais

- RF01 — `POST /api/notas/entrada` com o `EntradaInput` do front (plano): `{fornecedor:{cnpj, razaoSocial, ie, uf}, numero, serie, dataEmissao, dataEntrada, chaveAcesso, naturezaOperacao, itens:[{produtoId, codigo, descricao, ncm, cfop, quantidade, valorUnitario}]}`.
- RF02 — Item com `produtoId` usa esse produto; item com `produtoId: null` é resolvido pelo `codigo` no product-service (`codigo-index`). Código desconhecido → `422` com `message` listando os códigos e `campos[{campo: "itens[i].codigo", message: "Produto não cadastrado"}]` (decisão A5).
- RF03 — Grava `entry_notes` (`id` numérico, `status = registrada`, `valorTotal = Σ quantidade × valorUnitario`, `origem = manual`, `usuarioId`) e cria ou atualiza o fornecedor em `suppliers` (chave `cnpj`, `id` numérico).
- RF04 — Publica um `increase` por item (`referenciaType = entry`, `referenciaId = entryId`).
- RF05 — Chama `POST http://fiscal-service:8084/internal/notas/entrada` (não exposto no gateway), que grava em `notas_fiscais` uma nota `tipo = ENTRADA`, `status = autorizada`, `fornecedorId`, `valorTotal`, `emitidaEm = dataEmissao`, `chaveAcesso`, campos de cliente `null`, e os itens em `notas_items`.
- RF06 — Responde `201` com essa `NotaFiscal` (formato do front).

## Regras e requisitos não funcionais

- CNPJ válido; chave de acesso 44 dígitos com DV válido (validador `@ChaveAcesso`); `dataEntrada ≥ dataEmissao`; ao menos 1 item; quantidade > 0; valorUnitario > 0; NCM 8 dígitos; CFOP de entrada (`1xxx`/`2xxx`) → `400`.
- Chave de acesso já registrada → `409` (GSI `chave-index`).
- O `id` temporário que a tela usa nos itens novos não vem no payload.
- Se a chamada ao fiscal-service falhar, a entrada não é gravada (a chamada acontece antes de publicar os `increase`; falha → `503`).

## Critérios de aceite

```gherkin
Cenário: entrada válida
  Dado o produto PF-001 com estoqueAtual 10
  Quando registro uma entrada com 100 unidades de PF-001 e produtoId null
  Então recebo 201 com uma NotaFiscal tipo ENTRADA, status autorizada e fornecedorId preenchido
  E em até 5 segundos o PF-001 tem estoqueAtual 110
  E GET /api/notas lista a nota de entrada

Cenário: produto não cadastrado
  Quando registro um item com codigo "XYZ-999" inexistente
  Então recebo 422 com message citando "XYZ-999"

Cenário: data de entrada anterior à emissão
  Então recebo 400 com campos "dataEntrada"

Cenário: chave repetida
  Dado uma entrada já registrada com a mesma chave
  Então recebo 409
```

## Subtasks

- [ ] ST-B11.1.1 Entidades `NotaEntrada` e `Fornecedor` e repositórios
- [ ] ST-B11.1.2 DTO `EntradaInput` (plano) com validações
- [ ] ST-B11.1.3 Resolução de produtos por `produtoId` ou código
- [ ] ST-B11.1.4 Rota interna `POST /internal/notas/entrada` no fiscal-service
- [ ] ST-B11.1.5 Serviço, publicação e controller
- [ ] ST-B11.1.6 Compose e rota `/api/notas/entrada` no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B11.1.1 | Unitário | validações de CNPJ, chave, datas, CFOP | inválidos |
| TC-B11.1.2 | Integração (Compose) | entrada válida reflete no estoque e aparece em `GET /api/notas` | válida |
| TC-B11.1.3 | Unitário (WireMock) | código desconhecido → 422 | não cadastrado |
| TC-B11.1.4 | Integração | chave repetida → 409 | repetida |
| TC-B11.1.5 | Contrato | resposta valida contra `NotaFiscal` do front | RF06 |
