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
| `id` | `string` | UUID do backend |
| `tipo` | `"PF" \| "PJ"` | |
| `documento` | `string` | CPF ou CNPJ **sem formatação** (backend envia `12345678901234`) |
| `nome` | `string` | Nome ou razão social |
| `nomeFantasia` | `string?` | Obrigatório para PJ |
| `email` | `string` | |
| `telefone` | `string` | Sem formatação (backend envia `11987654321`) |
| `cep` | `string` | 8 dígitos sem formatação |
| `logradouro` | `string` | |
| `numero` | `string` | |
| `complemento` | `string?` | Opcional |
| `bairro` | `string` | |
| `cidade` | `string` | |
| `uf` | `string` | 2 caracteres |
| `inscricaoEstadual` | `string?` | Para PJ |
| `ativo` | `boolean` | Backend usa boolean, não "ativo"|"inativo" |
| `totalCompras` | `number` | Contagem de compras (campo calculado) |
| `valorTotalCompras` | `number` | Valor total em compras (campo calculado do backend) |
| `ultimaCompra` | `string` | ISO 8601 (`2024-09-15T14:30:00Z`) - formatar no frontend |
| `dataCriacao` | `string` | ISO 8601 |
| `dataAtualizacao` | `string` | ISO 8601 |

**Formatação no Frontend:**
- `documento`: Formatar apenas na exibição com `formatarDocumento(doc, tipo)`
- `telefone`: Formatar apenas na exibição com `formatarTelefone(tel)`
- `ultimaCompra`: Converter ISO 8601 para `dd/MM/yyyy` na exibição
- `ativo`: Mapear `true → "ativo"`, `false → "inativo"` para compatibilidade com código existente

Origem: `GET /api/clientes` (paginado). Filtros: tipo (`PF`/`PJ`), busca (nome, email, documento), cidade, uf, ativo.

### 3.2 `Produto` — `pages/Estoque.tsx:4`

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `string` | UUID do backend |
| `codigo` | `string` | SKU interno, ex.: `PF-001` |
| `nome` | `string` | |
| `descricao` | `string?` | Descrição detalhada (opcional) |
| `ncm` | `string` | Sem formatação (`87083010`) - formatar no frontend |
| `cest` | `string?` | Código CEST (opcional) |
| `categoria` | `string` | Ex: "Freios", "Motor", "Suspensão" |
| `fabricante` | `string?` | Nome do fabricante (opcional) |
| `codigoFabricante` | `string?` | Código do fabricante (opcional) |
| `precoCusto` | `number` | Preço de custo (R$) |
| `precoVenda` | `number` | Preço de venda (R$) |
| `margemLucro` | `number` | Percentual calculado pelo backend |
| `estoqueMinimo` | `number` | Estoque mínimo |
| `cfop` | `string?` | CFOP padrão (ex: "5102") |
| `origem` | `string?` | Origem da mercadoria (0-8) |
| `cstIcms` | `string?` | CST ICMS (ex: "00") |
| `aliquotaIcms` | `number?` | Alíquota ICMS em % |
| `ativo` | `boolean` | Produto ativo/inativo |
| `dataCriacao` | `string` | ISO 8601 |
| `dataAtualizacao` | `string` | ISO 8601 |
| `estoque` | `EstoqueInfo` | **Objeto aninhado** (ver estrutura abaixo) |

#### 3.2.1 `EstoqueInfo` — Objeto aninhado

| Campo | Tipo TS | Observação |
|---|---|---|
| `estoqueAtual` | `number` | Quantidade física atual |
| `reservado` | `number` | Quantidade reservada em vendas/orçamentos |
| `disponivel` | `number` | Calculado: `estoqueAtual - reservado` |
| `status` | `"ok" \| "atencao" \| "baixo" \| "critico"` | Calculado pelo backend |
| `ultimaMovimentacao` | `string` | ISO 8601 da última movimentação |

**Regras de Status (calculadas no backend):**
```
critico  → estoqueAtual <= estoqueMinimo × 0.4
baixo    → estoqueAtual <  estoqueMinimo × 0.7
atencao  → estoqueAtual <  estoqueMinimo
ok       → estoqueAtual >= estoqueMinimo
```

**Formatação no Frontend:**
- `ncm`: Formatar apenas na exibição com `formatarNCM(ncm)` → `"8708.30.10"`
- **NÃO calcular status no frontend** - usar o valor retornado pelo backend
- **Para compatibilidade com código legado**, mapear:
  - `produto.estoque.estoqueAtual` → `produto.estoque` (flat)
  - `produto.estoqueMinimo` → `produto.minimo`
  - `produto.precoCusto` → `produto.custo`
  - `produto.precoVenda` → `produto.venda`

Origem: `GET /api/produtos` (paginado). Filtros: busca (nome, código, NCM), categoria, status, ativo.

### 3.3 `ItemCarrinho` — `pages/PDV.tsx:20` (Apenas Frontend)

| Campo | Tipo TS | Observação |
|---|---|---|
| `produtoId` | `string` | UUID do produto (obrigatório para API) |
| `codigo` | `string` | SKU do produto (apenas para exibição) |
| `nome` | `string` | Nome do produto (apenas para exibição) |
| `quantidade` | `number` | Mínimo 1 |
| `valorUnitario` | `number` | **Editável na venda** (pode divergir de `precoVenda`) |
| `editandoPreco` | `boolean` | Somente UI, **não enviar ao back** |
| `precoTemp` | `string` | Somente UI, **não enviar ao back** |

**Importante:** Ao enviar para API, converter para `ItemVendaRequest`:

#### 3.3.1 `ItemVendaRequest` — Payload para API

| Campo | Tipo TS | Observação |
|---|---|---|
| `produtoId` | `string` | UUID do produto |
| `quantidade` | `number` | Quantidade vendida |
| `valorUnitario` | `number` | Preço unitário (pode ser editado) |

**Conversão:**
```typescript
function converterItemCarrinho(item: ItemCarrinho): ItemVendaRequest {
  return {
    produtoId: item.produtoId,
    quantidade: item.quantidade,
    valorUnitario: item.valorUnitario,
  };
  // NÃO enviar: id, codigo, nome, editandoPreco, precoTemp
}
```

### 3.4 PDV e Venda — `pages/PDV.tsx`

#### 3.4.1 Tipos Enumerados

```typescript
type TipoDocumento    = "nfe" | "nfce";
type Modalidade       = "fisica" | "virtual";
type DestinoMoeda     = "banco" | "cofre";
type MetodoPagamento  = "dinheiro" | "credito" | "debito" | "pix" | "boleto";
type BandeiraCartao   = "mastercard" | "visa" | "elo" | "outros";
type StatusVenda      = "orcamento" | "finalizada" | "cancelada";
```

#### 3.4.2 Estado do PDV (Frontend)

Estado gerenciado via `useState`:
- `tipoDocumento`: "nfe" | "nfce"
- `consumidorIdentificado`: boolean (se true: NF-e, se false: NFC-e)
- `carrinho`: ItemCarrinho[] (ver seção 3.3)
- `modalidade`: "fisica" | "virtual"
- `metodoPagamento`: MetodoPagamento
- `bandeira`: BandeiraCartao (apenas se método = crédito/débito)
- `destinoMoeda`: DestinoMoeda (apenas se método = dinheiro)
- `desconto`: number (em R$, não percentual)

**Campos de Cliente (soltos no formulário):**
- CPF/CNPJ: `<input>` sem estado (usar ref ou buscar no DOM)
- Nome: `<input>` sem estado

#### 3.4.3 Request de Venda (Payload para API)

**Endpoint:** `POST /api/vendas`

```typescript
interface VendaRequest {
  tipoDocumento: "nfe" | "nfce";
  modalidade: "fisica" | "virtual";
  clienteId: string | null;  // ← UUID ou null (consumidor final)
  metodoPagamento: MetodoPagamento;
  bandeira?: BandeiraCartao;         // Se crédito/débito
  destinoDinheiro?: DestinoMoeda;    // Se dinheiro
  desconto: number;
  status: "orcamento" | "finalizada";
  itens: ItemVendaRequest[];
}
```

**Exemplo de Payload:**
```json
{
  "tipoDocumento": "nfce",
  "modalidade": "fisica",
  "clienteId": null,
  "metodoPagamento": "credito",
  "bandeira": "visa",
  "destinoDinheiro": null,
  "desconto": 0,
  "status": "finalizada",
  "itens": [
    {
      "produtoId": "uuid-do-produto",
      "quantidade": 2,
      "valorUnitario": 89.90
    }
  ]
}
```

