package br.com.wcautopecas.sales.venda;

import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.sales.VendasProperties;
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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@Repository
public class VendaRepository {

    static final TableSchema<VendaItem> VENDA = TableSchema.fromBean(VendaItem.class);
    static final TableSchema<VendaItemLinha> LINHA = TableSchema.fromBean(VendaItemLinha.class);

    private final DynamoDbTable<VendaItem> vendas;
    private final DynamoDbTable<VendaItemLinha> itens;
    private final DynamoDbClient dynamo;
    private final VendasProperties props;

    public VendaRepository(DynamoDbEnhancedClient enhanced, DynamoDbClient dynamo, VendasProperties props) {
        this.vendas = enhanced.table(props.getTabelaVendas(), VENDA);
        this.itens = enhanced.table(props.getTabelaItens(), LINHA);
        this.dynamo = dynamo;
        this.props = props;
    }

    /** Venda e itens numa transação (máx. 99 itens por venda, limite do TransactWriteItems). */
    public void criar(VendaItem v, List<VendaItemLinha> linhas) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaVendas())
                .item(VENDA.itemToMap(v, true)).conditionExpression("attribute_not_exists(id)").build()).build());
        for (VendaItemLinha l : linhas) {
            ops.add(TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaItens())
                    .item(LINHA.itemToMap(l, true)).build()).build());
        }
        dynamo.transactWriteItems(r -> r.transactItems(ops));
    }

    public Optional<VendaItem> porId(long id) {
        return Optional.ofNullable(vendas.getItem(r -> r.key(Key.builder().partitionValue(id).build())
                .consistentRead(true)));
    }

    public List<VendaItemLinha> itens(long vendaId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(vendaId).build());
        return ListagemDynamo.todos(itens.query(r -> r.queryConditional(cond)));
    }

    public List<VendaItem> todas() {
        return ListagemDynamo.todos(vendas.scan());
    }

    public List<VendaItem> doCliente(long clienteId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(clienteId).build());
        return ListagemDynamo.todos(vendas.index("cliente-index").query(r -> r.queryConditional(cond)));
    }

    public List<VendaItem> doStatus(String status) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(status).build());
        return ListagemDynamo.todos(vendas.index("status-index").query(r -> r.queryConditional(cond)));
    }

    /** Grava só se o status ainda for o esperado; {@code false} se outra requisição mudou antes. */
    public boolean salvarSeStatus(VendaItem v, String statusEsperado) {
        try {
            dynamo.putItem(r -> r.tableName(props.getTabelaVendas()).item(VENDA.itemToMap(v, true))
                    .conditionExpression("#s = :esperado")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(":esperado", AttributeValue.fromS(statusEsperado))));
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public void vincularNota(long vendaId, long notaFiscalId) {
        dynamo.updateItem(r -> r.tableName(props.getTabelaVendas())
                .key(Map.of("id", AttributeValue.fromN(Long.toString(vendaId))))
                .conditionExpression("attribute_exists(id)")
                .updateExpression("SET notaFiscalId = :n")
                .expressionAttributeValues(Map.of(":n", AttributeValue.fromN(Long.toString(notaFiscalId)))));
    }
}
