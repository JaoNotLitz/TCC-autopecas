# HU-F00.6 — Toasts, confirmação e estados de tela

**Épico:** [EPF-00](README.md) · **Estimativa:** 3 pts · **Depende de:** —

## História

Como **usuário**, quero **saber quando algo deu certo, deu errado ou está carregando, e confirmar ações destrutivas** para **não ficar em dúvida nem errar sem querer**.

## Requisitos funcionais

- RF01 — `ToastService` + `<app-toasts>` (sucesso, erro, aviso; some em 5 s; erro fica até fechar), acessível (`role="status"`/`alert`).
- RF02 — `ConfirmService.confirmar({titulo, mensagem, textoConfirmar, perigo})` retornando `Promise<boolean>`, com diálogo modal (foco preso, `Esc` cancela).
- RF03 — Componentes `<app-carregando>` (skeleton de tabela) e `<app-vazio>` (mensagem e ação).
- RF04 — Diretiva `appBotaoOcupado` que desabilita o botão e mostra spinner enquanto uma Promise/Observable está pendente (evita duplo clique em "Finalizar venda").
- RF05 — Trocar todos os `alert()` existentes.

## Critérios de aceite

```gherkin
Cenário: duplo clique
  Quando clico duas vezes rápido em um botão com appBotaoOcupado
  Então só uma requisição é feita

Cenário: confirmação
  Quando a ação pede confirmação e eu aperto Esc
  Então nada é executado
```

## Subtasks

- [ ] ST-F00.6.1 Toasts
- [ ] ST-F00.6.2 Diálogo de confirmação
- [ ] ST-F00.6.3 Carregando, vazio e botão ocupado
- [ ] ST-F00.6.4 Remover `alert()`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F00.6.1 | Unitário | botão ocupado ignora segundo clique | duplo clique |
| TC-F00.6.2 | Unitário | Esc cancela | confirmação |
| TC-F00.6.3 | Lint | regra `no-alert` no ESLint | RF05 |