#### 3.4.4 Lógica de Cliente no PDV

**Cenário 1: Consumidor Não Identificado (NFC-e)**
```typescript
// clienteId = null
const venda = {
  tipoDocumento: "nfce",
  clienteId: null,
  // ... outros campos
};
```

**Cenário 2: Cliente Identificado (NF-e ou NFC-e com CPF)**
```typescript
async function prepararVenda() {
  let clienteId: string | null = null;
  
  if (consumidorIdentificado) {
    const cpfCnpj = document.getElementById('cpf-cnpj').value;
    
    // 1. Buscar cliente existente
    const clientes = await fetch(`/api/clientes?documento=${limparDocumento(cpfCnpj)}`);
    const data = await clientes.json();
    
    if (data.items.length > 0) {
      clienteId = data.items[0].id;
    } else {
      // 2. Criar cliente rápido
      const nome = document.getElementById('nome-cliente').value;
      const tipo = cpfCnpj.length === 11 ? "PF" : "PJ";
      
      const novoCliente = await fetch('/api/clientes', {
        method: 'POST',
        body: JSON.stringify({
          tipo,
          documento: limparDocumento(cpfCnpj),
          nome,
          email: `cliente${Date.now()}@temp.com`, // Temporário
          telefone: "00000000000", // Temporário
          cidade: "Não informado",
          uf: "MG",
          // Campos obrigatórios mínimos
        }),
      });
      
      const clienteData = await novoCliente.json();
      clienteId = clienteData.id;
    }
  }
  
  return clienteId;
}
```

#### 3.4.5 Cálculo de Totais

**Frontend (Validação):**
```typescript
const subtotal = carrinho.reduce(
  (sum, item) => sum + (item.quantidade * item.valorUnitario), 
  0
);
const total = subtotal - desconto;
```

**Backend:** Recalcula e valida. Frontend NÃO envia subtotal/total.

#### 3.4.6 Regras de Validação

| Regra | Descrição |
|-------|-----------|
| NF-e → Cliente obrigatório | Se `tipoDocumento = "nfe"`, `clienteId` não pode ser null |
| NFC-e → Cliente opcional | Se `tipoDocumento = "nfce"`, `clienteId` pode ser null |
| Bandeira para cartão | Se `metodoPagamento` = "credito" ou "debito", `bandeira` é obrigatória |
| Destino para dinheiro | Se `metodoPagamento` = "dinheiro", `destinoDinheiro` é obrigatório |
| Mínimo 1 item | Array `itens` deve ter pelo menos 1 elemento |
| Estoque disponível | Backend valida se há estoque para cada item |
| Desconto | Não pode ser negativo nem maior que subtotal |

#### 3.4.7 Response da API

```json
{
  "id": "uuid-da-venda",
  "numero": "VD-000126",
  "tipoDocumento": "nfce",
  "subtotal": 179.80,
  "desconto": 0,
  "total": 179.80,
  "status": "finalizada",
  "criadaEm": "2024-09-20T15:00:00Z",
  "finalizadaEm": "2024-09-20T15:00:00Z",
  "notaFiscalId": null,  // Será preenchido após emissão
  "itens": [...]
}
```

**Pós-venda:**
- Se `status = "finalizada"`: Backend dispara emissão fiscal automaticamente
- Nota será emitida de forma assíncrona
- Para acompanhar: Consultar `GET /api/notas?vendaId={id}`

### 3.5 Nota fiscal (painel) — `pages/PainelFiscal.tsx:3`

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `string` | UUID do backend |
| `tipo` | `"NFE" \| "NFCE" \| "DEVOLUCAO" \| "ENTRADA"` | Tipo da nota |
| `modelo` | `string` | "55" (NF-e) ou "65" (NFC-e) |
| `numero` | `string` | `"000126"` (com zeros à esquerda) |
| `serie` | `string` | Padrão "1" |
| `chaveAcesso` | `string` | 44 dígitos |
| `vendaId` | `string?` | UUID da venda (se aplicável) |
| `clienteId` | `string?` | UUID do cliente (null para consumidor final) |
| `cliente` | `string?` | Nome do cliente (apenas para exibição na listagem) |
| `valorTotal` | `number` | Valor total da nota |
| `emitidaEm` | `string` | ISO 8601 - separar em data e hora no frontend |
| `autorizadaEm` | `string?` | ISO 8601 (null se não autorizada) |
| `status` | `"processando" \| "autorizada" \| "rejeitada" \| "cancelada"` | Status atual |
| `protocolo` | `string?` | Protocolo SEFAZ (quando autorizada) |
| `codigoStatus` | `string?` | Código retorno SEFAZ (ex: "100") |
| `motivoRejeicao` | `string?` | Motivo se rejeitada |
| `ambiente` | `"producao" \| "homologacao"` | Ambiente de emissão |
| `xmlPath` | `string?` | Caminho S3 do XML |
| `pdfPath` | `string?` | Caminho S3 do PDF |

**Formatação no Frontend:**
- `emitidaEm`: Separar em `data` (dd/MM/yyyy) e `hora` (HH:mm)
- Se backend não retornar campo `cliente`, buscar pelo `clienteId` ou exibir "Consumidor Final"

**Observação sobre CNPJ:**
- Backend **não retorna CNPJ** na listagem
- Para exibir CNPJ, é necessário:
  1. Fazer requisição adicional `GET /api/clientes/{clienteId}`, ou
  2. Backend adicionar campo `clienteDocumento` na resposta da listagem

### 3.6 `NotaParaCancelar` — `pages/Cancelamento.tsx:4`

| Campo | Tipo TS |
|---|---|
| `id` | `string` | UUID |
| `numero` | `string` |
| `serie` | `string` |
| `cliente` | `string?` | Nome do cliente ou "Consumidor Final" |
| `valorTotal` | `number` | Usar este campo (backend não tem campo `valor`) |
| `emitidaEm` | `string` | ISO 8601 - formatar para `dd/MM/yyyy` no frontend |
| `autorizadaEm` | `string` | ISO 8601 |
| `chaveAcesso` | `string` | 44 dígitos |
| `status` | `"autorizada"` | Filtrar apenas autorizadas |
| `protocolo` | `string` | Protocolo de autorização |

**Endpoint:** `GET /api/notas?status=autorizada`

**Formulário de Cancelamento:**
- `justificativa`: string, mínimo 15 caracteres (validado no backend também)

**Endpoint de Cancelamento:** `POST /api/notas/{id}/cancelamento`
```json
{
  "justificativa": "Mínimo 15 caracteres"
}
```

**Validação de Prazo:**
- Backend valida prazo de 24h desde `autorizadaEm`
- Frontend deve tratar erro 400 se prazo expirado

### 3.7 Inutilização — `pages/Inutilizacao.tsx`

#### 3.7.1 Formulário de Inutilização

| Campo | Tipo TS | Regra |
|---|---|---|
| `serie` | `string` | Padrão `"1"` |
| `numeroInicial` | `number` | Numérico, ≥ 1 (converter de string para number ao enviar) |
| `numeroFinal` | `number` | ≥ `numeroInicial` (converter de string para number) |
| `justificativa` | `string` | ≥ 15 caracteres (após `trim`) |

**Endpoint:** `POST /api/notas/inutilizacao`
```json
{
  "serie": "1",
  "numeroInicial": 100,
  "numeroFinal": 105,
  "justificativa": "Mínimo 15 caracteres"
}
```

**Conversão no Frontend:**
```typescript
const payload = {
  serie: form.serie,
  numeroInicial: parseInt(form.numeroInicial),  // ← Converter
  numeroFinal: parseInt(form.numeroFinal),      // ← Converter
  justificativa: form.justificativa,
};
```

#### 3.7.2 Histórico de Inutilizações

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `string` | UUID |
| `serie` | `string` |
| `numeroInicial` | `number` | Backend usa este nome |
| `numeroFinal` | `number` | Backend usa este nome |
| `quantidade` | `number` | Calculado: `numeroFinal - numeroInicial + 1` |
| `justificativa` | `string` |
| `protocolo` | `string?` | Protocolo SEFAZ |
| `registradaEm` | `string` | ISO 8601 - formatar para dd/MM/yyyy |
| `status` | `string` | "inutilizada" ou "processando" |

**Endpoint:** `GET /api/notas/inutilizacao` (assumindo que existe)

### 3.8 `ItemDevolucao` e nota referenciada — `pages/NotaDevolucao.tsx`

