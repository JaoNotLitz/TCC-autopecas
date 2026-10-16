# HU-F10.2 — Importar XML com prévia

**Épico:** [EPF-10](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F10.1, HU-B11.2

## História

Como **estoquista**, quero **carregar o XML do fornecedor e ver o formulário preenchido** para **só conferir e confirmar**.

## Requisitos funcionais

- RF01 — Botão "Importar XML" (hoje sem handler) abre seletor de arquivo (`.xml`, máx. 5 MB validado no front) e também aceita arrastar e soltar.
- RF02 — Envia `multipart/form-data` para `POST /api/entrada/importar-xml/previa` com indicador de carregamento.
- RF03 — Preenche fornecedor, nota e itens com a resposta; `dataEntrada` fica vazia e com foco para o usuário preencher.
- RF04 — Itens com `cadastrado: false` ficam destacados com "Produto não cadastrado" e um link "Cadastrar produto" (abre o formulário de produto com código, descrição e NCM preenchidos, HU-F04.2); o registro fica bloqueado enquanto houver item não cadastrado.
- RF05 — Erros: `400` "XML inválido ou não é NF-e"; `409` "Esta nota já foi registrada"; `413` "Arquivo maior que 5 MB".

## Critérios de aceite

```gherkin
Cenário: XML válido
  Quando importo o XML do seed
  Então o formulário fica preenchido com fornecedor, nota e itens, e o foco vai para Data de entrada

Cenário: produto desconhecido
  Quando importo o XML com código inexistente
  Então o item aparece destacado e o botão Registrar fica bloqueado até eu cadastrar o produto

Cenário: nota repetida
  Então vejo "Esta nota já foi registrada"
```

## Subtasks

- [ ] ST-F10.2.1 Upload com seleção e arrastar-soltar
- [ ] ST-F10.2.2 Preenchimento do formulário com a prévia
- [ ] ST-F10.2.3 Destaque e atalho para cadastrar produto

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F10.2.1 | Unitário | FormData enviado | RF02 |
| TC-F10.2.2 | Unitário | preenchimento com fixture | válido |
| TC-F10.2.3 | Unitário | bloqueio com item não cadastrado | desconhecido |
| TC-F10.2.4 | E2E | importar XML do seed e registrar | válido |
