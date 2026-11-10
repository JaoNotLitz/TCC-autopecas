package br.com.wcautopecas.product.produto;

import br.com.wcautopecas.commons.config.WcProperties;
import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.product.ProdutosProperties;
import br.com.wcautopecas.product.estoque.MovimentoItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Tabelas {@code products}, {@code inventory} e {@code inventory_movements}. O código único é
 * garantido por um item-guarda na tabela {@code counters} ({@code unico:produtos:codigo:X}),
 * gravado na mesma transação.
 */
@Repository
public class ProdutoRepository {

    public static final TableSchema<ProdutoItem> PRODUTO = TableSchema.fromBean(ProdutoItem.class);
    public static final TableSchema<EstoqueItem> ESTOQUE = TableSchema.fromBean(EstoqueItem.class);
    public static final TableSchema<MovimentoItem> MOVIMENTO = TableSchema.fromBean(MovimentoItem.class);

    /** Lançada quando a versão do estoque mudou entre a leitura e a escrita. */
    public static class VersaoDesatualizadaException extends RuntimeException {
        public VersaoDesatualizadaException() {
            super("versão do estoque mudou");
        }
    }

    private final DynamoDbTable<ProdutoItem> produtos;
    private final DynamoDbTable<EstoqueItem> estoque;
    private final DynamoDbTable<MovimentoItem> movimentos;
    private final DynamoDbClient dynamo;
    private final ProdutosProperties props;
    private final String tabelaGuardas;

    public ProdutoRepository(DynamoDbEnhancedClient enhanced, DynamoDbClient dynamo, ProdutosProperties props,
            WcProperties wc) {
        this.produtos = enhanced.table(props.getTabelaProdutos(), PRODUTO);
        this.estoque = enhanced.table(props.getTabelaEstoque(), ESTOQUE);
        this.movimentos = enhanced.table(props.getTabelaMovimentos(), MOVIMENTO);
        this.dynamo = dynamo;
        this.props = props;
        this.tabelaGuardas = wc.getAws().getTabelaCounters();
    }

    // --- leitura -----------------------------------------------------------------------------

    public Optional<ProdutoItem> produto(long id) {
        return Optional.ofNullable(produtos.getItem(Key.builder().partitionValue(id).build()));
    }

