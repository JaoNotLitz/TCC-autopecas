# Mapeamento de Classes — WC Autopeças (front-end)

Documento de referência para integrar o front-end com o back-end. Ele lista as entidades de dados, os componentes, as regras de negócio que hoje vivem no front e os pontos onde o back precisa entrar.

> **Estado atual:** o projeto é 100% front-end. Não existe chamada HTTP (`fetch`/`axios`) em nenhum arquivo. Todos os dados são *mocks* fixos no código ou estado local do React, e as "transmissões à SEFAZ" são `setTimeout` simulados.
>
> **Sobre o termo "classes":** o projeto não usa `class`. Os equivalentes são as `interface`/`type` do TypeScript (modelo de dados) e as *function components* do React (telas e UI). Este documento cobre os dois e propõe, na seção 4, o modelo que o back deve expor.

Stack original: React 18 + Vite 6 + Tailwind 4 + `react-router` 7. Ícones em `lucide-react`. Alias `@` aponta para `src/`.

> **Atualização (2026-09-26): o front agora é Angular 22.** As seções abaixo descrevem o código React original, que continua em `referencia-react/src/`. Os modelos, regras de negócio e endpoints valem sem mudança. Correspondência de caminhos e conceitos:
>
> | React (original) | Angular (atual) |
> |---|---|
> | `main.tsx`, `App.tsx` | `src/main.ts`, `app/app.ts`, `app/app.config.ts` |
> | `routes.ts` | `app/app.routes.ts` |
> | `context/AuthContext.tsx` (`AuthProvider`, `useAuth`) | `core/auth.service.ts` (`AuthService`, signals) e `core/auth.guard.ts` |
> | `components/{Layout,Sidebar,MobileHeader}.tsx` | `layout/{layout,sidebar,mobile-header}` |
> | `components/{Vendas,Estoque,Fiscal}Card.tsx` | `cards/{vendas,estoque,fiscal}-card.ts` |
> | `pages/Nome.tsx` | `pages/nome.ts` + `nome.html` (kebab-case, ex.: `painel-fiscal`, `nota-entrada`) |
> | `ModalEditarProduto` (interno de `Estoque.tsx`) | `pages/modal-editar-produto.ts` |
> | `FileIcon` (interno de `PDV.tsx`) | `<svg>` inline no `pdv.html` |
> | `services/{auth,clientes,…}.ts` (objetos) | `services/*.service.ts` (classes `@Injectable`; o de auth chama-se `AuthApiService`) |
> | `useState` / `useEffect` | `signal()` / `computed()` / `effect()` |
> | `VITE_API_URL`, `VITE_MOCK_AUTH` | `environment.apiUrl`, `environment.mockAuth` (`src/environments/`) |
> | `components/ui/` (shadcn, sem uso) e bibliotecas sem uso | não portados |

---

## 1. Visão geral da estrutura

```
src/
├── main.tsx                     # entrada: monta <App/> em #root
├── app/
│   ├── App.tsx                  # AuthProvider > RouterProvider
│   ├── routes.ts                # tabela de rotas (createBrowserRouter)
│   ├── context/AuthContext.tsx  # AuthProvider + useAuth
│   ├── components/
│   │   ├── Layout.tsx           # casca autenticada (Sidebar + MobileHeader + <Outlet/>)
│   │   ├── Sidebar.tsx          # menu lateral
│   │   ├── MobileHeader.tsx     # barra superior mobile
│   │   ├── VendasCard.tsx       # card do Home
│   │   ├── EstoqueCard.tsx      # card do Home
│   │   ├── FiscalCard.tsx       # card do Home
│   │   ├── figma/ImageWithFallback.tsx
│   │   └── ui/                  # biblioteca shadcn/ui (sem regra de negócio)
│   └── pages/                   # 10 telas (uma por rota)
└── styles/                      # tailwind, tema, fontes
```

`components/ui/` é a biblioteca shadcn/ui gerada pelo Figma Make (accordion, dialog, table, form etc.) mais `use-mobile.ts` e `utils.ts`. **Nenhuma página ou componente de negócio importa de `ui/`**: as telas usam `<button>`, `<table>` e `<input>` puros com classes Tailwind. Ela não precisa de integração com o back.

`figma/ImageWithFallback.tsx` também não é usado por nenhuma tela hoje.

