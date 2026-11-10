package br.com.wcautopecas.document.nota;

import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.document.DocumentosProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class NotaRepository {

    static final TableSchema<Nota> NOTA = TableSchema.fromBean(Nota.class);

    private final DynamoDbTable<Nota> notas;
    private final DynamoDbClient dynamo;
    private final DocumentosProperties props;

    public NotaRepository(DynamoDbEnhancedClient enhanced, DynamoDbClient dynamo, DocumentosProperties props) {
        this.notas = enhanced.table(props.getTabelaNotas(), NOTA);
        this.dynamo = dynamo;
        this.props = props;
    }

    public Optional<Nota> porId(long id) {
        return Optional.ofNullable(notas.getItem(r -> r.key(Key.builder().partitionValue(id).build())
                .consistentRead(true)));
    }

    public List<Nota> todas() {
        return ListagemDynamo.todos(notas.scan());
    }

    public void marcarPdf(long id, String pdfPath) {
        atualizar(id, "pdfPath", pdfPath);
    }

    public void marcarEnviada(long id, String quando) {
        atualizar(id, "enviadaContadorEm", quando);
    }

    private void atualizar(long id, String campo, String valor) {
        dynamo.updateItem(r -> r.tableName(props.getTabelaNotas())
                .key(Map.of("id", AttributeValue.fromN(Long.toString(id))))
                .conditionExpression("attribute_exists(id)")
                .updateExpression("SET #c = :v")
                .expressionAttributeNames(Map.of("#c", campo))
                .expressionAttributeValues(Map.of(":v", AttributeValue.fromS(valor))));
    }
}
