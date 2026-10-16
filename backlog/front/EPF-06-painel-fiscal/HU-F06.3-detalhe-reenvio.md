# HU-F06.3 — Detalhe da nota e reenvio de rejeitada

**Épico:** [EPF-06](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B07.3, HU-B07.4, HU-B10.3

## História

Como **responsável fiscal**, quero **abrir uma nota, ver itens, motivo de rejeição e eventos, e reenviá-la** para **resolver pendências sem sair do painel**.

## Requisitos funcionais

- RF01 — Clique na linha abre painel lateral com `GET /api/notas/{id}`: cabeçalho (tipo, número, série, chave, protocolo, datas), cliente, itens, totais, `motivoRejeicao` e `eventos[]` (cancelamento/inutilização com status e protocolo).
- RF02 — Nota rejeitada mostra "Reenviar" → `POST /api/notas/{id}/reenviar`; a linha volta a `processando` e entra na atualização automática.
- RF03 — Quando a rejeição for de cadastro do cliente, link "Editar cliente".
- RF04 — Atalhos para Cancelar (vai para `/cancelamento?notaId=`) e Devolução (`/devolucao?notaId=`) quando a nota estiver autorizada.

## Critérios de aceite

```gherkin
Cenário: reenviar
  Dado uma nota rejeitada
  Quando clico em Reenviar
  Então a nota volta a "Processando" e depois muda para o novo resultado
```

## Subtasks

- [ ] ST-F06.3.1 Painel de detalhe
- [ ] ST-F06.3.2 Ação de reenvio e atalhos

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F06.3.1 | Unitário | fixture de nota com eventos | RF01 |
| TC-F06.3.2 | Unitário | reenviar muda status local | reenviar |