---

## 2. Rotas e navegação

Definidas em `src/app/routes.ts`. Tudo abaixo de `/` passa por `Layout`, que redireciona para `/login` se `isAuthenticated` for falso.

| Rota | Componente | Arquivo | Menu (Sidebar) | Integração com back |
|---|---|---|---|---|
| `/login` | `Login` | `pages/Login.tsx` | — | Autenticação |
| `/` | `Home` | `pages/Home.tsx` | Início | Resumos (vendas, estoque baixo, notas do dia) |
| `/pdv` | `PDV` | `pages/PDV.tsx` | PDV / Vendas | Venda, orçamento, emissão NF-e/NFC-e |
| `/clientes` | `Clientes` | `pages/Clientes.tsx` | Clientes | CRUD de clientes |
| `/estoque` | `Estoque` | `pages/Estoque.tsx` | Estoque | CRUD de produtos |
| `/fiscal` | `PainelFiscal` | `pages/PainelFiscal.tsx` | Fiscal › Painel Fiscal | Listagem de notas, XML/PDF, lote ao contador |
| `/cancelamento` | `Cancelamento` | `pages/Cancelamento.tsx` | Fiscal › Cancelamento | Evento de cancelamento |
| `/inutilizacao` | `Inutilizacao` | `pages/Inutilizacao.tsx` | Fiscal › Inutilização | Inutilização de faixa |
| `/devolucao` | `NotaDevolucao` | `pages/NotaDevolucao.tsx` | Fiscal › Nota de Devolução | Emissão de NF-e de devolução |
| `/entrada` | `NotaEntrada` | `pages/NotaEntrada.tsx` | Fiscal › Nota de Entrada | Registro de nota de entrada + import XML |

---

## 3. Modelos de dados existentes no front

Todos os tipos abaixo estão declarados **localmente dentro de cada página** (não há pasta `types/` compartilhada). Por isso o mesmo conceito aparece com nomes de campo diferentes em telas diferentes (ver seção 7).

### 3.1 `Cliente` — `pages/Clientes.tsx:15`

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `number` | |
| `tipo` | `"PF" \| "PJ"` | |
| `documento` | `string` | CPF ou CNPJ **já formatado** (`12.345.678/0001-90`) |
| `nome` | `string` | Nome ou razão social |
| `email` | `string` | |
| `telefone` | `string` | Formatado, `(11) 98765-4321` |
| `cidade` | `string` | |
| `uf` | `string` | |
| `totalCompras` | `number` | Contagem de compras (campo calculado) |
| `ultimaCompra` | `string` | `dd/MM/yyyy` (campo calculado) |
| `status` | `"ativo" \| "inativo"` | |

Origem: constante `clientesData` (8 registros). Filtros locais: tipo (`todos`/`PF`/`PJ`) e busca por nome, documento ou e-mail.

### 3.2 `Produto` — `pages/Estoque.tsx:4`

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `number` | |
| `codigo` | `string` | SKU interno, ex.: `PF-001` |
| `nome` | `string` | |
| `ncm` | `string` | Formatado, `8708.30.10` |
| `estoque` | `number` | Quantidade atual |
| `minimo` | `number` | Estoque mínimo |
| `custo` | `number` | Preço de custo (R$) |
| `venda` | `number` | Preço de venda (R$) |
| `status` | `string` | Valores usados: `ok`, `atencao`, `baixo`, `critico`. **Não é union type** |

Origem: `produtosIniciais` (8 registros). `Estoque` mantém a lista em `useState` e só altera memória local.

### 3.3 `ItemCarrinho` — `pages/PDV.tsx:20`

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `number` | |
| `codigo` | `string` | Deveria referenciar `Produto.codigo` |
| `nome` | `string` | |
| `quantidade` | `number` | Mínimo 1 |
| `valorUnitario` | `number` | **Editável na venda** (pode divergir de `Produto.venda`) |
| `editandoPreco` | `boolean` | Somente UI, **não enviar ao back** |
| `precoTemp` | `string` | Somente UI, **não enviar ao back** |

### 3.4 Tipos auxiliares do PDV — `pages/PDV.tsx:30-41`

