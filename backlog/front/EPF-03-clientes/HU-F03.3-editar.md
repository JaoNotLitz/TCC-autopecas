# HU-F03.3 — Editar cliente

**Épico:** [EPF-03](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-F03.2, HU-B03.4

## História

Como **vendedor**, quero **completar ou corrigir um cadastro** para **poder emitir NF-e**.

## Requisitos funcionais

- RF01 — "Editar" busca `GET /api/clientes/{id}` e abre o mesmo formulário preenchido.
- RF02 — Salvar envia `PUT /api/clientes/{id}`; sucesso atualiza a linha na lista.
- RF03 — Suporta abrir direto com foco no endereço (`/clientes/{id}/editar?foco=endereco`), usado pelo PDV quando a NF-e recebe `422`.

## Critérios de aceite

```gherkin
Cenário: completar endereço
  Dado um cliente sem endereço
  Quando preencho o endereço e salvo
  Então a linha é atualizada e o cliente fica apto para NF-e
```

## Subtasks

- [ ] ST-F03.3.1 Modo edição do formulário e foco por parâmetro

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F03.3.1 | Unitário | carrega, edita e envia PUT | completar |
