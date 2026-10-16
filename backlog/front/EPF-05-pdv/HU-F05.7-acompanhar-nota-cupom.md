# HU-F05.7 — Acompanhar a nota e imprimir cupom

**Épico:** [EPF-05](README.md) · **Estimativa:** 3 pts · **Depende de:** HU-F05.5, HU-B07.2, HU-B13.3

## História

Como **vendedor**, quero **ver quando a nota é autorizada e imprimir o cupom ali mesmo** para **entregar ao cliente sem trocar de tela**.

## Requisitos funcionais

- RF01 — Após a venda, consulta `GET /api/notas?vendaId={id}` a cada 2 s (até 60 s) mostrando "Processando nota…".
- RF02 — `autorizada`: mostra número, chave formatada e protocolo, e os botões "Imprimir cupom" (só NFC-e; abre `GET /api/documentos/pdf/{id}?inline=true` numa janela e chama `print()`), "Baixar PDF" e "Baixar XML".
- RF03 — `rejeitada`: mostra o motivo e o link para o Painel Fiscal.
- RF04 — Passou de 60 s: "A nota ainda está em processamento. Você pode acompanhar no Painel Fiscal" e libera o PDV.
- RF05 — "Nova venda" limpa a tela a qualquer momento (o acompanhamento continua no painel).

## Critérios de aceite

```gherkin
Cenário: cupom impresso
  Dado uma venda NFC-e finalizada
  Quando a nota é autorizada
  Então o botão Imprimir cupom abre o DANFE NFC-e para impressão

Cenário: rejeição
  Quando a nota é rejeitada
  Então vejo o motivo e o link para o painel
```

## Subtasks

- [ ] ST-F05.7.1 Polling com `timer` + `switchMap` + `takeWhile`
- [ ] ST-F05.7.2 Ações de documento (reaproveitando `DocumentosService`)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.7.1 | Unitário (fake timers) | processando → autorizada | cupom |
| TC-F05.7.2 | Unitário | rejeitada e timeout de 60 s | rejeição |
| TC-F05.7.3 | E2E | venda até cupom disponível | cupom |
