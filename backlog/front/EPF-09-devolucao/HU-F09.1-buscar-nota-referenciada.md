# HU-F09.1 — Buscar a nota de origem

**Épico:** [EPF-09](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B07.2

## História

Como **vendedor**, quero **encontrar a nota da compra que o cliente está devolvendo** para **vincular a devolução a ela**.

## Requisitos funcionais

- RF01 — Lista `GET /api/notas?status=autorizada&de={hoje − 90 dias}` paginada, com filtro de cliente (`clienteId`) enviado ao back. O front esconde notas `DEVOLUCAO` e `ENTRADA`, e o campo de busca filtra por número ou chave nas páginas já carregadas (o back não tem busca textual em notas).
- RF02 — `?notaId=` (vindo do painel) já seleciona a nota.
- RF03 — Remover `notasReferencia` mockadas.

## Critérios de aceite

```gherkin
Cenário: escolher nota
  Quando busco pelo número 000126
  Então vejo a nota e ao selecioná-la os itens são carregados
```

## Subtasks

- [ ] ST-F09.1.1 Busca e seleção

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F09.1.1 | Unitário | parâmetros e seleção | escolher |
