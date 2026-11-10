# HU-F11.3 — Acessibilidade e responsivo

**Épico:** [EPF-11](README.md) · **Estimativa:** 3 pts · **Depende de:** EPF-01 a EPF-10

## História

Como **usuário no celular ou com leitor de tela**, quero **usar as telas principais sem barreiras** para **trabalhar de qualquer dispositivo**.

## Requisitos funcionais

- RF01 — Checagem automática com `@axe-core/playwright` nas 10 rotas, sem violações `serious`/`critical`.
- RF02 — Formulários com `label` associado, mensagens de erro ligadas por `aria-describedby`, foco visível.
- RF03 — PDV, Clientes e Painel Fiscal utilizáveis em 375 px (tabelas viram cartões ou rolam só dentro do contêiner).
- RF04 — PDV operável só pelo teclado (busca, carrinho, pagamento, finalizar).

## Critérios de aceite

```gherkin
Cenário: axe sem violações graves
  Quando o teste roda o axe em cada rota
  Então não há violações serious ou critical
```

## Subtasks

- [ ] ST-F11.3.1 Testes axe
- [ ] ST-F11.3.2 Correções de labels e foco
- [ ] ST-F11.3.3 Ajustes de layout mobile

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F11.3.1 | E2E (axe) | 10 rotas | axe |
| TC-F11.3.2 | E2E (viewport 375) | PDV, Clientes e Painel | RF03 |
| TC-F11.3.3 | E2E (só teclado) | venda no PDV | RF04 |
