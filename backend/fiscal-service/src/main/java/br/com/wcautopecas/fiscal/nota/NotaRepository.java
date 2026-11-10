package br.com.wcautopecas.fiscal.nota;

import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.fiscal.FiscalProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Repository
public class NotaRepository {

    static final TableSchema<NotaItem> NOTA = TableSchema.fromBean(NotaItem.class);
    static final TableSchema<NotaLinha> LINHA = TableSchema.fromBean(NotaLinha.class);

    private final DynamoDbTable<NotaItem> notas;
    private final DynamoDbTable<NotaLinha> linhas;
    private final DynamoDbClient dynamo;
    private final FiscalProperties props;

    public NotaRepository(DynamoDbEnhancedClient enhanced, DynamoDbClient dynamo, FiscalProperties props) {
        this.notas = enhanced.table(props.getTabelaNotas(), NOTA);
        this.linhas = enhanced.table(props.getTabelaItens(), LINHA);
        this.dynamo = dynamo;
        this.props = props;
    }

    /** Nota e itens numa transação (até 99 itens). {@code extras} entram na mesma transação. */
    public void criar(NotaItem n, List<NotaLinha> itens, List<TransactWriteItem> extras) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaNotas())
                .item(NOTA.itemToMap(n, true)).conditionExpression("attribute_not_exists(id)").build()).build());
        for (NotaLinha l : itens) {
            ops.add(TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaItens())
                    .item(LINHA.itemToMap(l, true)).build()).build());
        }
        ops.addAll(extras);
        dynamo.transactWriteItems(r -> r.transactItems(ops));
    }

    public Optional<NotaItem> porId(long id) {
        return Optional.ofNullable(notas.getItem(r -> r.key(Key.builder().partitionValue(id).build())
                .consistentRead(true)));
    }

    public List<NotaLinha> itens(long notaId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(notaId).build());
        return ListagemDynamo.todos(linhas.query(r -> r.queryConditional(cond).consistentRead(true)));
    }

    public List<NotaItem> todas() {
        return ListagemDynamo.todos(notas.scan());
    }

    public List<NotaItem> daVenda(long vendaId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(vendaId).build());
        return ListagemDynamo.todos(notas.index("venda-index").query(r -> r.queryConditional(cond)));
    }

    public List<NotaItem> doStatus(String status) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(status).build());
        return ListagemDynamo.todos(notas.index("status-index").query(r -> r.queryConditional(cond)));
    }

    public List<NotaItem> doTipo(String tipo) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(tipo).build());
        return ListagemDynamo.todos(notas.index("emitidaEm-index").query(r -> r.queryConditional(cond)));
    }

    /** Grava só se o status ainda for o esperado; {@code false} se outra operação mudou antes. */
    public boolean salvarSeStatus(NotaItem n, String statusEsperado) {
        try {
            dynamo.transactWriteItems(r -> r.transactItems(seStatusTx(n, statusEsperado)));
            return true;
        } catch (TransactionCanceledException e) {
            return false;
        }
    }

    /** Put condicional ao status, para compor transações. */
    public TransactWriteItem seStatusTx(NotaItem n, String statusEsperado) {
        return TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaNotas())
                .item(NOTA.itemToMap(n, true))
                .conditionExpression("#s = :esperado")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(":esperado", AttributeValue.fromS(statusEsperado))).build()).build();
    }

    public void salvarLinha(NotaLinha l) {
        linhas.putItem(l);
    }

    public TransactWriteItem linhaTx(NotaLinha l) {
        return TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaItens())
                .item(LINHA.itemToMap(l, true)).build()).build();
    }
}
