# HU-F03.1 — Listar e filtrar clientes

**Épico:** [EPF-03](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F00.5, HU-B03.2

## História

Como **vendedor**, quero **buscar clientes por nome, e-mail ou documento e filtrar por tipo e situação** para **encontrar quem procuro**.

## Requisitos funcionais

- RF01 — Lista vinda de `GET /api/clientes` com paginação "Carregar mais".
- RF02 — Filtros enviados ao back: `tipo` (Todos/PF/PJ), `busca` (debounce 400 ms, mínimo 2 caracteres), `ativo` (Ativos/Inativos/Todos, padrão Ativos), `uf`.
- RF03 — Colunas: tipo, documento formatado, nome, e-mail, telefone formatado, cidade/UF, total de compras, última compra (`dd/MM/yyyy` ou "—"), status (ativo/inativo) e ações.
- RF04 — Remover a filtragem local e os `clientesData` mockados.
- RF05 — Filtros refletidos na URL (`?tipo=PJ&busca=silva`) para o link poder ser compartilhado.

## Critérios de aceite

```gherkin
Cenário: busca por documento com máscara
  Quando digito "123.456.789-09"
  Então o front envia busca=12345678909 e mostra o cliente

Cenário: lista vazia
  Quando a busca não encontra nada
  Então vejo "Nenhum cliente encontrado" e o botão "Novo cliente"
```

## Subtasks

- [ ] ST-F03.1.1 Integração com paginação e filtros
- [ ] ST-F03.1.2 Sincronizar filtros com a URL
- [ ] ST-F03.1.3 Remover mocks

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F03.1.1 | Unitário | parâmetros enviados por filtro | busca |
| TC-F03.1.2 | Unitário | debounce e cancelamento | RF02 |
| TC-F03.1.3 | E2E | busca real | busca |
