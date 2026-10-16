# HU-F03.2 — Cadastrar cliente

**Épico:** [EPF-03](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F00.2, HU-F00.4, HU-B03.1

## História

Como **vendedor**, quero **cadastrar um cliente com os dados que tenho agora** para **não travar a venda, sabendo que para NF-e vou precisar do endereço**.

## Requisitos funcionais

- RF01 — Botão "Novo cliente" abre formulário (modal ou rota `/clientes/novo`) com seções Identificação (tipo, documento, nome, nome fantasia, IE) — Contato (e-mail, telefone) — Endereço (CEP, logradouro, número, complemento, bairro, cidade, UF).
- RF02 — Obrigatórios só tipo, documento e nome. Aviso informativo: "Endereço completo é necessário para emitir NF-e".
- RF03 — Campos de PJ (nome fantasia, IE) só aparecem com tipo PJ; o rótulo do documento muda para CPF ou CNPJ.
- RF04 — Envia `POST /api/clientes` com documento, telefone e CEP só com dígitos; sucesso fecha o formulário, mostra toast e insere o cliente no topo da lista.
- RF05 — `409` marca documento ou e-mail com a mensagem do back; `400` marca os campos de `campos[]`.
- RF06 — O mesmo componente de formulário é reaproveitado no cadastro rápido do PDV (HU-F05.3), com modo `compacto`.

## Regras e requisitos não funcionais

- Validação local igual à do back (CPF/CNPJ, UF, telefone 10–11, CEP 8).

## Critérios de aceite

```gherkin
Cenário: cadastro rápido
  Quando preencho tipo PF, CPF válido e nome e salvo
  Então vejo "Cliente cadastrado" e ele aparece na lista

Cenário: CNPJ já cadastrado
  Quando salvo um CNPJ existente
  Então o campo CNPJ mostra "Já existe cliente com este documento"

Cenário: CPF inválido
  Então o botão Salvar fica bloqueado e o campo mostra "CPF inválido"
```

## Subtasks

- [ ] ST-F03.2.1 Componente `ClienteFormComponent` (Reactive Forms)
- [ ] ST-F03.2.2 Regras visuais PF/PJ
- [ ] ST-F03.2.3 Integração com POST e erros por campo

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F03.2.1 | Unitário | validações locais | inválido |
| TC-F03.2.2 | Unitário (`HttpTestingController`) | 409 marca o campo | já cadastrado |
| TC-F03.2.3 | Unitário | payload sem máscara | RF04 |
| TC-F03.2.4 | E2E | cadastro real | rápido |
