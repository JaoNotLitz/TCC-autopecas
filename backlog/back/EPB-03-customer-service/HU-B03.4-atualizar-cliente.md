# HU-B03.4 — Atualizar cliente

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 3 pts · **Depende de:** HU-B03.1

## História

Como **vendedor**, quero **completar ou corrigir o cadastro de um cliente** para **poder emitir NF-e para ele**.

## Requisitos funcionais

- RF01 — `PUT /api/clientes/{id}` com o mesmo corpo do POST; responde `200` com o cliente atualizado.
- RF02 — Mesmas validações do POST; unicidade ignora o próprio cliente.
- RF03 — Atualiza `dataAtualizacao`; não altera `totalCompras`, `valorTotalCompras`, `ultimaCompra`, `ativo` e `dataCriacao`.
- RF04 — Troca de documento atualiza o item-guarda de unicidade na mesma transação.
- RF05 — Invalida o cache do cliente e das buscas.

## Critérios de aceite

```gherkin
Cenário: completar endereço
  Dado um cliente sem endereço
  Quando envio PUT com cep, logradouro, numero, bairro, cidade e uf
  Então recebo 200 com enderecoCompleto true

Cenário: documento em uso por outro
  Quando altero o documento para o de outro cliente
  Então recebo 409

Cenário: cliente inexistente
  Então PUT em id inexistente responde 404
```

## Subtasks

- [ ] ST-B03.4.1 `ClienteService.atualizar` com transação
- [ ] ST-B03.4.2 Endpoint PUT

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.4.1 | Integração | completa endereço | completar |
| TC-B03.4.2 | Integração | troca para documento existente → 409; mesmo documento → 200 | em uso |
| TC-B03.4.3 | Unitário | campos protegidos não mudam | RF03 |