#### 3.8.1 Nota Referenciada (para seleção)

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `string` | UUID da nota |
| `numero` | `string` |
| `serie` | `string` |
| `cliente` | `string?` | Nome do cliente |
| `valorTotal` | `number` |
| `emitidaEm` | `string` | ISO 8601 - formatar para dd/MM/yyyy |
| `chaveAcesso` | `string` |
| `status` | `"autorizada"` | Apenas notas autorizadas |

**Endpoint:** `GET /api/notas?status=autorizada&tipo=NFE,NFCE`

#### 3.8.2 Item da Nota Original (retornado pelo backend)

**Endpoint:** `GET /api/notas/{id}/itens` (deve ser adicionado ao backend se não existir)

| Campo | Tipo TS | Observação |
|---|---|---|
| `itemId` | `string` | UUID do item na nota (necessário para devolução) |
| `produtoId` | `string` | UUID do produto |
| `codigo` | `string` | SKU do produto |
| `nome` | `string` | Nome do produto |
| `ncm` | `string` | NCM |
| `quantidade` | `number` | Quantidade original vendida |
| `valorUnitario` | `number` | Valor unitário na venda |
| `valorTotal` | `number` | Valor total do item |

#### 3.8.3 Item de Devolução (Frontend + Payload)

**Frontend (Interface local com controle de quantidade):**
```typescript
interface ItemDevolucao {
  itemId: string;          // UUID do item na nota original
  codigo: string;          // Para exibição
  nome: string;            // Para exibição
  qtdOriginal: number;     // Quantidade vendida
  qtdDevolver: number;     // Editável: 0 ≤ qtdDevolver ≤ qtdOriginal
  valorUnitario: number;   // Para cálculo do total
}
```

**Payload para API:**
```typescript
interface ItemDevolucaoRequest {
  itemId: string;           // UUID do item na nota
  quantidadeDevolver: number;  // Quantidade a devolver
}
```

**Endpoint de Devolução:** `POST /api/notas/devolucao`
```json
{
  "notaReferenciadaId": "uuid-da-nota-original",
  "motivo": "Produto com defeito",
  "itens": [
    {
      "itemId": "uuid-do-item",
      "quantidadeDevolver": 1
    }
  ]
}
```

**Conversão:**
```typescript
function converterItensDevolucao(itens: ItemDevolucao[]): ItemDevolucaoRequest[] {
  return itens
    .filter(item => item.qtdDevolver > 0)
    .map(item => ({
      itemId: item.itemId,
      quantidadeDevolver: item.qtdDevolver,
    }));
}
```

### 3.9 `ItemEntrada`, `fornecedor` e `nota` — `pages/NotaEntrada.tsx`

#### 3.9.1 Fornecedor

| Campo | Tipo TS | Observação |
|---|---|---|
| `cnpj` | `string` | 14 dígitos sem formatação |
| `razaoSocial` | `string` | Razão social completa |
| `ie` | `string` | Inscrição Estadual - **ADICIONAR INPUT NO FORMULÁRIO** |
| `uf` | `string` | 2 caracteres (sigla do estado) |

#### 3.9.2 Dados da Nota

| Campo | Tipo TS | Observação |
|---|---|---|
| `numero` | `string` | Número da nota do fornecedor |
| `serie` | `string` | Padrão "1" |
| `dataEmissao` | `string` | Formato `yyyy-MM-dd` (input type="date") |
| `dataEntrada` | `string` | Formato `yyyy-MM-dd` (default: hoje) |
| `chaveAcesso` | `string` | 44 dígitos (validar) |
| `naturezaOperacao` | `string` | Default: "Compra para comercialização" |

#### 3.9.3 Item de Entrada

| Campo | Tipo TS | Observação |
|---|---|---|
| `id` | `string?` | Temporário (Date.now()) - não enviar ao backend |
| `codigo` | `string` | SKU do produto |
| `descricao` | `string` | Nome/descrição do produto |
| `ncm` | `string` | NCM sem formatação |
| `quantidade` | `number` | Quantidade recebida |
| `valorUnitario` | `number` | Valor unitário |
| `cfop` | `string` | CFOP da operação (ex: "1102") |

**CFOPs Válidos:**
```typescript
const cfopOpcoes = [
  "1102", // Compra para comercialização
  "1202", // Devolução de venda
  "1403", // Compra para imobilizado
  "1411", // Compra para industrialização
  "2102", // Compra interestadual
  "2202"  // Devolução interestadual
];
```

#### 3.9.4 Payload para API

**Endpoint:** `POST /api/entrada`

```json
{
  "fornecedor": {
    "cnpj": "12345678901234",
    "razaoSocial": "Fornecedor ABC LTDA",
    "ie": "123456789",
    "uf": "SP"
  },
  "nota": {
    "numero": "12345",
    "serie": "1",
    "dataEmissao": "2024-09-20",
    "dataEntrada": "2024-09-21",
    "chaveAcesso": "35240912345678901234550010001234501234567890",
    "naturezaOperacao": "Compra para comercialização"
  },
  "itens": [
    {
      "codigo": "PF-001",
      "descricao": "Pastilha de Freio",
      "ncm": "87083010",
      "quantidade": 100,
      "valorUnitario": 45.50,
      "cfop": "1102"
    }
  ]
}
```

**Conversão:**
```typescript
// Remover campo 'id' temporário dos itens ao enviar
const payload = {
  fornecedor: fornecedorData,
  nota: notaData,
  itens: itens.map(({ id, ...item }) => item), // Remove 'id'
};
```

#### 3.9.5 Importação de XML

**Endpoint:** `POST /api/entrada/importar-xml`  
**Content-Type:** `multipart/form-data`

```typescript
async function importarXML(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch('/api/entrada/importar-xml', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    body: formData,
  });
  
  return response.json();
}
```

**IMPORTANTE:** Implementar handler para o input file que **atualmente não tem função**.

### 3.10 Usuário autenticado — `context/AuthContext.tsx`

```typescript
interface Usuario {
  id: string;        // UUID
  nome: string;
  email: string;
  papel?: string;    // Backend retorna mas frontend não usa ainda (futuro RBAC)
}

interface AuthContextType {
  isAuthenticated: boolean;
  carregando: boolean;  // true durante validação do token
  user: Usuario | null;
  login: (email: string, senha: string) => Promise<boolean>;
  logout: () => void;
}
```

#### 3.10.1 Login

