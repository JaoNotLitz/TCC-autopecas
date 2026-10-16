# HU-B08.1 — Gerar XML layout 4.00

**Épico:** [EPB-08](README.md) · **Unidade:** `FiscalProcessorListener` · **Estimativa:** 8 pts · **Depende de:** HU-B07.1, HU-B00.4

## História

Como **responsável fiscal**, quero **que o sistema monte o XML da nota com todos os grupos obrigatórios** para **a SEFAZ aceitar a nota**.

## Requisitos funcionais

- RF01 — Listener (3 concorrentes, visibility 120 s) recebe `{jobType: "generate_xml", notaId, tipoNota}`.
- RF02 — Monta o XML com JAXB a partir das classes geradas do XSD 4.00: `ide`, `emit` (do `emitente.json`), `dest` (NF-e e devolução; NFC-e só se houver CPF), `det` por item (`prod` + `imposto` com ICMS conforme `cstIcms`/`aliquotaIcms`, PIS/COFINS 07 para Simples ou alíquota configurada), `total/ICMSTot`, `transp` (`modFrete = 9`), `pag` (`tPag` mapeado do método: dinheiro 01, crédito 03, débito 04, boleto 15, pix 17; com `card/tBand` para cartão: visa 01, mastercard 02, elo 06, outros 99), `infAdic`.
- RF03 — Devolução: `finNFe = 4`, `NFref/refNFe` com a chave original, CFOP 1202/2202, `tpNF = 0` (entrada).
- RF04 — NFC-e: `mod = 65`, `tpImp = 4`, `indFinal = 1`, `indPres = 1` (física) ou `2` (virtual), e `infNFeSupl` com `qrCode` e `urlChave` (gerados após a assinatura, usando CSC de teste da configuração).
- RF05 — `tpAmb = 2` (homologação) no perfil `local`.

## Regras e requisitos não funcionais

- Totais calculados em `BigDecimal` e conferidos: `vNF = vProd − vDesc`; diferença de centavos no rateio do desconto vai para o último item.
- Dados de homologação: nome do destinatário substituído por "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL" (regra da SEFAZ).

## Critérios de aceite

```gherkin
Cenário: NFC-e consumidor final
  Dado uma nota NFCE sem cliente com 2 itens e pagamento crédito visa
  Quando o job é processado
  Então o XML tem mod 65, sem grupo dest, tPag 03 com tBand 01 (Visa) e infNFeSupl preenchido

Cenário: NF-e para PJ
  Dado uma nota NFE para cliente PJ com endereço completo
  Então o XML tem grupo dest com CNPJ, IE e enderDest

Cenário: devolução
  Dado uma nota DEVOLUCAO
  Então o XML tem finNFe 4, tpNF 0 e NFref com a chave da nota original

Cenário: rateio de desconto
  Dado 3 itens de 10,00 e desconto 1,00
  Então a soma dos vDesc dos itens é 1,00 e vNF é 29,00
```

## Subtasks

- [ ] ST-B08.1.1 Gerar classes JAXB a partir do XSD (plugin `jaxb2-maven-plugin`)
- [ ] ST-B08.1.2 `NfeXmlBuilder` por grupo (ide, emit, dest, det, total, transp, pag, infAdic)
- [ ] ST-B08.1.3 Regras específicas de NFC-e e devolução
- [ ] ST-B08.1.4 Rateio de desconto
- [ ] ST-B08.1.5 QR Code NFC-e (versão 2 do QR, hash com CSC)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B08.1.1 | Unitário (snapshot XML) | NFC-e consumidor final | NFC-e |
| TC-B08.1.2 | Unitário (snapshot XML) | NF-e PJ | NF-e |
| TC-B08.1.3 | Unitário | devolução | devolução |
| TC-B08.1.4 | Unitário (parametrizado) | rateio com dízimas | rateio |
