# HU-F06.1 — Listar e filtrar notas

**Épico:** [EPF-06](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F00.5, HU-B07.2, HU-B14.1

## História

Como **responsável fiscal**, quero **ver as notas por status, tipo e período com os números do dia** para **acompanhar a emissão**.

## Requisitos funcionais

- RF01 — Lista de `GET /api/notas` paginada; colunas: número, série, tipo, cliente e documento, valor, data e hora (de `emitidaEm`), status (processando, autorizada, rejeitada, cancelada) e ações.
- RF02 — Barra de filtros: status, tipo (NFE, NFCE, DEVOLUCAO, ENTRADA), período (`de`/`ate`, padrão hoje), refletidos na URL; aceita `?status=rejeitada` vindo do início.
- RF03 — Estatísticas do topo de `dashboard.fiscal` (emitidas hoje, autorizadas, processando, rejeitadas, valor), no lugar das fixas.
- RF04 — Notas em `processando` na tela são reconsultadas a cada 5 s (enquanto a aba estiver visível) e o status muda sem recarregar.
- RF05 — Remover `notasFiscais` mockadas.

## Critérios de aceite

```gherkin
Cenário: rejeitadas
  Quando filtro Rejeitada
  Então vejo só rejeitadas, cada uma com o motivo ao passar o mouse

Cenário: nota muda de status sozinha
  Dado uma nota em processando na lista
  Quando ela é autorizada no back
  Então em até 5 segundos a linha mostra Autorizada
```

## Subtasks

- [ ] ST-F06.1.1 Lista e filtros
- [ ] ST-F06.1.2 Estatísticas do dashboard
- [ ] ST-F06.1.3 Atualização das notas em processamento

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F06.1.1 | Unitário | filtros → parâmetros | rejeitadas |
| TC-F06.1.2 | Unitário (fake timers) | atualização de processando | muda sozinha |
| TC-F06.1.3 | E2E | painel com seed | rejeitadas |
