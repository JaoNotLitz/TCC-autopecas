package br.com.wcautopecas.commons.dynamo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.wcautopecas.commons.teste.ClientesTeste;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

class DynamoIT {

    final DynamoDbClient dynamo = ClientesTeste.dynamo();

    @Test
    void idsConcorrentesSaoDistintosEConsecutivos() throws Exception {
        IdGenerator ids = new IdGenerator(dynamo, "counters");
        String recurso = "teste-" + UUID.randomUUID();
        Set<Long> gerados = ConcurrentHashMap.newKeySet();
        try (var pool = Executors.newFixedThreadPool(10)) {
            var futuros = IntStream.range(0, 50).mapToObj(i -> pool.submit(() -> gerados.add(ids.proximo(recurso))))
                    .toList();
            for (var f : futuros) {
                f.get();
            }
        }
        assertThat(gerados).containsExactlyInAnyOrderElementsOf(LongStream.rangeClosed(1, 50).boxed().toList());
    }

    @Test
    void garantirMinimoNaoVoltaContador() {
        IdGenerator ids = new IdGenerator(dynamo, "counters");
        String recurso = "teste-" + UUID.randomUUID();
        ids.garantirMinimo(recurso, 10);
        assertThat(ids.proximo(recurso)).isEqualTo(11);
        ids.garantirMinimo(recurso, 5);
        assertThat(ids.proximo(recurso)).isEqualTo(12);
    }

    @DynamoDbBean
    public static class Linha {
        private String recurso;
        private Long valor;

        @DynamoDbPartitionKey
        public String getRecurso() {
            return recurso;
        }

        public void setRecurso(String recurso) {
            this.recurso = recurso;
        }

        public Long getValor() {
            return valor;
        }

        public void setValor(Long valor) {
            this.valor = valor;
        }
    }

    @Test
    void listagemPercorreTodasAsPaginas() {
        // Tabela própria para não misturar com os contadores dos outros testes.
        String tabela = "listagem_" + UUID.randomUUID().toString().substring(0, 8);
        dynamo.createTable(r -> r.tableName(tabela)
                .attributeDefinitions(a -> a.attributeName("recurso").attributeType("S"))
                .keySchema(k -> k.attributeName("recurso").keyType("HASH"))
                .billingMode("PAY_PER_REQUEST"));
        List<WriteRequest> lote = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            // ~600 bytes por item: 2.500 itens passam de 1 MB e forçam mais de uma página.
            lote.add(WriteRequest.builder().putRequest(PutRequest.builder().item(Map.of(
                    "recurso", AttributeValue.fromS("r" + i),
                    "valor", AttributeValue.fromN(Integer.toString(i)),
                    "pad", AttributeValue.fromS("x".repeat(600)))).build()).build());
            if (lote.size() == 25) {
                dynamo.batchWriteItem(BatchWriteItemRequest.builder().requestItems(Map.of(tabela, lote)).build());
                lote = new ArrayList<>();
            }
        }
        var enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamo).build();
        var t = enhanced.table(tabela, TableSchema.fromBean(Linha.class));
        List<Linha> todos = ListagemDynamo.todos(t.scan(r -> r.limit(1000)));
        assertThat(todos).hasSize(2500);
    }
}
