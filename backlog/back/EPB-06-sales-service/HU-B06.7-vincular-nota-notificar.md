# HU-B06.7 — Vincular nota à venda e notificar

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 2 pts · **Depende de:** HU-B06.4

## História

Como **vendedor**, quero **ver na venda qual nota foi gerada** e como **cliente**, quero **receber a confirmação da compra por e-mail**.

## Requisitos funcionais

- RF01 — Endpoint interno `PUT /internal/vendas/{id}/nota-fiscal` com `{notaFiscalId}`, chamado pelo fiscal-service ao criar a nota (HU-B07.1); não exposto no gateway.
- RF02 — Após finalizar uma venda cujo cliente tem e-mail, publica `sale_completed` no `sales-notifications-topic` (seção 7.2). Valor mínimo configurável `VENDA_NOTIFICACAO_MINIMO` (default 0).

## Critérios de aceite

```gherkin
Cenário: nota vinculada
  Dado uma venda finalizada
  Quando o fiscal-service cria a nota
  Então GET /api/vendas/{id} passa a mostrar notaFiscalId

Cenário: confirmação por e-mail
  Dado um cliente com e-mail
  Quando a venda é finalizada
  Então uma mensagem sale_completed é publicada no sales-notifications-topic
```

## Subtasks

- [ ] ST-B06.7.1 Endpoint interno
- [ ] ST-B06.7.2 Publicação no tópico de notificações

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.7.1 | API | PUT interno atualiza a venda | vinculada |
| TC-B06.7.2 | Integração | mensagem no tópico | e-mail |