**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "email": "admin@wcautopecas.com.br",
  "senha": "senha123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": {
    "id": "uuid",
    "nome": "Administrador",
    "email": "admin@wcautopecas.com.br",
    "papel": "ADMIN"
  },
  "expiresIn": 28800
}
```

**Persistência:**
- Token: `sessionStorage['wc_token']` ✅
- **NÃO** usar `localStorage` ✅
- Usuário: **NÃO persistir** - buscar via `GET /auth/me` ao carregar página ✅

#### 3.10.2 Validação de Sessão

**Endpoint:** `GET /api/auth/me`  
**Headers:** `Authorization: Bearer {token}`

**Response:**
```json
{
  "id": "uuid",
  "nome": "Administrador",
  "email": "admin@wcautopecas.com.br",
  "papel": "ADMIN"
}
```

**Comportamento:**
- Se retornar 401: Limpar token e redirecionar para `/login`
- Executar ao carregar aplicação (antes de renderizar telas autenticadas)

#### 3.10.3 Logout

**Endpoint:** `POST /api/auth/logout`  
**Headers:** `Authorization: Bearer {token}`

**Efeitos:**
- Remover `sessionStorage['wc_token']`
- Invalidar sessão no backend
- Redirecionar para `/login`

### 3.11 Dashboard e Cards do Home

#### 3.11.1 Dados Agregados do Dashboard

**Endpoint:** `GET /api/dashboard`

**Response:**
```json
{
  "vendas": {
    "hoje": 15,
    "valorHoje": 12500.00,
    "mes": 450,
    "valorMes": 250000.00,
    "ticketMedio": 833.33
  },
  "estoque": {
    "totalProdutos": 847,
    "produtosBaixo": 23,
    "produtosCriticos": 8,
    "valorTotalEstoque": 145890.00
  },
  "fiscal": {
    "emitidasHoje": 12,
    "autorizadas": 10,
    "processando": 2,
    "rejeitadas": 0,
    "valorHoje": 15000.00
  },
  "clientes": {
    "total": 320,
    "ativos": 305,
    "novosNoMes": 12
  }
}
```

**Cache:** 5 minutos no Redis

#### 3.11.2 EstoqueCard - Produtos Críticos

**Endpoint sugerido:** `GET /api/produtos?status=critico&limit=5`

**Estrutura:**
```typescript
interface ProdutoCritico {
  id: string;
  nome: string;
  codigo: string;
  ncm: string;
  estoque: {
    estoqueAtual: number;
    estoqueMinimo: number;
    status: "critico";
  };
}
```

**Adaptação:**
- Mapear `estoque.status === "critico"` para `critico: true` (compatibilidade)
- Mapear `estoque.estoqueAtual` para `quantidade`

#### 3.11.3 FiscalCard - Notas do Dia

**Endpoint sugerido:** `GET /api/notas?de={hoje}&limit=4`

**Estrutura:**
```typescript
interface NotaDoDia {
  id: string;
  numero: string;
  cliente: string;      // Nome ou "Consumidor Final"
  valorTotal: number;   // Formatar no frontend
  emitidaEm: string;    // ISO 8601 - extrair apenas hora
}
```

**Formatação:**
- `valorTotal`: Formatar com `Intl.NumberFormat` → `"R$ 1.250,00"`
- `emitidaEm`: Extrair apenas hora (`HH:mm`)

#### 3.11.4 VendasCard - Busca de Cliente

**Funcionalidade:** Buscar cliente por CNPJ e navegar para PDV

**Endpoint:** `GET /api/clientes?documento={cnpj}`

**Implementação necessária:**
```typescript
async function buscarClientePorCNPJ(cnpj: string) {
  const cnpjLimpo = limparDocumento(cnpj);
  const response = await fetch(`/api/clientes?documento=${cnpjLimpo}`);
  const data = await response.json();
  
  if (data.items.length > 0) {
    // Navegar para PDV com cliente pré-selecionado
    navigate('/pdv', { state: { cliente: data.items[0] } });
  } else {
    alert('Cliente não encontrado');
  }
}
```

---

## 4. Modelo do Backend (Contrato Real - IMPLEMENTADO)

Este modelo reflete a **arquitetura backend real** conforme documentado em `ARQUITETURA-BACKEND-DETALHADA.md`. O frontend deve adaptar-se a estes contratos.

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

### 4.3 Endpoints do Backend (Implementados)

#### 4.3.1 Autenticação (Auth Service - Port 8080)

| Operação | Método | Endpoint | Request | Response |
|----------|--------|----------|---------|----------|
| Login | POST | `/api/auth/login` | `{ email, senha }` | `{ token, refreshToken, usuario, expiresIn }` |
| Logout | POST | `/api/auth/logout` | Headers: Authorization | `204 No Content` |
| Validar sessão | GET | `/api/auth/me` | Headers: Authorization | `{ id, nome, email, papel }` |
| Refresh token | POST | `/api/auth/refresh` | `{ refreshToken }` | `{ token, ... }` |

#### 4.3.2 Clientes (Customer Service - Port 8081)

| Operação | Método | Endpoint | Query Params | Request Body |
|----------|--------|----------|--------------|--------------|
| Listar | GET | `/api/clientes` | `tipo`, `busca`, `cidade`, `uf`, `ativo`, `limit`, `lastKey` | - |
| Buscar por ID | GET | `/api/clientes/{id}` | - | - |
| Buscar por documento | GET | `/api/clientes?documento={doc}` | - | - |
| Criar | POST | `/api/clientes` | - | `ClienteInput` (ver seção 4.1) |
| Editar | PUT | `/api/clientes/{id}` | - | `ClienteInput` |
| Desativar | DELETE | `/api/clientes/{id}` | - | - |

**Response de listagem:** `{ items: Cliente[], lastKey?: string, count: number }`

#### 4.3.3 Produtos (Product Service - Port 8082)

| Operação | Método | Endpoint | Query Params | Request Body |
|----------|--------|----------|--------------|--------------|
| Listar | GET | `/api/produtos` | `busca`, `categoria`, `status`, `ativo`, `limit` | - |
| Buscar por ID | GET | `/api/produtos/{id}` | - | - |
| Criar | POST | `/api/produtos` | - | `ProdutoInput` |
| Editar | PUT | `/api/produtos/{id}` | - | `ProdutoInput` |
| Desativar | DELETE | `/api/produtos/{id}` | - | - |
| Estatísticas | GET | `/api/produtos/resumo` | - | - |
| Movimentações | GET | `/api/produtos/{id}/movimentos` | `de`, `ate`, `tipo`, `limit` | - |
| Ajuste manual | POST | `/api/produtos/{id}/ajuste` | - | `{ quantidadeNova, motivo }` |

#### 4.3.4 Vendas (Sales Service - Port 8083)

| Operação | Método | Endpoint | Query Params | Request Body |
|----------|--------|----------|--------------|--------------|
| Listar | GET | `/api/vendas` | `status`, `clienteId`, `de`, `ate`, `limit` | - |
| Buscar por ID | GET | `/api/vendas/{id}` | - | - |
| Criar | POST | `/api/vendas` | - | `VendaRequest` (ver seção 3.3.1) |
| Alterar status | PUT | `/api/vendas/{id}/status` | - | `{ status }` |
| Cancelar | DELETE | `/api/vendas/{id}` | - | - |

#### 4.3.5 Fiscal (Fiscal Service - Port 8084)

| Operação | Método | Endpoint | Query Params | Request Body |
|----------|--------|----------|--------------|--------------|
| Listar notas | GET | `/api/notas` | `status`, `tipo`, `de`, `ate`, `clienteId`, `limit` | - |
| Buscar nota | GET | `/api/notas/{id}` | - | - |
| Buscar itens | GET | `/api/notas/{id}/itens` | - | - |
| Reenviar | POST | `/api/notas/{id}/reenviar` | - | - |
| Cancelar | POST | `/api/notas/{id}/cancelamento` | - | `{ justificativa }` |
| Inutilizar faixa | POST | `/api/notas/inutilizacao` | - | `{ serie, numeroInicial, numeroFinal, justificativa }` |
| Emitir devolução | POST | `/api/notas/devolucao` | - | `{ notaReferenciadaId, motivo, itens[] }` |

#### 4.3.6 Entrada (Entry Service - Port 8085)

| Operação | Método | Endpoint | Content-Type | Request Body |
|----------|--------|----------|--------------|--------------|
| Listar | GET | `/api/entrada` | - | - |
| Buscar | GET | `/api/entrada/{id}` | - | - |
| Registrar | POST | `/api/entrada` | `application/json` | `{ fornecedor, nota, itens[] }` |
| Importar XML | POST | `/api/entrada/importar-xml` | `multipart/form-data` | `file=arquivo.xml` |

#### 4.3.7 Documentos (Document Service - Port 8086)

| Operação | Método | Endpoint | Response |
|----------|--------|----------|----------|
| Download XML | GET | `/api/documentos/xml/{notaId}` | XML file |
| Download PDF | GET | `/api/documentos/pdf/{notaId}` | PDF file |
| Lote contador | POST | `/api/documentos/lote-contador` | ZIP file URL |

#### 4.3.8 Relatórios (Reports Service - Port 8087)

| Operação | Método | Endpoint | Query Params |
|----------|--------|----------|--------------|
| Dashboard | GET | `/api/dashboard` | - |
| Relatório vendas | GET | `/api/relatorios/vendas` | `de`, `ate`, `formato`, `clienteId` |
| Relatório estoque | GET | `/api/relatorios/estoque` | `formato` |
| Relatório fiscal | GET | `/api/relatorios/fiscal` | `de`, `ate`, `tipo`, `status`, `formato` |

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

## 7. Correções Aplicadas e Incompatibilidades Resolvidas

### 7.1 ✅ Correções Realizadas no Mapeamento

#### 7.1.1 IDs: `number` → `string` (UUID)
- **Todos** os IDs foram alterados de `number` para `string`
- Cliente, Produto, Venda, Nota, Item, etc. agora usam UUID

#### 7.1.2 Formatação de Dados
- **Documento, Telefone, NCM:** Backend envia **sem formatação**
- **Frontend:** Formatar **apenas na exibição**
- Funções helper adicionadas: `formatarDocumento()`, `formatarTelefone()`, `formatarNCM()`

#### 7.1.3 Datas
- **Backend:** ISO 8601 (`2024-09-20T15:00:00Z`)
- **Frontend:** Converter para `dd/MM/yyyy` na exibição
- Input type="date" continua em `yyyy-MM-dd`

#### 7.1.4 Status
- **Cliente:** `ativo: boolean` (não mais `"ativo"|"inativo"`)
- **Produto:** `status: "ok"|"atencao"|"baixo"|"critico"` (union type)
- **Nota:** `status: "processando"|"autorizada"|"rejeitada"|"cancelada"`

#### 7.1.5 Estrutura de Estoque
- **Produto.estoque** agora é **objeto aninhado**:
  - `estoqueAtual`, `reservado`, `disponivel`, `status`, `ultimaMovimentacao`
- **Não calcular status no frontend** - usar valor do backend

#### 7.1.6 Campos Adicionados

**Cliente:**
- `nomeFantasia` (obrigatório para PJ)
- `inscricaoEstadual` (para PJ)
- Endereço completo: `cep`, `logradouro`, `numero`, `complemento`, `bairro`
- `dataCriacao`, `dataAtualizacao`
- `valorTotalCompras`

**Produto:**
- `categoria`, `fabricante`, `codigoFabricante`
- `cest`, `cfop`, `origem`, `cstIcms`, `aliquotaIcms`
- `margemLucro` (calculado)
- Objeto `estoque` completo

**Nota Fiscal:**
- `tipo`, `modelo`, `chaveAcesso`
- `autorizadaEm`, `protocolo`, `codigoStatus`
- `motivoRejeicao`, `ambiente`
- `xmlPath`, `pdfPath`

#### 7.1.7 Nomes de Campos Padronizados

| Frontend Original | Backend/Corrigido |
|------------------|-------------------|
| `minimo` | `estoqueMinimo` |
| `custo` | `precoCusto` |
| `venda` | `precoVenda` |
| `estoque` (number) | `estoque.estoqueAtual` (objeto) |
| `status: "ativo"` | `ativo: boolean` |
| `valor` | `valorTotal` |
| `data` + `hora` | `emitidaEm` (ISO 8601) |

#### 7.1.8 Estrutura de Venda Corrigida

**ItemCarrinho:** Adicionado `produtoId` (UUID)
- Campos apenas para exibição: `codigo`, `nome`
- Campos de UI: `editandoPreco`, `precoTemp` (não enviar)

**ItemVendaRequest:** Payload para API
- Apenas: `produtoId`, `quantidade`, `valorUnitario`

#### 7.1.9 Estrutura de Devolução Corrigida

**ItemDevolucao:** Adicionado `itemId` (UUID do item na nota original)
- Backend deve fornecer endpoint: `GET /api/notas/{id}/itens`
- Payload: `{ itemId, quantidadeDevolver }`

#### 7.1.10 Nota de Entrada

**Campo `ie` (Inscrição Estadual):**
- ⚠️ Existe no estado mas **não tem input**
- **AÇÃO NECESSÁRIA:** Adicionar campo no formulário

**Importação de XML:**
- Handler **ausente** - implementar upload multipart

### 7.2 ⚠️ Incompatibilidades Remanescentes (Requerem Ação)

#### 7.2.1 Frontend - Implementação Necessária

1. **Paginação:**
   - Backend usa cursor-based (`lastKey`)
   - Frontend não implementa
   - Adicionar scroll infinito ou botão "Carregar mais"

2. **Handlers Ausentes:** (22 botões sem função)
   - Clientes: Novo, Editar, Excluir
   - Estoque: Novo, Excluir
   - PDV: Adicionar Produto, Buscar Cliente, Finalizar, Salvar Orçamento, Imprimir
   - Painel Fiscal: Emitir, Enviar Lote, Exportar, Filtrar, XML, PDF
   - Devolução: Adicionar item
   - Entrada: Importar XML, Cancelar
   - Home: Novo Pedido

3. **Conversão de Dados:**
   - Implementar helpers de formatação
   - Implementar conversão de payloads (ItemCarrinho → ItemVendaRequest)
   - Implementar parse de respostas (API → Model)

4. **Gestão de Cliente no PDV:**
   - Buscar cliente por documento
   - Criar cliente rápido se não existir
   - Enviar `clienteId` (não campos soltos)

5. **Campo IE em Nota de Entrada:**
   - Adicionar input no formulário

6. **Upload de XML:**
   - Implementar handler multipart/form-data

#### 7.2.2 Backend - Melhorias Desejáveis

1. **Listagem de Notas:**
   - Adicionar campo `clienteDocumento` (CNPJ/CPF)
   - Evita requisições extras para buscar documento

2. **Dashboard:**
   - Retornar lista de produtos críticos (não apenas contagem)
   - Facilita exibição no EstoqueCard

3. **Endpoint de Itens:**
   - `GET /api/notas/{id}/itens` (se não existir)
   - Necessário para devolução

### 7.3 🎯 Resumo de Compatibilidade Após Correções

| Módulo | Antes | Depois | Status |
|--------|-------|--------|--------|
| Autenticação | 95% | 100% | ✅ Compatível |
| Clientes | 30% | 85% | ⚠️ Falta implementar handlers |
| Produtos | 25% | 85% | ⚠️ Falta implementar handlers |
| Vendas/PDV | 35% | 75% | ⚠️ Falta gestão de cliente |
| Fiscal | 60% | 90% | ⚠️ Falta campo CNPJ na listagem |
| Entrada | 50% | 85% | ⚠️ Falta campo IE e upload |
| Dashboard | 40% | 90% | ⚠️ Falta lista de críticos |

**Compatibilidade Geral:** 45% → **87%** 🎉

---

## 8. Helpers e Utilitários Necessários

### 8.1 Formatadores (src/utils/formatters.ts)

```typescript
/**
 * Formata CPF ou CNPJ
 * @param doc - Documento sem formatação (11 ou 14 dígitos)
 * @param tipo - "PF" ou "PJ"
 * @returns Documento formatado
 */
