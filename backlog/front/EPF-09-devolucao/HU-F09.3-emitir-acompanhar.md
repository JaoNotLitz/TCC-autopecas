# HU-F09.3 — Emitir e acompanhar a devolução

**Épico:** [EPF-09](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F09.2, HU-B07.7, HU-B09.2

## História

Como **vendedor**, quero **emitir a nota de devolução e ver quando foi autorizada** para **liberar o cliente e saber que o estoque voltou**.

## Requisitos funcionais

- RF01 — "Emitir devolução" envia `POST /api/notas/devolucao` com `itensDevolucaoParaPayload` (só `itemId` e `quantidadeDevolver` > 0).
- RF02 — Acompanha a nova nota (`GET /api/notas/{id}` a cada 3 s, até 60 s); autorizada mostra número, chave e botões PDF/XML.
- RF03 — `422` por item marca a linha com a message.
- RF04 — Substitui o `setTimeout` simulado de 1,4 s.

## Critérios de aceite

```gherkin
Cenário: devolução autorizada
  Quando devolvo 1 unidade e emito
  Então vejo a nota de devolução autorizada e posso baixar o PDF

Cenário: outro usuário devolveu antes
  Então a linha do item mostra a mensagem de quantidade excedida
```

## Subtasks

- [ ] ST-F09.3.1 Envio e acompanhamento
- [ ] ST-F09.3.2 Erros por item

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F09.3.1 | Unitário | payload filtrado | RF01 |
| TC-F09.3.2 | Unitário | 422 por item | excedida |
| TC-F09.3.3 | E2E | devolução real | autorizada |
