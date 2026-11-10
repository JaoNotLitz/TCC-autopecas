# HU-F01.1 — Tela de login

**Épico:** [EPF-01](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F00.2, HU-B01.1, HU-B01.2

## História

Como **usuário da loja**, quero **entrar com e-mail e senha e entender o que deu errado quando não consigo** para **começar a trabalhar**.

## Requisitos funcionais

- RF01 — Formulário com e-mail e senha (mostrar/ocultar), botão "Entrar" com `appBotaoOcupado`.
- RF02 — `POST /auth/login`; sucesso grava `token` e `refreshToken` no `sessionStorage`, guarda `usuario` no signal do `AuthService` e navega para a rota de origem (`returnUrl`) ou `/`.
- RF03 — Mensagens: `401` "E-mail ou senha inválidos"; `423` mostra a message do back (minutos restantes); `400` marca os campos; status 0 "Sem conexão com o servidor".
- RF04 — `?expirada=1` mostra aviso "Sua sessão expirou. Entre novamente".

## Regras e requisitos não funcionais

- Validação local: e-mail válido e senha obrigatória antes de enviar.
- Campo senha com `autocomplete="current-password"`; Enter envia.

## Critérios de aceite

```gherkin
Cenário: login correto
  Quando entro com o admin do seed
  Então vou para a tela inicial e vejo meu nome no menu

Cenário: conta bloqueada
  Dado 5 tentativas erradas
  Quando tento de novo
  Então vejo a mensagem de bloqueio com os minutos restantes

Cenário: voltar para onde estava
  Dado que acessei /estoque sem estar logado
  Quando faço login
  Então vou para /estoque
```

## Subtasks

- [ ] ST-F01.1.1 Reactive Form e validações
- [ ] ST-F01.1.2 Tratamento dos códigos de erro
- [ ] ST-F01.1.3 `returnUrl`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F01.1.1 | Unitário (componente) | 401, 423, 400, 0 | mensagens |
| TC-F01.1.2 | Unitário | grava tokens e navega para `returnUrl` | voltar |
| TC-F01.1.3 | E2E | login real | correto |
