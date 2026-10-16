# HU-B08.3 — Assinar com certificado A1

**Épico:** [EPB-08](README.md) · **Unidade:** `FiscalProcessorListener` · **Estimativa:** 5 pts · **Depende de:** HU-B08.1, HU-B00.4

## História

Como **sistema fiscal**, quero **assinar o XML com o certificado digital da empresa** para **a SEFAZ reconhecer o emitente**.

## Requisitos funcionais

- RF01 — Carrega o `.pfx` e a senha de `wc-sefaz-certificate` (cache em memória, recarregado a cada 1 h).
- RF02 — Assina o elemento `infNFe` com XMLDSig enveloped: `RSA-SHA1`, `SHA1` digest, canonicalização C14N, transforms `enveloped-signature` + `c14n`, `Reference URI="#NFe{chave}"`, `KeyInfo/X509Data/X509Certificate` (padrão exigido pela SEFAZ no layout 4.00).
- RF03 — O mesmo componente assina eventos (`infEvento`) e inutilizações (`infInut`), usado no EPB-10.

## Regras e requisitos não funcionais

- Certificado vencido ou a vencer em menos de 0 dias → erro definitivo (HU-B08.6). A menos de 30 dias → publica aviso `warning` no `system-notifications-topic` uma vez por dia.
- A chave privada nunca é logada nem gravada em disco.

## Critérios de aceite

```gherkin
Cenário: assinatura válida
  Dado o certificado de teste do seed
  Quando um XML de NF-e é assinado
  Então a validação com javax.xml.crypto.dsig confirma a assinatura e o digest

Cenário: XML alterado depois de assinado
  Quando altero o valor de um item após assinar
  Então a validação da assinatura falha

Cenário: certificado expirado
  Dado o secret apontando para o certificado expirado do seed
  Quando um job é processado
  Então a nota não é assinada e o erro é tratado como definitivo
```

## Subtasks

- [ ] ST-B08.3.1 `CertificadoProvider` (Secrets Manager + cache)
- [ ] ST-B08.3.2 `XmlSigner` genérico por elemento
- [ ] ST-B08.3.3 Aviso de vencimento

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B08.3.1 | Unitário | assina e valida | válida |
| TC-B08.3.2 | Unitário | adulteração detectada | alterado |
| TC-B08.3.3 | Integração (LocalStack) | certificado expirado | expirado |
