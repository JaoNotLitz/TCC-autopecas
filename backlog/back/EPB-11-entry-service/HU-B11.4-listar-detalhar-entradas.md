# HU-B11.4 — Listar e detalhar entradas

**Épico:** [EPB-11](README.md) · **Unidade:** `entry-service` · **Estimativa:** 2 pts · **Depende de:** HU-B11.1

## História

Como **estoquista**, quero **ver as entradas registradas e seu status** para **conferir o que já foi lançado e o que ficou com pendência**.

## Requisitos funcionais

- RF01 — `GET /api/entrada` com `fornecedorCnpj`, `de`, `ate`, `status`, `limit`, `lastKey`; envelope padrão com resumo (`id, fornecedor.razaoSocial, fornecedor.cnpj, nota.numero, nota.dataEntrada, valorTotal, status, origem, quantidadeItens`).
- RF02 — `GET /api/entrada/{id}` com todos os dados e itens (incluindo `produtoId` e `vinculado` por item, e `erro` quando `status = erro`).
- RF03 — Status possíveis: `recebida`, `registrada`, `com_pendencias`, `erro`.

## Critérios de aceite

```gherkin
Cenário: listar por fornecedor
  Dado 3 entradas do fornecedor 12345678000195
  Quando filtro fornecedorCnpj=12345678000195
  Então recebo as 3
```

## Subtasks

- [ ] ST-B11.4.1 Consultas por índice e endpoint de detalhe

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B11.4.1 | Integração | filtros | fornecedor |
| TC-B11.4.2 | API | detalhe e 404 | RF02 |
