# HU-B07.2 — Listar notas fiscais

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 3 pts · **Depende de:** HU-B07.1

## História

Como **responsável fiscal**, quero **listar notas por status e período** para **acompanhar o que foi autorizado, cancelado ou ainda está processando**.

## Requisitos funcionais

- RF01 — `GET /api/notas` com os filtros do front (`status`, `de`, `ate` em `yyyy-MM-dd`) e os extras `tipo` e `vendaId`; responde `NotaFiscal[]` (array puro), no formato do tipo do front: `id`, `tipo`, `numero`, `serie`, `chaveAcesso`, `clienteId`, `clienteNome`, `clienteDocumento`, `fornecedorId`, `valorTotal`, `emitidaEm`, `status`, `vendaId`, `notaReferenciadaId`.
- RF02 — Inclui notas de venda (`NFE`, `NFCE`), de devolução e de entrada (gravadas pelo endpoint interno `POST /internal/notas/entrada`, chamado pelo entry-service na HU-B11.1).
- RF03 — `status` → `status-index` (SK `emitidaEm`); período sem status → `emitidaEm-index` (PK `dataDia`); `vendaId` → `venda-index`.
- RF04 — Ordem decrescente de `emitidaEm`.

## Regras e requisitos não funcionais

- `vendaId` é extra: o PDV acha a nota da venda por ele (resposta à Q1 do front).
- Enums: `tipo` maiúsculo, `status` minúsculo; valor fora do enum → `400`.
- `status = rejeitada` existe no back, mas não no tipo do front (ver seção 5.10 da arquitetura).

## Critérios de aceite

```gherkin
Cenário: notas do dia para o card do início
  Dado 6 notas emitidas hoje e 3 ontem
  Quando chamo GET /api/notas?de={hoje}&ate={hoje}
  Então recebo um array com as 6 de hoje, as mais recentes primeiro

Cenário: notas canceláveis
  Quando filtro status=autorizada
  Então todas as notas do array têm status "autorizada"

Cenário: nota da venda
  Dado a venda 126 finalizada
  Quando chamo GET /api/notas?vendaId=126
  Então recebo um array com a nota dessa venda
```

## Subtasks

- [ ] ST-B07.2.1 Consultas por índice e filtros combinados
- [ ] ST-B07.2.2 DTO `NotaFiscal` no formato do front

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.2.1 | Integração | filtros e ordem | dia |
| TC-B07.2.2 | Contrato | item valida contra o tipo `NotaFiscal` do front (sem campos a mais) | RF01 |
| TC-B07.2.3 | Unitário | enum inválido → 400 | RNF |
