# HU-F00.5 — Paginação por cursor ("Carregar mais")

**Épico:** [EPF-00](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F00.3

## História

Como **usuário com muitos clientes, produtos e notas**, quero **carregar a lista aos poucos** para **a tela abrir rápido**.

## Requisitos funcionais

- RF01 — Utilitário `paginacao<T>(buscar: (lastKey?) => Observable<Pagina<T>>)` baseado em signals, expondo `items`, `carregando`, `temMais`, `erro`, `carregarMais()`, `reiniciar()`.
- RF02 — Componente `<app-carregar-mais>` com botão "Carregar mais" (desabilitado durante carregamento, oculto quando `temMais` é falso).
- RF03 — Mudança de filtro chama `reiniciar()` e cancela a requisição anterior (`switchMap`).
- RF04 — Totais da tela vêm do dashboard ou `/resumo`, nunca de `count` (que é só da página).

## Critérios de aceite

```gherkin
Cenário: três páginas
  Dado 45 itens no back com limit 20
  Quando clico em "Carregar mais" duas vezes
  Então vejo 45 itens e o botão some

Cenário: filtro muda no meio do carregamento
  Quando troco o filtro enquanto a página 2 carrega
  Então a resposta antiga é descartada e a lista recomeça
```

## Subtasks

- [ ] ST-F00.5.1 Utilitário de paginação
- [ ] ST-F00.5.2 Componente `app-carregar-mais`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F00.5.1 | Unitário | três páginas | três páginas |
| TC-F00.5.2 | Unitário | troca de filtro descarta resposta antiga | filtro |
