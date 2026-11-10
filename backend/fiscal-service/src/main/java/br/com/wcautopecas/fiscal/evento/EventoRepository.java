package br.com.wcautopecas.fiscal.evento;

import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.fiscal.FiscalProperties;
import java.util.List;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@Repository
public class EventoRepository {

    static final TableSchema<EventoFiscal> EVENTO = TableSchema.fromBean(EventoFiscal.class);

    private final DynamoDbTable<EventoFiscal> eventos;
    private final FiscalProperties props;

    public EventoRepository(DynamoDbEnhancedClient enhanced, FiscalProperties props) {
        this.eventos = enhanced.table(props.getTabelaEventos(), EVENTO);
        this.props = props;
    }

    public void salvar(EventoFiscal e) {
        eventos.putItem(e);
    }

    public TransactWriteItem criarTx(EventoFiscal e) {
        return TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaEventos())
                .item(EVENTO.itemToMap(e, true)).conditionExpression("attribute_not_exists(id)").build()).build();
    }

    public java.util.Optional<EventoFiscal> porId(long id) {
        return java.util.Optional.ofNullable(eventos.getItem(Key.builder().partitionValue(id).build()));
    }

    /** Put condicional ao status, para compor transações com a nota. */
    public TransactWriteItem seStatusTx(EventoFiscal e, String statusEsperado) {
        return TransactWriteItem.builder().put(Put.builder().tableName(props.getTabelaEventos())
                .item(EVENTO.itemToMap(e, true)).conditionExpression("#s = :s")
                .expressionAttributeNames(java.util.Map.of("#s", "status"))
                .expressionAttributeValues(java.util.Map.of(":s",
                        software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS(statusEsperado)))
                .build()).build();
    }

    public List<EventoFiscal> daNota(long notaId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(notaId).build());
        return ListagemDynamo.todos(eventos.index("nota-index").query(r -> r.queryConditional(cond)));
    }

    public List<EventoFiscal> doTipo(String tipo) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(tipo).build());
        return ListagemDynamo.todos(eventos.index("tipo-index").query(r -> r.queryConditional(cond)));
    }
}
