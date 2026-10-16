# HU-F01.3 — Sair do sistema

**Épico:** [EPF-01](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-F01.2, HU-B01.5

## História

Como **usuário**, quero **sair do sistema pelo menu** para **ninguém usar minha sessão**.

## Requisitos funcionais

- RF01 — Botão "Sair" na sidebar chama `POST /auth/logout`, limpa `sessionStorage` e navega para `/login`.
- RF02 — Se o logout falhar (rede), limpa a sessão local mesmo assim.

## Critérios de aceite

```gherkin
Cenário: sair
  Quando clico em Sair
  Então vou para o login e voltar para / me leva ao login de novo
```

## Subtasks

- [ ] ST-F01.3.1 Ação de logout no `AuthService` e na sidebar

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F01.3.1 | Unitário | logout com sucesso e com falha de rede | sair |