```ts
type TipoDocumento    = "nfe" | "nfce";
type Modalidade       = "fisica" | "virtual";
type DestinoMoeda     = "banco" | "cofre";
type MetodoPagamento  = "dinheiro" | "credito" | "debito" | "pix" | "boleto";
type BandeiraCartao   = "mastercard" | "visa" | "elo" | "outros";
```

Estado do PDV (todos em `useState`): `tipoDocumento`, `consumidorIdentificado`, `carrinho`, `modalidade`, `metodoPagamento`, `bandeira`, `destinoMoeda`, `desconto`. Os campos de CPF/CNPJ e nome do cliente **não estão ligados a nenhum estado** (são `<input>` soltos).

### 3.5 Nota fiscal (painel) — `pages/PainelFiscal.tsx:3` (sem interface)

Array `notasFiscais` sem tipo declarado:

| Campo | Tipo inferido | Observação |
|---|---|---|
| `id` | `number` | |
| `numero` | `string` | `"000126"` (com zeros à esquerda) |
| `serie` | `string` | |
| `cliente` | `string` | Só o nome (sem `clienteId`) |
| `cnpj` | `string` | Formatado |
| `valor` | `number` | |
| `data` | `string` | `dd/MM/yyyy` |
| `hora` | `string` | `HH:mm` |
| `status` | `string` | Usados: `autorizada`, `cancelada`, `processando` (tudo que não é os dois primeiros cai em "Processando") |

### 3.6 `NotaParaCancelar` — `pages/Cancelamento.tsx:4`

| Campo | Tipo TS |
|---|---|
| `id` | `number` |
| `numero` | `string` |
| `serie` | `string` |
| `cliente` | `string` |
| `valor` | `number` |
| `dataEmissao` | `string` (`dd/MM/yyyy`) |
| `chaveAcesso` | `string` (44 dígitos) |
| `status` | `"autorizada" \| "cancelada"` |

A tela só lista notas com `status === "autorizada"`. Formulário: `justificativa` (string, ≥ 15 caracteres).

### 3.7 Inutilização — `pages/Inutilizacao.tsx` (sem interface)

Formulário (`useState`):

| Campo | Tipo | Regra |
|---|---|---|
| `serie` | `string` | Padrão `"1"` |
| `numeroInicial` | `string` | Numérico, ≥ 1 |
| `numeroFinal` | `string` | ≥ `numeroInicial` |
| `justificativa` | `string` | ≥ 15 caracteres (após `trim`) |

Histórico exibido (mock `inutilizacoes`): `{ id, serie, inicio, fim, data, motivo, status: "inutilizada" }`. Note que o histórico usa `inicio`/`fim`, enquanto o formulário usa `numeroInicial`/`numeroFinal`.

### 3.8 `ItemDevolucao` e nota referenciada — `pages/NotaDevolucao.tsx`

```ts
interface ItemDevolucao {
  id: number; codigo: string; nome: string;
  qtdOriginal: number;   // quantidade vendida na nota original
  qtdDevolver: number;   // editável, limitada a [0, qtdOriginal]
  valorUnitario: number;
}
```

Nota referenciada (`notasReferencia`, tipo inferido): `{ numero, serie, cliente, cnpj, valor, data }`. Estado: `notaRef`, `itens`, `motivo`. Os itens vêm de `itensMock` (fixos, iguais para qualquer nota selecionada — o back deve devolver os itens reais da nota).

### 3.9 `ItemEntrada`, `fornecedor` e `nota` — `pages/NotaEntrada.tsx`

```ts
interface ItemEntrada {
  id: number;            // Date.now() para novos itens
  codigo: string; descricao: string; ncm: string;
  quantidade: number; valorUnitario: number;
  cfop: string;          // "1102" | "1202" | "1403" | "1411" | "2102" | "2202"
}
```

Fornecedor (`useState`, sem interface): `{ cnpj, razaoSocial, ie, uf }`. O campo `ie` existe no estado mas **não tem input na tela**.

Nota (`useState`, sem interface): `{ numero, serie, dataEmissao, dataEntrada, chaveAcesso, naturezaOperacao }`. Datas em formato ISO `yyyy-MM-dd` (do `<input type="date">`). Valores padrão: `serie="1"`, `naturezaOperacao="Compra para comercialização"`, `dataEntrada=hoje`. O botão de importar XML (`<input type="file" accept=".xml">`) **não tem handler**.