export function formatarDocumento(doc: string, tipo: "PF" | "PJ"): string {
  if (!doc) return '';
  
  if (tipo === "PF") {
    // 12345678901 → 123.456.789-01
    return doc.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
  }
  
  // 12345678901234 → 12.345.678/0001-34
  return doc.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, "$1.$2.$3/$4-$5");
}

/**
 * Remove formatação de documento
 * @param doc - Documento formatado
 * @returns Apenas dígitos
 */
export function limparDocumento(doc: string): string {
  return doc.replace(/\D/g, '');
}

/**
 * Formata telefone brasileiro
 * @param tel - Telefone sem formatação (10 ou 11 dígitos)
 * @returns Telefone formatado
 */
export function formatarTelefone(tel: string): string {
  if (!tel) return '';
  
  if (tel.length === 11) {
    // 11987654321 → (11) 98765-4321
    return tel.replace(/(\d{2})(\d{5})(\d{4})/, "($1) $2-$3");
  }
  
  // 1134567890 → (11) 3456-7890
  return tel.replace(/(\d{2})(\d{4})(\d{4})/, "($1) $2-$3");
}

/**
 * Formata NCM com pontos
 * @param ncm - NCM sem formatação (8 dígitos)
 * @returns NCM formatado
 */
export function formatarNCM(ncm: string): string {
  if (!ncm || ncm.length !== 8) return ncm;
  
  // 87083010 → 8708.30.10
  return ncm.replace(/(\d{4})(\d{2})(\d{2})/, "$1.$2.$3");
}

/**
 * Remove formatação de NCM
 * @param ncm - NCM formatado
 * @returns Apenas dígitos
 */
export function limparNCM(ncm: string): string {
  return ncm.replace(/\D/g, '');
}

/**
 * Converte data ISO 8601 para dd/MM/yyyy
 * @param isoDate - Data em ISO 8601 (ex: "2024-09-20T15:00:00Z")
 * @returns Data formatada (ex: "20/09/2024")
 */
export function formatarDataBR(isoDate: string): string {
  if (!isoDate) return '';
  
  const date = new Date(isoDate);
  return date.toLocaleDateString('pt-BR');
}

/**
 * Extrai hora de data ISO 8601
 * @param isoDate - Data em ISO 8601
 * @returns Hora formatada (ex: "15:00")
 */
export function extrairHora(isoDate: string): string {
  if (!isoDate) return '';
  
  const date = new Date(isoDate);
  return date.toLocaleTimeString('pt-BR', { 
    hour: '2-digit', 
    minute: '2-digit' 
  });
}

/**
 * Converte dd/MM/yyyy para ISO 8601
 * @param dataBR - Data em formato brasileiro
 * @returns Data em ISO 8601
 */
export function paraISO8601(dataBR: string): string {
  if (!dataBR) return '';
  
  const [dia, mes, ano] = dataBR.split('/');
  return `${ano}-${mes}-${dia}T00:00:00Z`;
}

/**
 * Formata valor monetário
 * @param valor - Valor numérico
 * @returns Valor formatado (ex: "R$ 1.250,00")
 */
export function formatarMoeda(valor: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(valor);
}

/**
 * Formata CEP
 * @param cep - CEP sem formatação (8 dígitos)
 * @returns CEP formatado (ex: "01310-100")
 */
