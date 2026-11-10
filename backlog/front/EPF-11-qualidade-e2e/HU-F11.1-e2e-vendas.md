# HU-F11.1 — E2E do fluxo de venda

**Épico:** [EPF-11](README.md) · **Estimativa:** 5 pts · **Depende de:** EPF-01, EPF-03, EPF-04, EPF-05

## História

Como **banca do TCC**, quero **ver um teste automatizado fazendo uma venda pela tela** para **confirmar que front e back conversam**.

## Requisitos funcionais

- RF01 — Projeto Playwright em `e2e/` com `globalSetup` que confere `make status` do back e roda `make seed`.
- RF02 — Fixture de login (admin e vendedor) guardando o estado da sessão.
- RF03 — Cenários: login inválido e válido; cadastrar cliente; cadastrar produto com estoque inicial; venda NFC-e consumidor final até "Imprimir cupom" disponível; venda NF-e para cliente sem endereço → completar cadastro → finalizar; salvar e finalizar orçamento.
- RF04 — Seletores por `data-testid` e papéis ARIA (não por classe CSS).

## Critérios de aceite

```gherkin
Cenário: venda NFC-e pela tela
  Dado a stack no ar com seed
  Quando o teste faz login, adiciona PF-001 e finaliza em pix
  Então em até 60 segundos o botão "Imprimir cupom" aparece
```

## Subtasks

- [ ] ST-F11.1.1 Configuração do Playwright e fixtures
- [ ] ST-F11.1.2 Cenários de cadastro e venda
- [ ] ST-F11.1.3 `data-testid` nas telas envolvidas

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F11.1.1 | E2E | login | RF03 |
| TC-F11.1.2 | E2E | venda NFC-e | venda pela tela |
| TC-F11.1.3 | E2E | NF-e com 422 e correção | RF03 |
| TC-F11.1.4 | E2E | orçamento | RF03 |