### 3.10 Usuário autenticado — `context/AuthContext.tsx`

```ts
interface AuthContextType {
  isAuthenticated: boolean;
  carregando: boolean;                                    // validando o token em /auth/me
  user: Usuario | null;                                   // { id, nome, email }
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;
}
```

Persistência: o token fica em `sessionStorage["wc_token"]`. O usuário **não** é persistido: é carregado de `GET /auth/me` ao abrir a página. *(Na versão original, `login` era síncrono e o usuário ficava em `localStorage["wc_user"]`, sem token.)*

### 3.11 Dados mock dos cards do Home

| Componente | Estrutura do mock | Observação |
|---|---|---|
| `EstoqueCard` | `{ id, nome, ncm, quantidade, critico: boolean }` | Usa `critico` (boolean), diferente do `status` de `Estoque` |
| `FiscalCard` | `{ id, numero, cliente, valor: string, hora }` | `valor` é **string já formatada** (`"R$ 1.250,00"`) |
| `VendasCard` | — | Só um campo de busca por CNPJ (`id="cnpj"`) e um botão, sem estado |

---

## 4. Modelo sugerido para o back (contrato proposto)

Estas são **propostas** derivadas do front; não existem no código. A ideia é o back expor um modelo único e o front deixar de duplicar tipos por página.

### 4.1 Entidades

| Entidade | Campos principais | Substitui no front |
|---|---|---|
| `Usuario` | `id`, `nome`, `email`, senha (hash), `papel` | `AuthContext.user` |
| `Cliente` | `id`, `tipo` (PF/PJ), `documento` (só dígitos), `nome`, `email`, `telefone`, `cidade`, `uf`, `ativo` | `Cliente` |
| `Produto` | `id`, `codigo`, `nome`, `ncm`, `estoqueAtual`, `estoqueMinimo`, `precoCusto`, `precoVenda` | `Produto`, mocks do Home |
| `Fornecedor` | `id`, `cnpj`, `razaoSocial`, `ie`, `uf` | `fornecedor` em `NotaEntrada` |
| `Venda` | `id`, `tipoDocumento`, `modalidade`, `clienteId?`, `metodoPagamento`, `bandeira?`, `destinoDinheiro?`, `desconto`, `subtotal`, `total`, `status` (`orcamento`/`finalizada`) | estado do `PDV` |
| `VendaItem` | `vendaId`, `produtoId`, `quantidade`, `valorUnitario` | `ItemCarrinho` (sem campos de UI) |
| `NotaFiscal` | `id`, `tipo` (`NFE`/`NFCE`/`DEVOLUCAO`/`ENTRADA`), `numero`, `serie`, `chaveAcesso`, `clienteId?`, `fornecedorId?`, `valorTotal`, `emitidaEm`, `status`, `vendaId?`, `notaReferenciadaId?` | `notasFiscais`, `NotaParaCancelar`, `notasReferencia`, `notasEmitidas` |
| `NotaFiscalItem` | `notaId`, `produtoId`, `descricao`, `ncm`, `cfop`, `quantidade`, `valorUnitario` | `ItemDevolucao`, `ItemEntrada` |
| `EventoFiscal` | `id`, `notaId`, `tipo` (`CANCELAMENTO`), `justificativa`, `protocolo`, `registradoEm` | fluxo de `Cancelamento` |
| `Inutilizacao` | `id`, `serie`, `numeroInicial`, `numeroFinal`, `justificativa`, `protocolo`, `registradaEm` | `Inutilizacao` |

### 4.2 Enums

| Enum | Valores |
|---|---|
| `TipoPessoa` | `PF`, `PJ` |
| `TipoDocumento` | `nfe`, `nfce` |
| `Modalidade` | `fisica`, `virtual` |
| `MetodoPagamento` | `dinheiro`, `credito`, `debito`, `pix`, `boleto` |
| `BandeiraCartao` | `mastercard`, `visa`, `elo`, `outros` |
| `DestinoDinheiro` | `banco`, `cofre` |
| `StatusNota` | `autorizada`, `cancelada`, `processando` (`inutilizada` para faixas) |
| `StatusEstoque` | `ok`, `atencao`, `baixo`, `critico` (ver regra na seção 5) |

### 4.3 Endpoints sugeridos, por tela