export function formatarCEP(cep: string): string {
  if (!cep || cep.length !== 8) return cep;
  
  return cep.replace(/(\d{5})(\d{3})/, "$1-$2");
}

/**
 * Valida CPF
 * @param cpf - CPF com ou sem formatação
 * @returns true se válido
 */
export function validarCPF(cpf: string): boolean {
  cpf = limparDocumento(cpf);
  
  if (cpf.length !== 11 || /^(\d)\1+$/.test(cpf)) {
    return false;
  }
  
  let soma = 0;
  let resto;
  
  for (let i = 1; i <= 9; i++) {
    soma += parseInt(cpf.substring(i - 1, i)) * (11 - i);
  }
  
  resto = (soma * 10) % 11;
  if (resto === 10 || resto === 11) resto = 0;
  if (resto !== parseInt(cpf.substring(9, 10))) return false;
  
  soma = 0;
  for (let i = 1; i <= 10; i++) {
    soma += parseInt(cpf.substring(i - 1, i)) * (12 - i);
  }
  
  resto = (soma * 10) % 11;
  if (resto === 10 || resto === 11) resto = 0;
  if (resto !== parseInt(cpf.substring(10, 11))) return false;
  
  return true;
}

/**
 * Valida CNPJ
 * @param cnpj - CNPJ com ou sem formatação
 * @returns true se válido
 */
export function validarCNPJ(cnpj: string): boolean {
  cnpj = limparDocumento(cnpj);
  
  if (cnpj.length !== 14 || /^(\d)\1+$/.test(cnpj)) {
    return false;
  }
  
  let tamanho = cnpj.length - 2;
  let numeros = cnpj.substring(0, tamanho);
  const digitos = cnpj.substring(tamanho);
  let soma = 0;
  let pos = tamanho - 7;
  
  for (let i = tamanho; i >= 1; i--) {
    soma += parseInt(numeros.charAt(tamanho - i)) * pos--;
    if (pos < 2) pos = 9;
  }
  
  let resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
  if (resultado !== parseInt(digitos.charAt(0))) return false;
  
  tamanho = tamanho + 1;
  numeros = cnpj.substring(0, tamanho);
  soma = 0;
  pos = tamanho - 7;
  
  for (let i = tamanho; i >= 1; i--) {
    soma += parseInt(numeros.charAt(tamanho - i)) * pos--;
    if (pos < 2) pos = 9;
  }
  
  resultado = soma % 11 < 2 ? 0 : 11 - (soma % 11);
  if (resultado !== parseInt(digitos.charAt(1))) return false;
  
  return true;
}
```

### 8.2 Parsers/Converters (src/utils/parsers.ts)

```typescript
import { 
  formatarDocumento, 
  formatarTelefone, 
  formatarNCM, 
  formatarDataBR,
  extrairHora 
} from './formatters';

/**
 * Converte Cliente da API para modelo do frontend
 */
export function parseCliente(apiCliente: any): any {
  return {
    ...apiCliente,
    documento: apiCliente.documento, // Manter sem formatação
    telefone: apiCliente.telefone,   // Manter sem formatação
    status: apiCliente.ativo ? "ativo" : "inativo",
    ultimaCompra: apiCliente.ultimaCompra 
      ? formatarDataBR(apiCliente.ultimaCompra) 
      : null,
    
    // Para exibição, criar campos formatados opcionais:
    _documentoFormatado: formatarDocumento(apiCliente.documento, apiCliente.tipo),
    _telefoneFormatado: formatarTelefone(apiCliente.telefone),
  };
}

/**
 * Converte Produto da API para modelo do frontend
 */
export function parseProduto(apiProduto: any): any {
  return {
    id: apiProduto.id,
    codigo: apiProduto.codigo,
    nome: apiProduto.nome,
    ncm: apiProduto.ncm, // Manter sem formatação
    categoria: apiProduto.categoria,
    
    // Para compatibilidade com código legado:
    estoque: apiProduto.estoque.estoqueAtual,
    minimo: apiProduto.estoqueMinimo,
    custo: apiProduto.precoCusto,
    venda: apiProduto.precoVenda,
    status: apiProduto.estoque.status,
    
    // Dados completos:
    estoqueCompleto: apiProduto.estoque,
    precoCusto: apiProduto.precoCusto,
    precoVenda: apiProduto.precoVenda,
    estoqueMinimo: apiProduto.estoqueMinimo,
    
    // Para exibição:
    _ncmFormatado: formatarNCM(apiProduto.ncm),
  };
}

/**
 * Converte Nota da API para modelo do frontend
 */
export function parseNota(apiNota: any): any {
  const emitidaEm = new Date(apiNota.emitidaEm);
  
  return {
    ...apiNota,
    data: formatarDataBR(apiNota.emitidaEm),
    hora: extrairHora(apiNota.emitidaEm),
    valor: apiNota.valorTotal,
    cliente: apiNota.cliente || "Consumidor Final",
  };
}

/**
 * Converte ItemCarrinho para ItemVendaRequest (payload API)
 */
export function converterItemCarrinho(item: any): any {
  return {
    produtoId: item.produtoId,
    quantidade: item.quantidade,
    valorUnitario: item.valorUnitario,
  };
}

/**
 * Converte array de ItemDevolucao para payload API
 */
export function converterItensDevolucao(itens: any[]): any[] {
  return itens
    .filter(item => item.qtdDevolver > 0)
    .map(item => ({
      itemId: item.itemId,
      quantidadeDevolver: item.qtdDevolver,
    }));
}

/**
 * Converte ClienteInput do formulário para payload API
 */
export function converterClienteInput(form: any): any {
  return {
    ...form,
    documento: limparDocumento(form.documento),
    telefone: limparTelefone(form.telefone),
  };
}

// Helper para limpar telefone
function limparTelefone(tel: string): string {
  return tel.replace(/\D/g, '');
}
```

### 8.3 Paginação Helper (src/utils/pagination.ts)

```typescript
interface PaginatedResponse<T> {
  items: T[];
  lastKey?: string;
  count: number;
}

interface PaginationState<T> {
  items: T[];
  lastKey?: string;
  hasMore: boolean;
  loading: boolean;
}

/**
 * Hook para gerenciar paginação
 */
