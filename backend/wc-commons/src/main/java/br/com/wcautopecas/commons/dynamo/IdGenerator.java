package br.com.wcautopecas.commons.dynamo;

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Ids numéricos sequenciais por recurso, como o front espera. Usa a tabela {@code counters}
 * (PK {@code recurso}) com {@code UpdateItem ADD valor :1}, que é atômico no DynamoDB.
 */
public class IdGenerator {

    private final DynamoDbClient dynamo;
    private final String tabela;

    public IdGenerator(DynamoDbClient dynamo, String tabela) {
        this.dynamo = dynamo;
        this.tabela = tabela;
    }

    /** Próximo id do recurso (1, 2, 3...). */
    public long proximo(String recurso) {
        return avancar(recurso, 1);
    }

    /** Reserva {@code quantidade} ids e devolve o último. */
    public long avancar(String recurso, long quantidade) {
        var resp = dynamo.updateItem(UpdateItemRequest.builder()
                .tableName(tabela)
                .key(Map.of("recurso", AttributeValue.fromS(recurso)))
                .updateExpression("ADD valor :n")
                .expressionAttributeValues(Map.of(":n", AttributeValue.fromN(Long.toString(quantidade))))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build());
        return Long.parseLong(resp.attributes().get("valor").n());
    }

    /** Garante que o contador seja pelo menos {@code minimo} (usado pelo seed com ids fixos). */
    public void garantirMinimo(String recurso, long minimo) {
        try {
            dynamo.updateItem(UpdateItemRequest.builder()
                    .tableName(tabela)
                    .key(Map.of("recurso", AttributeValue.fromS(recurso)))
                    .updateExpression("SET valor = :m")
                    .conditionExpression("attribute_not_exists(valor) OR valor < :m")
                    .expressionAttributeValues(Map.of(":m", AttributeValue.fromN(Long.toString(minimo))))
                    .build());
        } catch (software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException e) {
            // já está acima do mínimo
        }
    }
}
