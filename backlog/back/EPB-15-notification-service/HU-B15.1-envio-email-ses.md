# HU-B15.1 — Enviar e-mail por template via SES

**Épico:** [EPB-15](README.md) · **Unidade:** `EmailSenderListener` · **Estimativa:** 5 pts · **Depende de:** HU-B00.2

## História

Como **sistema**, quero **um único lugar que transforma pedidos de e-mail em mensagens HTML** para **todos os serviços avisarem pessoas do mesmo jeito**.

## Requisitos funcionais

- RF01 — Listener na `email-queue` (5 concorrentes) recebe `{to, cc, subject, template, data, priority}`.
- RF02 — Templates Thymeleaf em `resources/email-templates/`: `welcome`, `sale-confirmation`, `low-stock-alert`, `fiscal-error`, `contador-lote`, com CSS inline.
- RF03 — Envia via SES `SendEmail` de `noreply@wcautopecas.com.br`.
- RF04 — Registra log de envio (`to`, `template`, `messageId` do SES, `correlationId`) para auditoria.

## Regras e requisitos não funcionais

- E-mail de destino inválido → log e ACK (sem retry).
- Erro do SES → retry até 3 vezes (visibility 30 s); depois descarta com log `ERROR` (fila sem DLQ, conforme 6.8).
- Template inexistente → log `ERROR` e ACK.

## Critérios de aceite

```gherkin
Cenário: boas-vindas
  Quando chega na email-queue um pedido template "welcome" para "joao@email.com"
  Então o e-mail aparece em /_aws/ses com assunto e corpo renderizados com o nome

Cenário: destino inválido
  Quando o "to" é "joao@"
  Então nada é enviado e a mensagem é removida da fila
```

## Subtasks

- [ ] ST-B15.1.1 Configuração Thymeleaf e templates
- [ ] ST-B15.1.2 `EmailSender` (SES)
- [ ] ST-B15.1.3 Listener e tratamento de erros
- [ ] ST-B15.1.4 Compose

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B15.1.1 | Unitário | cada template renderiza com dados de exemplo | RF02 |
| TC-B15.1.2 | Integração (LocalStack) | envia e consulta `/_aws/ses` | boas-vindas |
| TC-B15.1.3 | Unitário | destino inválido | inválido |