| Tela | Operação | Endpoint sugerido |
|---|---|---|
| Login | Autenticar | `POST /auth/login` → `{ token, usuario }` |
| Layout | Sessão atual | `GET /auth/me` |
| Home | Resumos | `GET /dashboard` (ou reaproveitar as listagens abaixo com filtros) |
| Clientes | Listar / buscar | `GET /clientes?tipo=&busca=` |
| Clientes | Criar / editar / excluir | `POST /clientes`, `PUT /clientes/:id`, `DELETE /clientes/:id` |
| Estoque | Listar / buscar | `GET /produtos?busca=` |
| Estoque | Criar / editar / excluir | `POST /produtos`, `PUT /produtos/:id`, `DELETE /produtos/:id` |
| Estoque | Indicadores | `GET /produtos/resumo` (total, baixo, valor em estoque, críticos) |
| PDV | Buscar cliente por documento | `GET /clientes?documento=` |
| PDV | Buscar produto | `GET /produtos?busca=` |
| PDV | Finalizar venda + emitir documento | `POST /vendas` (e emissão fiscal em seguida) |
| PDV | Salvar orçamento | `POST /vendas` com `status=orcamento` |
| Painel Fiscal | Listar notas | `GET /notas?status=&de=&ate=` |
| Painel Fiscal | Baixar XML / PDF | `GET /notas/:id/xml`, `GET /notas/:id/pdf` |
| Painel Fiscal | Enviar lote ao contador | `POST /notas/lote-contador` |
| Painel Fiscal | Exportar relatório | `GET /notas/relatorio` |
| Cancelamento | Cancelar | `POST /notas/:id/cancelamento` `{ justificativa }` |
| Inutilização | Listar / inutilizar | `GET /inutilizacoes`, `POST /inutilizacoes` |
| Nota de Devolução | Buscar nota e itens | `GET /notas?status=autorizada`, `GET /notas/:id/itens` |
| Nota de Devolução | Emitir | `POST /notas/devolucao` `{ notaReferenciadaId, itens[], motivo }` |
| Nota de Entrada | Registrar | `POST /notas/entrada` |
| Nota de Entrada | Importar XML | `POST /notas/entrada/importar-xml` (multipart) |

---

## 5. Regras de negócio hoje implementadas no front

O back deve reproduzir (ou passar a ser a fonte da verdade de) cada regra abaixo.

| Regra | Onde | Detalhe |
|---|---|---|
| **Status de estoque** | `Estoque.tsx` › `salvarProduto` | `estoque <= minimo × 0,4` → `critico`; `< minimo × 0,7` → `baixo`; `< minimo` → `atencao`; caso contrário `ok`. Calculado ao **salvar**; os mocks iniciais não seguem esta regra (ex.: Filtro de Óleo 5/15 está como `baixo`, mas pela regra seria `critico`) |
| **Subtotal e total da venda** | `PDV.tsx` | `subtotal = Σ quantidade × valorUnitario`; `total = subtotal − desconto`. Desconto em R$, mínimo 0 (não há teto: pode ser maior que o subtotal e gerar total negativo) |
| **Preço editável no PDV** | `PDV.tsx` | Aceita vírgula (`replace(",", ".")`); valor inválido mantém o preço anterior |
| **Quantidade mínima** | `PDV.tsx` | `< 1` é ignorado |
| **NF-e exige cliente identificado** | `PDV.tsx` | Ao escolher `nfe`, `consumidorIdentificado = true` e o rótulo é "CNPJ/CPF *". Ao escolher `nfce`, `consumidorIdentificado = false` e o CPF é opcional |
| **Bandeira só para cartão** | `PDV.tsx` | Visível quando `metodoPagamento` é `credito` ou `debito` |
| **Destino do dinheiro só em espécie** | `PDV.tsx` | Visível apenas com `metodoPagamento = "dinheiro"` |
| **Impressão de cupom** | `PDV.tsx` | Botão "Imprimir Cupom (NTH)" só aparece com `nfce` |
| **Prazo de cancelamento** | `Cancelamento.tsx` | Texto informa "até 24 horas após a autorização". **A tela não valida o prazo**: só lista `status === "autorizada"`. O back deve validar |
| **Justificativa mínima** | `Cancelamento.tsx`, `Inutilizacao.tsx` | ≥ 15 caracteres após `trim` |
| **Faixa de inutilização** | `Inutilizacao.tsx` | `numeroFinal >= numeroInicial`; quantidade = `final − inicial + 1`. Irreversível |
| **Limite de devolução** | `NotaDevolucao.tsx` | `0 ≤ qtdDevolver ≤ qtdOriginal`. Emitir exige ao menos um item e total > 0. O motivo **não é obrigatório** |
| **Total da devolução** | `NotaDevolucao.tsx` | `Σ qtdDevolver × valorUnitario` |
| **Total da nota de entrada** | `NotaEntrada.tsx` | `Σ quantidade × valorUnitario`. Salvar exige `numero` e `cnpj` do fornecedor |
| **Efeito da entrada no estoque** | `NotaEntrada.tsx` | A mensagem de sucesso diz que os itens entram no estoque automaticamente. **Nada disso é feito no front**: o back precisa somar as quantidades |
| **Autenticação** | `AuthContext.tsx` | *Original:* credencial fixa `admin@autopecas.com` / `admin123`. **Já substituída** por login via API (ver seção 8) |

