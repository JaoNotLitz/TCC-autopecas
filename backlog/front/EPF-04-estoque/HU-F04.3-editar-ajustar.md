# HU-F04.3 — Editar produto e ajustar quantidade

**Épico:** [EPF-04](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F04.2, HU-B04.4, HU-B04.6

## História

Como **gerente**, quero **editar dados do produto e, separadamente, corrigir a quantidade informando o motivo** para **o histórico mostrar por que o estoque mudou**.

## Requisitos funcionais

- RF01 — `ModalEditarProduto` passa a usar o `ProdutoFormComponent` sem o campo de quantidade; salvar chama `PUT /api/produtos/{id}`.
- RF02 — Seção/aba "Ajustar estoque" (só ADMIN) com "Nova quantidade" e "Motivo" (mínimo 10 caracteres, contador visível). Envia `POST /api/produtos/{id}/ajuste`.
- RF03 — Resposta `202` (ou `200`): mostra "Ajuste enviado" e recarrega o produto após 2 s (o estoque é atualizado pelo listener); se ainda não mudou, mostra "Processando…" e tenta de novo até 3 vezes.
- RF04 — `422` (abaixo do reservado) mostra a mensagem no campo de quantidade.

## Critérios de aceite

```gherkin
Cenário: ajuste com motivo
  Dado um produto com 40 unidades
  Quando ajusto para 35 com motivo "Inventário físico de outubro"
  Então em alguns segundos a lista mostra 35

Cenário: motivo curto
  Quando escrevo "erro"
  Então o botão Ajustar fica bloqueado

Cenário: editar não mexe no estoque
  Quando altero só o preço e salvo
  Então a quantidade não muda e nenhum ajuste é enviado
```

## Subtasks

- [ ] ST-F04.3.1 Separar edição e ajuste no modal
- [ ] ST-F04.3.2 Recarregamento após ajuste assíncrono

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F04.3.1 | Unitário | edição envia PUT sem quantidade | não mexe |
| TC-F04.3.2 | Unitário | motivo mínimo | curto |
| TC-F04.3.3 | Unitário (fake timers) | recarrega até refletir | com motivo |
| TC-F04.3.4 | E2E | ajuste real | com motivo |
