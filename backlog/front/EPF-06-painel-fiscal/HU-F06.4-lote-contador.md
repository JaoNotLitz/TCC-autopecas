# HU-F06.4 — Enviar lote ao contador

**Épico:** [EPF-06](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-B13.4

## História

Como **dona da loja**, quero **mandar ao contador os XMLs de um período ou das notas que eu selecionar** para **fechar o mês sem trabalho manual**.

## Requisitos funcionais

- RF01 — Seleção múltipla por checkbox na lista (só autorizadas/canceladas).
- RF02 — "Enviar lote" abre diálogo com e-mail do contador (lembrado em `localStorage` por conveniência) e opção "Notas selecionadas" ou "Todas do período".
- RF03 — `POST /api/documentos/lote-contador`; sucesso mostra quantidade de arquivos, tamanho e link "Baixar ZIP" (válido por 1 h) e informa que o e-mail foi enviado.

## Critérios de aceite

```gherkin
Cenário: lote do período
  Quando escolho "Todas do período" e informo o e-mail
  Então vejo "25 arquivos" e o link para baixar o ZIP
```

## Subtasks

- [ ] ST-F06.4.1 Seleção múltipla
- [ ] ST-F06.4.2 Diálogo e chamada

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F06.4.1 | Unitário | payload por ids e por período | RF02 |
| TC-F06.4.2 | Unitário | e-mail inválido bloqueia | RF02 |
