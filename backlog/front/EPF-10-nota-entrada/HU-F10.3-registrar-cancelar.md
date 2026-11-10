# HU-F10.3 — Registrar entrada e limpar formulário

**Épico:** [EPF-10](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-F10.1, HU-B11.1

## História

Como **estoquista**, quero **registrar a entrada e começar outra** para **lançar várias notas seguidas**.

## Requisitos funcionais

- RF01 — "Registrar" envia `POST /api/entrada` com `entradaFormParaInput`; sucesso mostra "Entrada registrada. O estoque será atualizado em instantes" e limpa o formulário.
- RF02 — `422` (código não cadastrado) e `400` marcam os campos pelos caminhos de `campos[]` (ex.: `itens[2].codigo`).
- RF03 — "Cancelar" (hoje sem handler) pede confirmação se houver dados e limpa o formulário.
- RF04 — Substitui o `setTimeout` simulado.

## Critérios de aceite

```gherkin
Cenário: registrar
  Quando registro uma entrada válida
  Então vejo a mensagem de sucesso e o formulário vazio

Cenário: cancelar com dados
  Dado um formulário preenchido
  Quando clico em Cancelar e confirmo
  Então o formulário fica vazio
```

## Subtasks

- [ ] ST-F10.3.1 Envio e erros por campo
- [ ] ST-F10.3.2 Cancelar com confirmação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F10.3.1 | Unitário | sucesso limpa | registrar |
| TC-F10.3.2 | Unitário | 422 marca `itens[2].codigo` | RF02 |
| TC-F10.3.3 | Unitário | cancelar com e sem dados | cancelar |
