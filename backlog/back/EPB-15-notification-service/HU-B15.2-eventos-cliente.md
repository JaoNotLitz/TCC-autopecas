# HU-B15.2 — Boas-vindas e eventos de cliente

**Épico:** [EPB-15](README.md) · **Unidade:** `CustomerEventsListener` · **Estimativa:** 2 pts · **Depende de:** HU-B15.1, HU-B03.6

## História

Como **cliente novo**, quero **receber um e-mail de boas-vindas** para **saber que meu cadastro foi feito**.

## Requisitos funcionais

- RF01 — `customer.created` com e-mail → publica `welcome` na `email-queue`; sem e-mail → nada.
- RF02 — `customer.updated` com `emailAnterior` diferente → publica confirmação para o novo e-mail (template `welcome` com flag `atualizacao`); senão só log de auditoria.
- RF03 — `customer.deactivated` → log de auditoria (notificação ao gerente fica para o futuro, como na arquitetura).

## Critérios de aceite

```gherkin
Cenário: cliente com e-mail
  Quando um cliente com e-mail é cadastrado
  Então um e-mail de boas-vindas aparece no SES local

Cenário: cadastro rápido sem e-mail
  Quando um cliente sem e-mail é cadastrado
  Então nenhum e-mail é enviado
```

## Subtasks

- [ ] ST-B15.2.1 Listener e roteamento por eventType

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B15.2.1 | Integração (Compose) | cadastro → e-mail no SES | com e-mail |
| TC-B15.2.2 | Unitário | sem e-mail não publica | sem e-mail |