---

## 6. Componentes e páginas

Formato: **nome** — arquivo — responsabilidade — props — estado — dados — pontos de integração.

### 6.1 Infraestrutura

| Componente | Arquivo | Responsabilidade | Props | Estado / hooks |
|---|---|---|---|---|
| `App` | `app/App.tsx` | Raiz: `AuthProvider` envolvendo `RouterProvider` | — | — |
| `router` | `app/routes.ts` | Tabela de rotas | — | — |
| `AuthProvider` | `context/AuthContext.tsx` | Provê `isAuthenticated`, `user`, `login`, `logout` | `{ children: ReactNode }` | `user` (inicializado de `localStorage`) |
| `useAuth` | `context/AuthContext.tsx` | Hook de acesso ao contexto (lança erro fora do provider) | — | `useContext` |
| `Layout` | `components/Layout.tsx` | Casca autenticada; bloqueia scroll do `body` com o menu aberto | — | `sidebarOpen`; usa `useAuth` |
| `Sidebar` | `components/Sidebar.tsx` | Menu principal + submenu Fiscal + usuário + sair | `SidebarProps { isOpen: boolean; onClose: () => void }` | `fiscalAberto`; usa `useAuth`, `useLocation`, `useNavigate` |
| `MobileHeader` | `components/MobileHeader.tsx` | Barra superior com botão de menu (só mobile) | `MobileHeaderProps { onMenuClick: () => void }` | — |
| `ImageWithFallback` | `components/figma/ImageWithFallback.tsx` | `<img>` com imagem de erro (não usado) | `React.ImgHTMLAttributes<HTMLImageElement>` | `didError` |

Hierarquia autenticada: `Layout` → (`Sidebar`, `MobileHeader`, `<Outlet/>` com a página da rota).

### 6.2 Cards do Home

| Componente | Arquivo | Dados | Integração futura |
|---|---|---|---|
| `Home` | `pages/Home.tsx` | Data por extenso (`toLocaleDateString('pt-BR')`); compõe os 3 cards | — |
| `VendasCard` | `components/VendasCard.tsx` | Nenhum (busca de cliente por CNPJ + "Novo Pedido/Nota Fiscal") | Buscar cliente; navegar para `/pdv` (o botão ainda não navega) |
| `EstoqueCard` | `components/EstoqueCard.tsx` | `produtosBaixoEstoque` (5 itens) | `GET /produtos?status=baixo` |
| `FiscalCard` | `components/FiscalCard.tsx` | `notasEmitidas` (4 itens) | Últimas notas do dia, XML/PDF, lote ao contador |

### 6.3 Páginas

