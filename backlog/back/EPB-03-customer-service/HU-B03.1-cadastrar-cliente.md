# HU-B03.1 — Cadastrar cliente

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 5 pts · **Depende de:** HU-B00.3

## História

Como **vendedor**, quero **cadastrar um cliente PF ou PJ, inclusive só com documento e nome** para **atender rápido no balcão**.

## Requisitos funcionais

- RF01 — `POST /api/clientes` recebe o `ClienteInput` do front (`tipo`, `documento`, `nome`, `email`, `telefone`, `cidade`, `uf`, `ativo`) e responde `201` com o `Cliente` (seção 5.2 da arquitetura).
- RF02 — Obrigatórios: `tipo` (`PF`|`PJ`), `documento`, `nome`. Os demais podem vir vazios.
- RF03 — Gera `id` numérico (`IdGenerator`, recurso `clientes`), `totalCompras = 0`, `ultimaCompra = null`; `ativo` vem do corpo (default `true`). Guarda `dataCriacao`/`dataAtualizacao` e `valorTotalCompras` só internamente (não vão na resposta).
- RF04 — Documento e telefone são gravados só com dígitos (máscara removida se vier).

## Regras e requisitos não funcionais

- `documento` com 11 dígitos exige `tipo = PF` e CPF válido; 14 dígitos exige `PJ` e CNPJ válido.
- Unicidade: documento (GSI `documento-index`) e e-mail (GSI `email-index`, comparação em minúsculas) → `409` com `message` dizendo qual.
- Para evitar corrida entre a consulta e a gravação, grava também um item-guarda `DOC#{documento}` com `attribute_not_exists` numa transação (`TransactWriteItems`).
- Telefone 10 ou 11 dígitos; UF entre as 27 siglas → `400` com `campos`.
- Não há endereço, nome fantasia nem IE: o front não tem esses campos (ver seção 5.10 da arquitetura).

## Critérios de aceite

```gherkin
Cenário: cadastro rápido de PF
  Quando envio tipo PF, documento CPF válido e nome
  Então recebo 201 com id numérico, ativo true, totalCompras 0 e ultimaCompra null

Cenário: documento duplicado
  Dado um cliente com o CNPJ 12345678000195
  Quando cadastro outro com o mesmo CNPJ
  Então recebo 409 com message citando o documento

Cenário: e-mail duplicado com caixa diferente
  Dado um cliente com e-mail "a@b.com"
  Quando cadastro outro com "A@B.com"
  Então recebo 409 com message citando o e-mail

Cenário: CPF com tipo PJ
  Quando envio tipo PJ com 11 dígitos
  Então recebo 400 com campos "documento"

Cenário: UF inválida
  Quando envio uf "XX"
  Então recebo 400 com campos "uf"
```

## Subtasks

- [ ] ST-B03.1.1 Entidade `Cliente` e mapeamento DynamoDB (id `long`)
- [ ] ST-B03.1.2 DTO `ClienteInput` com validações
- [ ] ST-B03.1.3 `ClienteService.criar` com transação de unicidade
- [ ] ST-B03.1.4 `ClienteController` POST
- [ ] ST-B03.1.5 Entrada no Compose e rota no gateway

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.1.1 | Unitário | validações de documento × tipo, telefone, UF | inválidos |
| TC-B03.1.2 | Integração (LocalStack) | criar e duplicar documento → 409 | duplicado |
| TC-B03.1.3 | Integração | 10 cadastros concorrentes do mesmo documento → 1 sucesso e 9 × 409 | corrida |
| TC-B03.1.4 | Contrato | resposta valida contra o tipo `Cliente` de `src/app/types/index.ts` do front (sem campos a mais) | RF01 |
