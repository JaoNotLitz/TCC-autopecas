# HU-B11.4 — Listar e detalhar entradas (extra)

> **Extra.** A tela atual vê as entradas em `GET /api/notas` (tipo `ENTRADA`). Estes endpoints mostram o status interno da importação.

**Épico:** [EPB-11](README.md) · **Unidade:** `entry-service` · **Estimativa:** 2 pts · **Depende de:** HU-B11.1

## História

Como **estoquista**, quero **ver as entradas registradas e seu status** para **conferir o que já foi lançado e o que ficou com pendência**.

## Requisitos funcionais

- RF01 — `GET /api/notas/entrada` com `fornecedorCnpj`, `de`, `ate`, `status`; array puro com resumo (`id, fornecedor.razaoSocial, fornecedor.cnpj, numero, dataEntrada, valorTotal, status, origem, quantidadeItens`).
- RF02 — `GET /api/notas/entrada/{id}` com todos os dados e itens (incluindo `produtoId` e `vinculado` por item, e `erro` quando `status = erro`).
- RF03 — Status possíveis: `recebida`, `registrada`, `com_pendencias`, `erro`.

## Critérios de aceite

```gherkin
Cenário: listar por fornecedor
  Dado 3 entradas do fornecedor 12345678000195
  Quando filtro fornecedorCnpj=12345678000195
  Então recebo um array com as 3
```

## Subtasks

- [ ] ST-B11.4.1 Consultas por índice e endpoint de detalhe

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B11.4.1 | Integração | filtros | fornecedor |
| TC-B11.4.2 | API | detalhe e 404 | RF02 |