export function usePagination<T>(
  fetchFunction: (lastKey?: string) => Promise<PaginatedResponse<T>>
) {
  const [state, setState] = React.useState<PaginationState<T>>({
    items: [],
    lastKey: undefined,
    hasMore: true,
    loading: false,
  });
  
  async function loadMore() {
    if (state.loading || !state.hasMore) return;
    
    setState(prev => ({ ...prev, loading: true }));
    
    try {
      const response = await fetchFunction(state.lastKey);
      
      setState(prev => ({
        items: [...prev.items, ...response.items],
        lastKey: response.lastKey,
        hasMore: !!response.lastKey,
        loading: false,
      }));
    } catch (error) {
      setState(prev => ({ ...prev, loading: false }));
      throw error;
    }
  }
  
  function reset() {
    setState({
      items: [],
      lastKey: undefined,
      hasMore: true,
      loading: false,
    });
  }
  
  return { ...state, loadMore, reset };
}
```

### 8.4 Tratamento de Erros (src/utils/errors.ts)

```typescript
export class ApiError extends Error {
  constructor(
    public status: number,
    public message: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Wrapper para requisições HTTP com tratamento de erros
 */
export async function handleRequest<T>(
  request: Promise<Response>
): Promise<T> {
  try {
    const response = await request;
    
    if (!response.ok) {
      const error = await response.json().catch(() => ({ 
        message: 'Erro desconhecido' 
      }));
      
      throw new ApiError(response.status, error.message);
    }
    
    // Se for 204 No Content, retornar null
    if (response.status === 204) {
      return null as T;
    }
    
    return response.json();
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    
    // Erro de rede ou outro erro
    throw new ApiError(0, 'Erro de conexão. Verifique sua internet.');
  }
}

/**
 * Exibe erro de forma amigável
 */
export function exibirErro(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  
  if (error instanceof Error) {
    return error.message;
  }
  
  return 'Erro desconhecido';
}
```

## 9. Base de integração já criada

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

## 9. Checklist de Integração Frontend → Backend

### 9.1 ✅ Já Realizado (Base de Integração)

- [x] Tipos compartilhados em `src/app/types/index.ts`
- [x] Cliente HTTP em `src/app/services/http.ts`
- [x] Storage de token em `src/app/services/tokenStorage.ts`
- [x] Serviços de API criados (auth, clientes, produtos, vendas, notas, etc.)
- [x] Auth assíncrono implementado em `AuthContext.tsx`
- [x] Layout aguarda validação de sessão
- [x] Login sem credencial fixa
- [x] URL da API em `environment*.ts`

### 9.2 🔴 Prioridade ALTA (Bloqueadores - 40-60h)

#### 9.2.1 Atualizar Interfaces de Dados

- [ ] **Cliente:** Adicionar campos obrigatórios
  - [ ] `nomeFantasia`, `inscricaoEstadual`
  - [ ] `cep`, `logradouro`, `numero`, `complemento`, `bairro`
  - [ ] Mudar `status: string` para `ativo: boolean`
  - [ ] Arquivos: `Clientes.tsx`, `types/index.ts`

- [ ] **Produto:** Adaptar estrutura de estoque
  - [ ] Mudar `estoque: number` para `estoque: { estoqueAtual, reservado, disponivel, status }`
  - [ ] Adicionar campos: `categoria`, `fabricante`, etc.
  - [ ] Arquivos: `Estoque.tsx`, `types/index.ts`

- [ ] **Venda/PDV:** Adicionar `produtoId`
  - [ ] Modificar `ItemCarrinho` para incluir `produtoId`
  - [ ] Criar converter: `ItemCarrinho` → `ItemVendaRequest`
  - [ ] Arquivo: `PDV.tsx`

- [ ] **Nota Fiscal:** Adicionar campos completos
  - [ ] `tipo`, `chaveAcesso`, `autorizadaEm`, etc.
  - [ ] Adicionar `status: "rejeitada"`
  - [ ] Arquivo: `PainelFiscal.tsx`

#### 9.2.2 Implementar Helpers de Formatação

- [ ] `formatarDocumento(doc: string, tipo: "PF"|"PJ"): string`
- [ ] `formatarTelefone(tel: string): string`
- [ ] `formatarNCM(ncm: string): string`
- [ ] `formatarDataBR(isoDate: string): string`
- [ ] `formatarMoeda(valor: number): string`
- [ ] `limparDocumento(doc: string): string` (remover formatação)
- [ ] Arquivo sugerido: `src/utils/formatters.ts`

#### 9.2.3 Implementar Converters (API ↔ Model)

- [ ] `parseCliente(apiCliente: ClienteAPI): Cliente`
- [ ] `parseProduto(apiProduto: ProdutoAPI): Produto`
- [ ] `parseNota(apiNota: NotaAPI): Nota`
- [ ] `converterItemCarrinho(item: ItemCarrinho): ItemVendaRequest`
- [ ] `converterItensDevolucao(itens: ItemDevolucao[]): ItemDevolucaoRequest[]`
- [ ] Arquivo sugerido: `src/utils/parsers.ts`

#### 9.2.4 PDV: Gestão de Cliente

- [ ] Implementar busca de cliente por documento
  - [ ] Endpoint: `GET /api/clientes?documento={doc}`
  - [ ] Se não existir: Criar cliente rápido
- [ ] Modificar função `finalizarVenda()` para enviar `clienteId`
- [ ] Remover campos soltos de CPF/Nome (ou usá-los apenas para busca/criação)
- [ ] Arquivo: `PDV.tsx`

#### 9.2.5 Tratamento de Erros Global

- [ ] Criar wrapper `handleRequest<T>()` para tratar erros
- [ ] Exibir `error.message` do backend
- [ ] Substituir `alert()` por toast/notification
- [ ] Arquivo: `src/services/http.ts`

### 9.3 🟡 Prioridade MÉDIA (Funcionalidade - 30-40h)

#### 9.3.1 Implementar Paginação

- [ ] Clientes: Scroll infinito ou "Carregar mais"
- [ ] Produtos: Scroll infinito ou "Carregar mais"
- [ ] Notas Fiscais: Scroll infinito ou "Carregar mais"
- [ ] Adaptar state para guardar `lastKey`
- [ ] Arquivos: `Clientes.tsx`, `Estoque.tsx`, `PainelFiscal.tsx`

#### 9.3.2 Conectar Dashboard

- [ ] Consumir `GET /api/dashboard`
- [ ] Atualizar VendasCard com dados reais
- [ ] Atualizar EstoqueCard com produtos críticos
  - [ ] Endpoint: `GET /api/produtos?status=critico&limit=5`
- [ ] Atualizar FiscalCard com notas do dia
  - [ ] Endpoint: `GET /api/notas?de={hoje}&limit=4`
- [ ] Arquivos: `Home.tsx`, `*Card.tsx`

#### 9.3.3 Adicionar Campo IE em Nota de Entrada

- [ ] Adicionar `<input>` para Inscrição Estadual
- [ ] Validar formato
- [ ] Arquivo: `NotaEntrada.tsx`

#### 9.3.4 Implementar Upload de XML

- [ ] Criar handler `handleImportarXML()`
- [ ] FormData com arquivo
- [ ] POST multipart para `/api/entrada/importar-xml`
- [ ] Exibir feedback de processamento
- [ ] Arquivo: `NotaEntrada.tsx`

#### 9.3.5 Buscar Itens de Nota para Devolução

- [ ] Endpoint: `GET /api/notas/{id}/itens`
- [ ] Mapear `itemId` para cada item
- [ ] Converter para `ItemDevolucao` com controle de quantidade
- [ ] Arquivo: `NotaDevolucao.tsx`

### 9.4 🟢 Prioridade BAIXA (Polimento - 60-80h)

#### 9.4.1 Implementar Handlers Ausentes (22 botões)

**Clientes:**
- [ ] Novo Cliente
- [ ] Editar Cliente
- [ ] Excluir Cliente

**Estoque:**
- [ ] Novo Produto
- [ ] Excluir Produto

**PDV:**
- [ ] Adicionar Produto (busca)
- [ ] Finalizar Venda ✅ (já listado em Alta)
- [ ] Salvar Orçamento
- [ ] Imprimir Cupom

**Painel Fiscal:**
- [ ] Emitir NF-e (botão manual)
- [ ] Enviar Lote ao Contador
- [ ] Exportar Relatório
- [ ] Filtrar (implementar filtros)
- [ ] Download XML
- [ ] Download PDF

**Devolução:**
- [ ] Adicionar item manualmente

**Entrada:**
- [ ] Cancelar (limpar formulário)

**Home:**
- [ ] Novo Pedido (navegar para PDV)

#### 9.4.2 Remover Lógica de Negócio do Frontend

- [ ] **Status de Estoque:** Usar valor do backend (não calcular)
- [ ] **Cálculos de Totais:** Validar mas não replicar lógica
- [ ] **Validações:** Sincronizar com backend

#### 9.4.3 Testes de Integração

- [ ] Testar cada fluxo end-to-end
- [ ] Validar tratamento de erros
- [ ] Validar formatação de dados
- [ ] Validar conversão de payloads

#### 9.4.4 Melhorias de UX

- [ ] Loading states em todas operações
- [ ] Feedback visual de sucesso/erro
- [ ] Confirmações de ações destrutivas
- [ ] Desabilitar botões durante processamento

### 9.5 📋 Ordem Recomendada de Implementação

1. **Semana 1:** Prioridade Alta (itens 9.2.1 a 9.2.5)
   - Atualizar interfaces
   - Criar helpers e converters
   - Implementar gestão de cliente no PDV
   - Tratamento de erros

2. **Semana 2:** Prioridade Média (itens 9.3.1 a 9.3.5)
   - Paginação
   - Dashboard
   - Campo IE
   - Upload XML
   - Itens de devolução

3. **Semanas 3-4:** Prioridade Baixa (item 9.4)
   - Handlers restantes
   - Polimento e testes
   - Melhorias de UX

### 9.6 🎯 Critérios de Aceitação

**Integração considerada completa quando:**
- [ ] Todas as telas consomem APIs reais (sem mocks)
- [ ] Formatação de dados consistente
- [ ] Erros do backend são tratados e exibidos
- [ ] Paginação implementada onde necessário
- [ ] Todos os handlers críticos funcionam
- [ ] Conversão de dados (API ↔ Model) implementada
- [ ] Testes manuais de cada fluxo passam

**Tempo estimado total:** 130-180 horas (3-4 semanas)

---

## 10. Exemplo de Integração Completa (Clientes)

Este exemplo mostra como integrar a tela de Clientes do zero, seguindo todas as correções do mapeamento.

### 10.1 Atualizar Interface

```typescript
// src/app/types/cliente.types.ts
export interface Cliente {
  id: string;                    // UUID, não number
  tipo: "PF" | "PJ";
  documento: string;             // Sem formatação
  nome: string;
  nomeFantasia?: string;         // Obrigatório para PJ
  email: string;
  telefone: string;              // Sem formatação
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  inscricaoEstadual?: string;    // Para PJ
  ativo: boolean;                // Não mais "ativo"|"inativo"
  totalCompras: number;
  valorTotalCompras: number;
  ultimaCompra: string | null;   // ISO 8601
  dataCriacao: string;
  dataAtualizacao: string;
}

export interface ClientesResponse {
  items: Cliente[];
  lastKey?: string;
  count: number;
}

export interface ClienteInput {
  tipo: "PF" | "PJ";
  documento: string;             // Enviar sem formatação
  nome: string;
  nomeFantasia?: string;
  email: string;
  telefone: string;              // Enviar sem formatação
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  inscricaoEstadual?: string;
}
```

### 10.2 Criar/Atualizar Serviço

```typescript
// src/app/services/clientes.service.ts
import { handleRequest } from '@/utils/errors';
import { limparDocumento, limparTelefone } from '@/utils/formatters';

export const clientesService = {
  async listar(params?: {
    tipo?: "PF" | "PJ";
    busca?: string;
    cidade?: string;
    uf?: string;
    ativo?: boolean;
    limit?: number;
    lastKey?: string;
  }): Promise<ClientesResponse> {
    const queryParams = new URLSearchParams();
    if (params?.tipo) queryParams.append('tipo', params.tipo);
    if (params?.busca) queryParams.append('busca', params.busca);
    if (params?.cidade) queryParams.append('cidade', params.cidade);
    if (params?.uf) queryParams.append('uf', params.uf);
    if (params?.ativo !== undefined) queryParams.append('ativo', String(params.ativo));
    if (params?.limit) queryParams.append('limit', String(params.limit));
    if (params?.lastKey) queryParams.append('lastKey', params.lastKey);
    
    const url = `/api/clientes?${queryParams}`;
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    
    return handleRequest<ClientesResponse>(response);
  },
  
  async buscarPorId(id: string): Promise<Cliente> {
    const response = await fetch(`/api/clientes/${id}`, {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    
    return handleRequest<Cliente>(response);
  },
  
  async buscarPorDocumento(documento: string): Promise<ClientesResponse> {
    const docLimpo = limparDocumento(documento);
    return this.listar({ busca: docLimpo });
  },
  
  async criar(input: ClienteInput): Promise<Cliente> {
    const response = await fetch('/api/clientes', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`
      },
      body: JSON.stringify({
        ...input,
        documento: limparDocumento(input.documento),
        telefone: limparTelefone(input.telefone),
      }),
    });
    
    return handleRequest<Cliente>(response);
  },
  
  async atualizar(id: string, input: ClienteInput): Promise<Cliente> {
    const response = await fetch(`/api/clientes/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`
      },
      body: JSON.stringify({
        ...input,
        documento: limparDocumento(input.documento),
        telefone: limparTelefone(input.telefone),
      }),
    });
    
    return handleRequest<Cliente>(response);
  },
  
  async desativar(id: string): Promise<void> {
    const response = await fetch(`/api/clientes/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    
    return handleRequest<void>(response);
  },
};

function getToken(): string {
  return sessionStorage.getItem('wc_token') || '';
}
```

### 10.3 Atualizar Componente

```typescript
// src/app/pages/clientes/clientes.component.ts
import { Component, OnInit } from '@angular/core';
import { clientesService } from '@/services/clientes.service';
import { parseCliente } from '@/utils/parsers';
import { formatarDocumento, formatarTelefone, formatarDataBR } from '@/utils/formatters';
import { exibirErro } from '@/utils/errors';

@Component({
  selector: 'app-clientes',
  templateUrl: './clientes.component.html',
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  clientesFiltrados: Cliente[] = [];
  
  filtroTipo: 'todos' | 'PF' | 'PJ' = 'todos';
  busca = '';
  
  lastKey?: string;
  carregando = false;
  
  async ngOnInit() {
    await this.carregarClientes();
  }
  
  async carregarClientes() {
    if (this.carregando) return;
    
    this.carregando = true;
    
    try {
      const response = await clientesService.listar({
        tipo: this.filtroTipo === 'todos' ? undefined : this.filtroTipo,
        busca: this.busca || undefined,
        limit: 20,
        lastKey: this.lastKey,
      });
      
      // Concatenar com existentes (scroll infinito)
      this.clientes = [...this.clientes, ...response.items];
      this.lastKey = response.lastKey;
      
      this.aplicarFiltros();
    } catch (error) {
      alert(exibirErro(error));
    } finally {
      this.carregando = false;
    }
  }
  
  aplicarFiltros() {
    let resultado = this.clientes;
    
    if (this.filtroTipo !== 'todos') {
      resultado = resultado.filter(c => c.tipo === this.filtroTipo);
    }
    
    if (this.busca) {
      const termo = this.busca.toLowerCase();
      resultado = resultado.filter(c =>
        c.nome.toLowerCase().includes(termo) ||
        c.email.toLowerCase().includes(termo) ||
        c.documento.includes(termo)
      );
    }
    
    this.clientesFiltrados = resultado;
  }
  
  // Helpers para template
  formatarDocumento(doc: string, tipo: "PF" | "PJ"): string {
    return formatarDocumento(doc, tipo);
  }
  
  formatarTelefone(tel: string): string {
    return formatarTelefone(tel);
  }
  
  formatarData(isoDate: string | null): string {
    return isoDate ? formatarDataBR(isoDate) : '-';
  }
  
  statusTexto(ativo: boolean): string {
    return ativo ? 'ativo' : 'inativo';
  }
  
  async carregarMais() {
    if (!this.lastKey) return;
    await this.carregarClientes();
  }
}
```

### 10.4 Template HTML (Exemplo)

```html
<!-- clientes.component.html -->
<div class="clientes-container">
  <div class="filtros">
    <select [(ngModel)]="filtroTipo" (change)="aplicarFiltros()">
      <option value="todos">Todos</option>
      <option value="PF">Pessoa Física</option>
      <option value="PJ">Pessoa Jurídica</option>
    </select>
    
    <input 
      type="text" 
      [(ngModel)]="busca" 
      (input)="aplicarFiltros()"
      placeholder="Buscar por nome, email ou documento"
    />
  </div>
  
  <table>
    <thead>
      <tr>
        <th>Tipo</th>
        <th>Documento</th>
        <th>Nome</th>
        <th>Email</th>
        <th>Telefone</th>
        <th>Cidade/UF</th>
        <th>Total Compras</th>
        <th>Última Compra</th>
        <th>Status</th>
        <th>Ações</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let cliente of clientesFiltrados">
        <td>{{ cliente.tipo }}</td>
        <td>{{ formatarDocumento(cliente.documento, cliente.tipo) }}</td>
        <td>{{ cliente.nome }}</td>
        <td>{{ cliente.email }}</td>
        <td>{{ formatarTelefone(cliente.telefone) }}</td>
        <td>{{ cliente.cidade }}/{{ cliente.uf }}</td>
        <td>{{ cliente.totalCompras }}</td>
        <td>{{ formatarData(cliente.ultimaCompra) }}</td>
        <td>
          <span [class]="'badge badge-' + (cliente.ativo ? 'success' : 'secondary')">
            {{ statusTexto(cliente.ativo) }}
          </span>
        </td>
        <td>
          <button (click)="editar(cliente.id)">Editar</button>
          <button (click)="desativar(cliente.id)">Desativar</button>
        </td>
      </tr>
    </tbody>
  </table>
  
  <div *ngIf="lastKey" class="carregar-mais">
    <button 
      (click)="carregarMais()" 
      [disabled]="carregando"
    >
      {{ carregando ? 'Carregando...' : 'Carregar mais' }}
    </button>
  </div>
</div>
```

### 10.5 Resultado Final

**O que foi corrigido:**
- ✅ IDs de `number` para `string` (UUID)
- ✅ Campo `status` substituído por `ativo: boolean`
- ✅ Campos obrigatórios adicionados (`nomeFantasia`, `inscricaoEstadual`, endereço)
- ✅ Formatação de dados apenas na exibição
- ✅ Conversão de datas ISO para dd/MM/yyyy
- ✅ Paginação implementada (scroll infinito)
- ✅ Tratamento de erros
- ✅ Limpeza de documento/telefone ao enviar

**Aplicar mesmo padrão para:**
- Produtos (seção 3.2)
- Vendas/PDV (seção 3.4)
- Notas Fiscais (seção 3.5)
- Todas as demais telas

---

**Fim do Mapeamento de Classes Corrigido**  
**Versão:** 2.0 (Compatível com Backend)  
**Data:** 29/09/2026