    public Optional<ProdutoItem> porCodigo(String codigo) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(codigo).build());
        return ListagemDynamo.todos(produtos.index("codigo-index").query(r -> r.queryConditional(cond))).stream()
                .findFirst();
    }

    /** Leitura consistente: usada antes de escrever com controle de versão. */
    public Optional<EstoqueItem> estoque(long produtoId) {
        return Optional.ofNullable(estoque.getItem(r -> r.key(Key.builder().partitionValue(produtoId).build())
                .consistentRead(true)));
    }

    public List<ProdutoItem> todosProdutos() {
        return ListagemDynamo.todos(produtos.scan());
    }

    public List<EstoqueItem> todoEstoque() {
        return ListagemDynamo.todos(estoque.scan());
    }

    /** Movimentações do produto, mais recentes primeiro (produto-data-index, ordem decrescente). */
    public List<MovimentoItem> movimentos(long produtoId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(produtoId).build());
        return ListagemDynamo.todos(movimentos.index("produto-data-index")
                .query(r -> r.queryConditional(cond).scanIndexForward(false)));
    }

    // --- escrita -----------------------------------------------------------------------------

    /** Produto + estoque + (opcional) movimentação inicial + guarda do código, numa transação. */
    public void criar(ProdutoItem p, EstoqueItem e, MovimentoItem inicial) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(put(props.getTabelaProdutos(), PRODUTO.itemToMap(p, true), null));
        ops.add(put(props.getTabelaEstoque(), ESTOQUE.itemToMap(e, true), "attribute_not_exists(produtoId)"));
        if (inicial != null) {
            ops.add(put(props.getTabelaMovimentos(), MOVIMENTO.itemToMap(inicial, true), null));
        }
        ops.add(guarda(p.getCodigo(), p.getId()));
        executar(ops);
    }

    /** Atualiza o cadastro (trocando o guarda se o código mudou) e, se vier, o estoque com versão. */
    public void atualizar(ProdutoItem antigo, ProdutoItem novo, EstoqueItem estoqueNovo, long versaoLida,
            MovimentoItem movimento) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(put(props.getTabelaProdutos(), PRODUTO.itemToMap(novo, true), null));
        if (!Objects.equals(antigo.getCodigo(), novo.getCodigo())) {
            ops.add(TransactWriteItem.builder().delete(Delete.builder().tableName(tabelaGuardas)
                    .key(Map.of("recurso", AttributeValue.fromS(chaveGuarda(antigo.getCodigo())))).build()).build());
            ops.add(guarda(novo.getCodigo(), novo.getId()));
        }
        if (estoqueNovo != null) {
            ops.add(estoqueComVersao(estoqueNovo, versaoLida));
        }
        if (movimento != null) {
            ops.add(put(props.getTabelaMovimentos(), MOVIMENTO.itemToMap(movimento, true), null));
        }
        executar(ops);
    }

    public void salvarProduto(ProdutoItem p) {
        produtos.putItem(p);
    }

    /** Estoque (com versão) + movimentação na mesma transação (HU-B05.4). */
    public void gravarMovimento(EstoqueItem novo, long versaoLida, MovimentoItem movimento) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(estoqueComVersao(novo, versaoLida));
        if (movimento != null) {
            ops.add(put(props.getTabelaMovimentos(), MOVIMENTO.itemToMap(movimento, true), null));
        }
        executar(ops);
    }

    private TransactWriteItem estoqueComVersao(EstoqueItem novo, long versaoLida) {
        return TransactWriteItem.builder().put(Put.builder()
                .tableName(props.getTabelaEstoque())
                .item(ESTOQUE.itemToMap(novo, true))
                .conditionExpression("versao = :v")
                .expressionAttributeValues(Map.of(":v", AttributeValue.fromN(Long.toString(versaoLida))))
                .build()).build();
    }

    private TransactWriteItem guarda(String codigo, long produtoId) {
        return TransactWriteItem.builder().put(Put.builder()
                .tableName(tabelaGuardas)
                .item(Map.of("recurso", AttributeValue.fromS(chaveGuarda(codigo)),
                        "produtoId", AttributeValue.fromN(Long.toString(produtoId))))
                .conditionExpression("attribute_not_exists(recurso) OR produtoId = :id")
                .expressionAttributeValues(Map.of(":id", AttributeValue.fromN(Long.toString(produtoId))))
                .build()).build();
    }

    private static TransactWriteItem put(String tabela, Map<String, AttributeValue> item, String condicao) {
        var b = Put.builder().tableName(tabela).item(item);
        if (condicao != null) {
            b.conditionExpression(condicao);
        }
        return TransactWriteItem.builder().put(b.build()).build();
    }

    static String chaveGuarda(String codigo) {
        return "unico:produtos:codigo:" + codigo;
    }

    private void executar(List<TransactWriteItem> ops) {
        try {
            dynamo.transactWriteItems(r -> r.transactItems(ops));
        } catch (TransactionCanceledException e) {
            List<CancellationReason> motivos = e.cancellationReasons();
            for (int i = 0; i < motivos.size() && i < ops.size(); i++) {
                if (!"ConditionalCheckFailed".equals(motivos.get(i).code()) || ops.get(i).put() == null) {
                    continue;
                }
                String tabela = ops.get(i).put().tableName();
                if (tabela.equals(tabelaGuardas)) {
                    throw new ConflitoException("Código já cadastrado");
                }
                if (tabela.equals(props.getTabelaEstoque())) {
                    throw new VersaoDesatualizadaException();
                }
            }
            throw e;
        }
    }
}
