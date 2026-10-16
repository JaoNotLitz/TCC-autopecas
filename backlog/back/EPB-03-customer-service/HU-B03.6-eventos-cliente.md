# HU-B03.6 — Publicar eventos de cliente

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 2 pts · **Depende de:** HU-B03.1, HU-B03.4, HU-B03.5

## História

Como **notification-service**, quero **ser avisado quando um cliente é criado, alterado ou desativado** para **enviar o e-mail de boas-vindas e registrar auditoria**.

## Requisitos funcionais

- RF01 — Após gravar com sucesso, publica na `customer-events-queue` a mensagem da seção 6.1: `{eventType, customerId, timestamp, data:{id, nome, email, documento}}`.
- RF02 — `eventType`: `customer.created`, `customer.updated` (inclui `emailAnterior` quando o e-mail mudou), `customer.deactivated`.
- RF03 — Atributo de mensagem `correlationId`.

## Regras e requisitos não funcionais

- Falha ao publicar não desfaz a gravação: o erro é logado e o evento vai para uma tabela/registro de pendências reenviado por um job a cada 1 min (padrão outbox simplificado).

## Critérios de aceite

```gherkin
Cenário: evento de criação
  Quando cadastro um cliente
  Então chega uma mensagem customer.created na customer-events-queue com o id do cliente

Cenário: SQS fora do ar
  Dado a fila indisponível no momento da gravação
  Quando cadastro um cliente
  Então recebo 201
  E o evento é publicado quando a fila volta
```

## Subtasks

- [ ] ST-B03.6.1 `ClienteEventPublisher`
- [ ] ST-B03.6.2 Outbox simples (atributo `eventosPendentes` + job agendado)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.6.1 | Integração (LocalStack) | lê a fila após POST, PUT e DELETE | eventos |
| TC-B03.6.2 | Integração | simula falha do SQS e verifica reenvio | fora do ar |
| TC-B03.6.3 | Contrato | mensagem valida contra o schema `customer-event` | RF01 |
