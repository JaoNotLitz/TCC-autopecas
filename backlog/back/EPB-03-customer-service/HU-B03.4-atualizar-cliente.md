# HU-B03.4 — Atualizar cliente

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 3 pts · **Depende de:** HU-B03.1

## História

Como **vendedor**, quero **corrigir o cadastro de um cliente ou reativá-lo** para **manter os dados certos nas notas**.

## Requisitos funcionais

- RF01 — `PUT /api/clientes/{id}` com o `ClienteInput` do front; responde `200` com o `Cliente` atualizado.
- RF02 — Mesmas validações do POST; unicidade ignora o próprio cliente.
- RF03 — `ativo` vem do corpo: `true` reativa um cliente desativado. Não altera `totalCompras`, `ultimaCompra` nem `dataCriacao`.
- RF04 — Troca de documento atualiza o item-guarda de unicidade na mesma transação.
- RF05 — Invalida o cache do cliente e das buscas.

## Critérios de aceite

```gherkin
Cenário: corrigir telefone
  Dado um cliente existente
  Quando envio PUT com outro telefone
  Então recebo 200 com o telefone novo e totalCompras inalterado

Cenário: reativar
  Dado um cliente desativado
  Quando envio PUT com ativo true
  Então ele volta a aparecer como ativo na listagem

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
| TC-B03.4.1 | Integração | corrige telefone e reativa | corrigir / reativar |
| TC-B03.4.2 | Integração | troca para documento existente → 409; mesmo documento → 200 | em uso |
| TC-B03.4.3 | Unitário | `totalCompras`, `ultimaCompra` e `dataCriacao` não mudam | RF03 |
