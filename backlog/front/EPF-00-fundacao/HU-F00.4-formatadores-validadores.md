# HU-F00.4 — Pipes de formatação e validadores

**Épico:** [EPF-00](README.md) · **Estimativa:** 3 pts · **Depende de:** —

## História

Como **usuário**, quero **ver CPF, CNPJ, telefone, NCM, datas e valores formatados** e **ser avisado na hora quando digito um documento inválido**.

## Requisitos funcionais

- RF01 — Pipes standalone: `documento` (CPF/CNPJ pelo tamanho), `telefone`, `ncm`, `cep`, `chaveAcesso` (grupos de 4), `moedaBr` (R$), `dataBr` (dd/MM/yyyy) e `horaBr` (HH:mm), com fuso `America/Sao_Paulo`.
- RF02 — Validadores de Reactive Forms: `cpfValidator`, `cnpjValidator`, `cpfOuCnpjValidator`, `ufValidator`, `ncmValidator`, `chaveAcessoValidator` (com DV), `minTrimLength(n)`.
- RF03 — Diretiva `appMascara` para inputs (documento, telefone, CEP, NCM) que exibe com máscara mas guarda só dígitos no controle.
- RF04 — Reaproveitar as funções da seção 8.1 do mapeamento como implementação base.

## Critérios de aceite

```gherkin
Cenário: CNPJ formatado
  Dado o documento "12345678000195"
  Então o pipe mostra "12.345.678/0001-95"

Cenário: CPF inválido no formulário
  Quando digito "111.111.111-11"
  Então o campo mostra "CPF inválido" e o valor do controle é "11111111111"
```

## Subtasks

- [ ] ST-F00.4.1 Pipes
- [ ] ST-F00.4.2 Validadores
- [ ] ST-F00.4.3 Diretiva de máscara

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F00.4.1 | Unitário (parametrizado) | cada pipe com entradas válidas, vazias e tamanhos errados | formatado |
| TC-F00.4.2 | Unitário | CPF/CNPJ válidos, inválidos e repetidos | inválido |
| TC-F00.4.3 | Unitário (componente host) | máscara exibe formatado e guarda dígitos | RF03 |
