# HU-F07.2 — Solicitar e acompanhar cancelamento

**Épico:** [EPF-07](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F07.1, HU-B07.5, HU-B10.1

## História

Como **administradora**, quero **cancelar a nota com justificativa e ver se a SEFAZ aceitou** para **ter certeza de que a venda foi desfeita**.

## Requisitos funcionais

- RF01 — Formulário com justificativa (mínimo 15 caracteres após trim, contador) e confirmação "Esta ação não pode ser desfeita".
- RF02 — `POST /api/notas/{id}/cancelamento`; mostra "Solicitação enviada à SEFAZ" e acompanha `GET /api/notas/{id}` a cada 3 s (até 60 s) até `cancelada` ou evento `rejeitado`.
- RF03 — Sucesso: "Nota cancelada. O estoque foi devolvido". Rejeição: motivo do evento.
- RF04 — `422` (prazo expirado) e `409` (já em andamento) mostram a message do back.
- RF05 — Substitui o `setTimeout` simulado de 1,2 s.

## Critérios de aceite

```gherkin
Cenário: cancelamento aceito
  Quando envio justificativa válida e confirmo
  Então vejo "Processando" e depois "Nota cancelada"

Cenário: prazo expirado
  Dado que a nota passou de 24 horas enquanto eu escrevia
  Então vejo "Prazo de cancelamento expirado"
```

## Subtasks

- [ ] ST-F07.2.1 Formulário e confirmação
- [ ] ST-F07.2.2 Acompanhamento do resultado

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F07.2.1 | Unitário | justificativa com espaços | RF01 |
| TC-F07.2.2 | Unitário (fake timers) | processando → cancelada | aceito |
| TC-F07.2.3 | Unitário | 422 exibido | prazo |
| TC-F07.2.4 | E2E | cancelamento real | aceito |
