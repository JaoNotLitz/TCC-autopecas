# HU-F05.3 — Identificar ou cadastrar cliente

**Épico:** [EPF-05](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F03.2, HU-B03.2

## História

Como **vendedor**, quero **identificar o cliente pelo CPF/CNPJ ou cadastrá-lo na hora** para **emitir NF-e ou colocar o CPF na NFC-e**.

## Requisitos funcionais

- RF01 — Seletor NFC-e / NF-e. NF-e: cliente obrigatório ("CNPJ/CPF *"). NFC-e: opcional ("Consumidor final" quando vazio).
- RF02 — Campo de documento com máscara; ao sair do campo (ou Enter) busca `GET /api/clientes?documento=`.
- RF03 — Encontrado: mostra nome, documento e um selo "Endereço completo"/"Endereço incompleto" (`enderecoCompleto` do detalhe).
- RF04 — Não encontrado: botão "Cadastrar cliente" abre o `ClienteFormComponent` compacto com o documento preenchido; ao salvar, seleciona o cliente.
- RF05 — NF-e com cliente de endereço incompleto: aviso antes de finalizar com atalho "Completar cadastro".
- RF06 — `?clienteId=` na URL (vindo do início) carrega o cliente.
- RF07 — Envia só `clienteId` na venda (nunca CPF/nome soltos).

## Critérios de aceite

```gherkin
Cenário: NF-e com cliente completo
  Quando escolho NF-e e informo um CNPJ cadastrado com endereço
  Então vejo o nome e o selo "Endereço completo"

Cenário: cliente novo no balcão
  Quando informo um CPF não cadastrado e cadastro só com nome
  Então o cliente fica selecionado para a venda

Cenário: trocar para NFC-e
  Quando troco para NFC-e e limpo o documento
  Então a venda vai como consumidor final
```

## Subtasks

- [ ] ST-F05.3.1 Bloco de identificação por tipo de documento
- [ ] ST-F05.3.2 Busca por documento e estados (encontrado, não encontrado, carregando)
- [ ] ST-F05.3.3 Cadastro rápido reaproveitando o formulário de clientes

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F05.3.1 | Unitário | regras de obrigatoriedade NF-e × NFC-e | trocar |
| TC-F05.3.2 | Unitário | encontrado e não encontrado | cenários |
| TC-F05.3.3 | E2E | cadastro no balcão e venda | cliente novo |
