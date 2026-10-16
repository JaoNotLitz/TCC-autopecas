# HU-B01.2 — Bloqueio após 5 tentativas falhas

**Épico:** [EPB-01](README.md) · **Unidade:** `auth-service` · **Estimativa:** 3 pts · **Depende de:** HU-B01.1

## História

Como **administradora**, quero **que uma conta seja bloqueada temporariamente após várias senhas erradas** para **dificultar ataques de força bruta**.

## Requisitos funcionais

- RF01 — Cada senha errada incrementa `tentativasFalhas` do usuário (contador no Redis `login-fail:{userId}`, TTL 15 min).
- RF02 — Na 5ª falha, grava `bloqueadoAte = agora + 15 min` e responde `423 Locked`.
- RF03 — Enquanto `bloqueadoAte` estiver no futuro, qualquer login (mesmo com senha certa) responde `423` com message "Usuário bloqueado temporariamente. Tente novamente em N minutos".
- RF04 — Login bem-sucedido zera o contador.

## Regras e requisitos não funcionais

- Tentativas para e-mail inexistente não criam registro (não há usuário), mas contam no rate limit por IP do gateway (EPB-02).

## Critérios de aceite

```gherkin
Cenário: quinta tentativa bloqueia
  Dado um usuário com 4 falhas registradas
  Quando erro a senha pela 5ª vez
  Então recebo 423

Cenário: senha certa durante o bloqueio
  Dado um usuário bloqueado há 5 minutos
  Quando faço login com a senha correta
  Então recebo 423 com os minutos restantes na message

Cenário: bloqueio expira
  Dado um usuário bloqueado há 16 minutos
  Quando faço login com a senha correta
  Então recebo 200 e o contador volta a 0
```

## Subtasks

- [ ] ST-B01.2.1 Contador de falhas no Redis
- [ ] ST-B01.2.2 Campo `bloqueadoAte` e checagem antes da senha
- [ ] ST-B01.2.3 `UsuarioBloqueadoException` → 423 no handler

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B01.2.1 | Unitário | 5 falhas seguidas → 423 na 5ª | quinta tentativa |
| TC-B01.2.2 | Unitário (relógio fixo com `Clock`) | senha certa durante bloqueio → 423 | durante bloqueio |
| TC-B01.2.3 | Unitário (relógio avançado) | após 15 min → 200 e contador zerado | expira |
| TC-B01.2.4 | Integração (Redis) | TTL do contador é 15 min | RF01 |