| Página | Estado local (`useState`) | Dados | Ações que precisam do back |
|---|---|---|---|
| `Login` | `email`, `senha`, `mostrarSenha`, `erro`, `carregando` | — | `login()` (hoje síncrono; `await` de 600 ms é simulação) |
| `PDV` | ver 3.4 e 3.3 | `carrinho` inicial com 2 itens, `BANDEIRAS` | Adicionar/buscar produto, identificar cliente, Finalizar Venda, Imprimir Cupom, Salvar Orçamento (os 4 botões e a busca **não têm handler**) |
| `Clientes` | `filtroTipo`, `busca` | `clientesData` (8) | Novo, Editar, Excluir (**sem handler**); estatísticas calculadas no cliente |
| `Estoque` | `produtos`, `busca`, `produtoEditando` | `produtosIniciais` (8), `estatisticas` **fixas** (847, 23, R$ 145.890, 8) | Salvar edição (só memória), Novo Produto e Excluir (**sem handler**) |
| `ModalEditarProduto` (interno de `Estoque.tsx`) | `form: Produto` | — | Props: `{ produto: Produto; onSalvar: (p: Produto) => void; onFechar: () => void }` |
| `PainelFiscal` | — | `notasFiscais` (8), `estatisticas` **fixas** | Emitir NF-e, Enviar Lote, Exportar, Filtrar, XML, PDF (**sem handler**) |
| `Cancelamento` | `busca`, `notaSelecionada`, `justificativa`, `enviando`, `sucesso` | `notas` (4) | `handleCancelar` (simulado 1,2 s) |
| `Inutilizacao` | `form`, `enviando`, `sucesso`, `erro` | `inutilizacoes` (2) | `handleSubmit` (simulado 1,2 s); `erro` nunca é preenchido |
| `NotaDevolucao` | `busca`, `notaRef`, `itens`, `motivo`, `enviando`, `sucesso` | `notasReferencia` (3), `itensMock` (2) | `handleEmitir` (simulado 1,4 s); "Adicionar item" **sem handler** |
| `NotaEntrada` | `fornecedor`, `nota`, `itens`, `enviando`, `sucesso` | 1 item inicial, `cfopOpcoes` | `handleSalvar` (simulado 1,2 s); Importar XML e Cancelar **sem handler** |
| `FileIcon` (interno de `PDV.tsx`) | — | — | Props: `{ size: number }`; SVG do ícone de boleto |

---

## 7. Inconsistências para alinhar antes de integrar

Pontos em que o front e o futuro modelo do back podem divergir:

1. **Nomes diferentes para o mesmo conceito.**
   - Documento do cliente: `documento` (Clientes) × `cnpj` (PainelFiscal, NotaDevolucao, NotaEntrada).
   - Quantidade: `quantidade` (PDV, NotaEntrada) × `estoque` (Produto) × `quantidade` (EstoqueCard).
   - Preço: `valorUnitario` (itens) × `venda`/`custo` (Produto).
   - Descrição do item: `nome` (PDV, Devolução) × `descricao` (Entrada).
   - Faixa de inutilização: `numeroInicial`/`numeroFinal` × `inicio`/`fim`.
2. **Formatação misturada com dado.** Documentos, telefones e NCM são guardados **formatados**; datas em `dd/MM/yyyy` (exceto `NotaEntrada`, em ISO); `FiscalCard.valor` é texto (`"R$ 1.250,00"`). Sugestão: back envia dado cru (só dígitos, ISO 8601, número) e o front formata na exibição.
3. **Dinheiro em `number` (float).** Se o back usar centavos inteiros ou `decimal`, o front precisa converter ao ler e ao enviar.
4. **Status como `string` solta.** `Produto.status` e o status das notas do painel não são union types; um valor inesperado do back cai no ramo padrão do badge ("OK" em Estoque, "Processando" em PainelFiscal).
5. **Nota fiscal sem itens nem tipo.** `notasFiscais` não diz se é NF-e ou NFC-e, e não traz itens; a devolução usa `itensMock` fixos. O back precisa devolver os itens reais da nota.
6. **Relacionamentos por texto.** Notas referenciam o cliente pelo nome (`cliente: string`) e itens referenciam produto por `codigo`. Usar `clienteId` e `produtoId`.
7. **IDs.** `id: number` nas listas; itens novos em `NotaEntrada` usam `Date.now()` como id temporário. Definir se o back gera `id` numérico ou UUID e não enviar o `id` temporário.
8. **Campos só de UI dentro do modelo.** `ItemCarrinho.editandoPreco` e `precoTemp` não devem ir no payload.
9. **Indicadores fixos.** Estatísticas de `Estoque` e `PainelFiscal` são strings digitadas no código e não batem com os mocks (ex.: "Emitidas Hoje: 4" e "Autorizadas: 6"). Devem vir do back.
10. **Dados mock discordam entre telas.** Pastilha de Freio aparece com 45 un. em `Estoque` e 3 un. em `EstoqueCard`.
11. **Handlers ausentes.** Vários botões (Novo Cliente, Excluir, Finalizar Venda, Emitir NF-e, Importar XML etc.) só têm visual. A seção 6.3 lista quais.
12. **Segurança (original — já tratada na seção 8).** Havia credencial fixa no código e exibida na tela, e sessão baseada só em `localStorage` sem token. Agora o token vem do back, fica só em `sessionStorage` e o `Layout` valida a sessão via `GET /auth/me`.
13. **Regra de prazo (24 h) apenas descrita em texto.** Validar no back com a data/hora de autorização da nota.
14. **`fornecedor.ie` sem campo na tela.** Existe no estado de `NotaEntrada`, mas não é editável.

