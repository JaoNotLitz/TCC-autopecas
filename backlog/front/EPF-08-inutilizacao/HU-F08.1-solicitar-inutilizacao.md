# HU-F08.1 — Solicitar inutilização

**Épico:** [EPF-08](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B07.6, HU-B10.2

## História

Como **administradora**, quero **inutilizar uma faixa de números com confirmação clara** para **regularizar a numeração sem errar a faixa**.

## Requisitos funcionais

- RF01 — Formulário: modelo (NF-e 55 / NFC-e 65), série (padrão "1"), número inicial, número final, justificativa (≥ 15 após trim).
- RF02 — Mostra "Quantidade: N números" (`final − inicial + 1`) enquanto digita; final < inicial bloqueia.
- RF03 — Confirmação explícita: "Inutilizar os números X a Y da série S? Esta ação é irreversível".
- RF04 — `POST /api/notas/inutilizacao` com `numeroInicial`/`numeroFinal` como `number`; mostra `processando` e acompanha o resultado (via `eventos` da resposta ou reconsulta) até concluído/rejeitado.
- RF05 — `422` lista os números com nota emitida; `409` informa sobreposição.
- RF06 — Preencher `erro` (hoje nunca preenchido) e substituir o `setTimeout` simulado.

## Critérios de aceite

```gherkin
Cenário: faixa válida
  Quando informo 100 a 105 com justificativa e confirmo
  Então vejo "6 números" e depois o protocolo da inutilização

Cenário: faixa com nota emitida
  Então vejo a mensagem do back citando o número
```

## Subtasks

- [ ] ST-F08.1.1 Formulário com quantidade calculada
- [ ] ST-F08.1.2 Confirmação e envio
- [ ] ST-F08.1.3 Tratamento de 422/409

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F08.1.1 | Unitário | conversão para number e quantidade | válida |
| TC-F08.1.2 | Unitário | 422 exibido | com nota |
