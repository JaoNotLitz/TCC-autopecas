# HU-F05.4 — Forma de pagamento

**Épico:** [EPF-05](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-F05.2

## História

Como **vendedor**, quero **informar como o cliente pagou** para **a nota e o caixa ficarem corretos**.

## Requisitos funcionais

- RF01 — Modalidade (física/virtual) e método (dinheiro, crédito, débito, pix, boleto).
- RF02 — Bandeira (mastercard, visa, elo, outros) visível e obrigatória só para crédito/débito; limpa ao trocar de método.
- RF03 — Destino do dinheiro (banco, cofre) visível e obrigatório só para dinheiro; limpa ao trocar.
- RF04 — Mapeia para `metodoPagamento`, `bandeira`, `destinoDinheiro` do `VendaInput` (o estado local `destinoMoeda` vira `destinoDinheiro`).

## Critérios de aceite

```gherkin
Cenário: crédito sem bandeira
  Quando escolho crédito e não escolho bandeira
  Então Finalizar fica desabilitado com aviso "Escolha a bandeira"

Cenário: trocar de crédito para pix
  Então a bandeira escolhida é descartada do payload
```

## Subtasks

- [ ] ST-F05.4.1 Regras condicionais e mapeamento

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.4.1 | Unitário (parametrizado) | combinações método × campos | cenários |
