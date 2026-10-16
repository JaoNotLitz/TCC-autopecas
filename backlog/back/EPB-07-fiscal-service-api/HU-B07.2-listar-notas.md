# HU-B07.2 — Listar notas fiscais

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 3 pts · **Depende de:** HU-B07.1

## História

Como **responsável fiscal**, quero **listar notas por status, tipo e período** para **acompanhar o que foi autorizado, rejeitado ou ainda está processando**.

## Requisitos funcionais

- RF01 — `GET /api/notas` com `status`, `tipo`, `de`, `ate`, `clienteId`, `vendaId`, `limit`, `lastKey`; envelope padrão; itens no formato resumido da seção 5.5 (`cliente` = nome ou "Consumidor Final").
- RF02 — Inclui `clienteDocumento` (melhoria pedida na seção 7.2.2 do mapeamento) e `motivoRejeicao` quando rejeitada.
- RF03 — `status` → `status-index` (SK `emitidaEm`); período sem status → `emitidaEm-index` (PK `dataDia`); `vendaId` → `venda-index`.
- RF04 — Ordem decrescente de `emitidaEm`.

## Regras e requisitos não funcionais

- `vendaId` não está na arquitetura; foi adicionado para o PDV acompanhar a nota da venda (HU-F05.7).
- Enums: `tipo` maiúsculo, `status` minúsculo; valor fora do enum → `400`.

## Critérios de aceite

```gherkin
Cenário: notas do dia para o card do início
  Dado 6 notas emitidas hoje
  Quando chamo GET /api/notas?de={hoje}&limit=4
  Então recebo 4 notas, as mais recentes primeiro, e um lastKey

Cenário: rejeitadas
  Quando filtro status=rejeitada
  Então cada item traz motivoRejeicao
```

## Subtasks

- [ ] ST-B07.2.1 Consultas por índice e filtros combinados
- [ ] ST-B07.2.2 DTO resumido

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.2.1 | Integração | filtros e ordem | dia |
| TC-B07.2.2 | Contrato | item valida contra schema `NotaResumo` do front | RF01 |
| TC-B07.2.3 | Unitário | enum inválido → 400 | RNF |
