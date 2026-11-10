# HU-B11.2 — Importar XML de entrada (prévia)

**Épico:** [EPB-11](README.md) · **Unidade:** `entry-service` · **Estimativa:** 5 pts · **Depende de:** HU-B11.1

## História

Como **estoquista**, quero **subir o XML da nota do fornecedor e ver o formulário preenchido antes de confirmar** para **não digitar tudo e ainda poder corrigir**.

## Requisitos funcionais

- RF01 — `POST /api/notas/entrada/importar-xml` (`multipart/form-data`, campo **`arquivo`**, como o `notasService.importarXmlEntrada` do front) responde `200` com um `EntradaInput` preenchido, sem gravar nada.
- RF02 — Parser `NfeEntradaParser` (compartilhado com o EPB-12) aceita `nfeProc` e `NFe` e extrai: fornecedor (`emit/CNPJ`, `xNome`, `IE`, `enderEmit/UF`), nota (`ide/nNF`, `serie`, `dhEmi` → data, `natOp`, chave de `protNFe/infProt/chNFe` ou do `Id` de `infNFe`), itens (`prod/cProd`, `xProd`, `NCM`, `qCom`, `vUnCom`, `CFOP`).
- RF03 — `dataEntrada` volta com a data de hoje (`yyyy-MM-dd`), que o usuário pode trocar.
- RF04 — Cada item volta com `produtoId` preenchido quando o `codigo` existe no catálogo, senão `null` (a tela mostra quais precisam ser cadastrados antes de salvar).

## Regras e requisitos não funcionais

- Arquivo > 5 MB → `413`; não é XML ou não é NF-e → `400` "XML inválido ou não é NF-e".
- Parser seguro contra XXE (DTD e entidades externas desabilitados).
- CFOP de saída do fornecedor (5102) é convertido para o de entrada correspondente (1102).
- Chave já registrada → `409`.

## Critérios de aceite

```gherkin
Cenário: XML válido preenche o formulário
  Quando envio o XML válido do seed no campo "arquivo"
  Então recebo 200 com um EntradaInput: fornecedor, numero, serie, datas, chave, naturezaOperacao e itens
  E nenhuma entrada é gravada

Cenário: arquivo grande
  Quando envio um arquivo de 6 MB
  Então recebo 413

Cenário: XML com entidade externa
  Quando envio um XML com DOCTYPE e entidade externa
  Então recebo 400 e nenhum arquivo do servidor é lido

Cenário: item não cadastrado
  Quando envio o XML do seed com código inexistente
  Então o item volta com produtoId null
```

## Subtasks

- [ ] ST-B11.2.1 `NfeEntradaParser` (StAX ou JAXB com proteção XXE)
- [ ] ST-B11.2.2 Conversão de CFOP saída → entrada
- [ ] ST-B11.2.3 Endpoint multipart (campo `arquivo`) com limite de tamanho

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B11.2.1 | Unitário | parse dos 3 XMLs do seed | válido / malformado |
| TC-B11.2.2 | Unitário | payload XXE | XXE |
| TC-B11.2.3 | API | 6 MB → 413 | grande |
| TC-B11.2.4 | Integração | nada gravado no DynamoDB/S3/SQS | RF01 |
| TC-B11.2.5 | Contrato | resposta valida contra `EntradaInput` do front | RF01 |