---

## 8. Base de integração já criada

As etapas 1 a 3 do plano de integração **já estão feitas**. As seções 3 a 7 descrevem o estado *original* das telas; as telas (exceto Login e Layout) ainda usam os mocks.

| O que | Onde | Observação |
|---|---|---|
| Tipos compartilhados | `src/app/types/index.ts` | Modelo da seção 4: entidades, enums e payloads (`ClienteInput`, `ProdutoInput`, `VendaInput`, `DevolucaoInput`, `EntradaInput`, `InutilizacaoInput`). Dinheiro em `number`, datas ISO, documento/telefone/NCM sem formatação |
| Cliente HTTP | `src/app/services/http.ts` | `request<T>()` (JSON) e `requestBlob()` (XML/PDF). Envia `Authorization: Bearer <token>`. `ApiError` com `status` e `message` (lida do campo `message` do JSON de erro) |
| Token | `src/app/services/tokenStorage.ts` | **Somente `sessionStorage`** (chave `wc_token`), nunca `localStorage`. Todos os acessos em `try/catch` |
| Serviços | `src/app/services/{auth,clientes,produtos,vendas,notas,inutilizacoes}.ts` | Um objeto por recurso, com os endpoints da seção 4.3. Reexportados em `services/index.ts` |
| Auth assíncrono | `src/app/context/AuthContext.tsx` | `login()` retorna `Promise<boolean>`; ao carregar a página, se há token, valida em `GET /auth/me` antes de renderizar (`carregando`). Uma resposta 401 limpa o token e desloga. Remove a chave antiga `wc_user` do `localStorage` |
| Layout / Login | `components/Layout.tsx`, `pages/Login.tsx` | `Layout` aguarda `carregando`. `Login` não tem mais credencial fixa, e-mail pré-preenchido, dica de demo nem atraso simulado |
| URL da API | `src/environments/environment*.ts` | `apiUrl` (padrão `/api` em produção; `http://localhost:3000` em desenvolvimento). *(No React: `VITE_API_URL` em `.env.example`.)* |

Contratos que o back precisa respeitar para esta base funcionar:

- `POST /auth/login` recebe `{ email, senha }` e responde `{ token, usuario: { id, nome, email } }`.
- `GET /auth/me` responde o `Usuario` do token (401 se inválido).
- Erros respondem JSON com `{ "message": "..." }`.
- Os campos usam os nomes de `types/index.ts` (por exemplo `estoqueAtual`, não `estoque`).

> Sem o back no ar, o login falha em produção. Em dev (`ng serve`), `AuthApiService` tem um login mock: `admin@autopecas.com` / `admin123` (desligue com `mockAuth: false` em `src/environments/environment.development.ts`).

## 9. Checklist do que falta

1. Substituir os mocks tela a tela pelos serviços, na ordem: Clientes → Estoque → PDV → Painel Fiscal → Cancelamento → Inutilização → Devolução → Entrada → cards do Home.
2. Trocar as `interface` locais de cada página pelos tipos de `src/app/types`. Como os nomes de campo mudaram (seção 7, item 1), ajustar as telas junto.
3. Formatar na exibição documento, telefone, NCM e datas (o back envia dado cru).
4. Conectar os botões sem handler (seção 6.3) e tratar estados de carregamento e erro (hoje só `enviando`/`sucesso`).
5. Remover os `setTimeout` que simulam a SEFAZ e usar a resposta real (`protocolo`, `status`).
6. Tratar `ApiError` nas telas (mostrar `error.message`).
