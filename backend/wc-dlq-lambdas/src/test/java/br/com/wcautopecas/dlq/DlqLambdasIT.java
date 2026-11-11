package br.com.wcautopecas.dlq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.wcautopecas.commons.teste.InfraTeste;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/** Os handlers rodam aqui dentro, apontando para o LocalStack dos testes (mesmo init do Compose). */
class DlqLambdasIT {

    static final ObjectMapper JSON = new ObjectMapper();
    final Ambiente amb = new Ambiente(InfraTeste.endpoint(), "sa-east-1", "fiscal-dlq", 5,
            "system-notifications-topic", "counters");
    final SqsClient sqs = amb.sqs();
    final DynamoDbClient dynamo = amb.dynamo();

    String url(String fila) {
        return sqs.getQueueUrl(r -> r.queueName(fila)).queueUrl();
    }

    List<Message> esvaziar(String fila) {
        List<Message> todas = new ArrayList<>();
        String u = url(fila);
        while (true) {
            var ms = sqs.receiveMessage(r -> r.queueUrl(u).maxNumberOfMessages(10).waitTimeSeconds(0)
                    .messageAttributeNames("All")).messages();
            if (ms.isEmpty()) {
                return todas;
            }
            todas.addAll(ms);
            ms.forEach(m -> sqs.deleteMessage(r -> r.queueUrl(u).receiptHandle(m.receiptHandle())));
        }
    }

    void naDlq(String corpo, Map<String, String> atributos) {
        Map<String, MessageAttributeValue> a = new java.util.HashMap<>();
        atributos.forEach((k, v) -> a.put(k, MessageAttributeValue.builder().dataType("String").stringValue(v).build()));
        sqs.sendMessage(r -> r.queueUrl(url("fiscal-dlq")).messageBody(corpo).messageAttributes(a));
    }

    int naDlqAgora() {
        return sqs.getQueueAttributes(r -> r.queueUrl(url("fiscal-dlq"))
                .attributeNamesWithStrings("ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible"))
                .attributesAsStrings().values().stream().mapToInt(Integer::parseInt).sum();
    }

    @BeforeEach
    void limpar() {
        esvaziar("fiscal-dlq");
        esvaziar("email-queue");
        esvaziar("sefaz-queue");
        dynamo.deleteItem(r -> r.tableName("counters").key(Map.of("recurso", AttributeValue.fromS("dlq-monitor"))));
    }

    List<JsonNode> alertas() {
        return esvaziar("email-queue").stream().map(m -> {
            try {
                return JSON.readTree(m.body());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).filter(j -> "fiscal-dlq".equals(j.path("service").asText())).toList();
    }

    @Test
    void monitorAlertaUmaVezPorFaixa() {
        var monitor = new DlqMonitorHandler(amb);
        for (int i = 0; i < 2; i++) {
            naDlq("{\"type\":\"decrease\",\"produtoId\":1,\"quantidade\":1}", Map.of());
        }
        assertThat(monitor.handleRequest(Map.of(), null)).containsEntry("alertou", false).containsEntry("quantidade", 2);

        for (int i = 0; i < 4; i++) {
            naDlq("{\"notaId\":" + i + ",\"xmlPath\":\"s3://b/" + i + ".xml\"}", Map.of("origemFila", "sefaz-queue"));
        }
        assertThat(monitor.handleRequest(Map.of(), null)).containsEntry("alertou", true).containsEntry("quantidade", 6);
        List<JsonNode> recebidos = new ArrayList<>();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            recebidos.addAll(alertas());
            assertThat(recebidos).hasSize(1);
        });
        JsonNode a = recebidos.get(0);
        assertThat(a.path("MessageType").asText()).isEqualTo("error");
        assertThat(a.path("severity").asText()).isEqualTo("high");
        assertThat(a.path("details").path("quantidade").asInt()).isEqualTo(6);
        assertThat(a.path("details").path("amostra")).isNotEmpty();
        assertThat(a.path("details").path("amostra").get(0).path("origem").asText()).isNotBlank();

        // mesma faixa: não repete; a amostra não tirou nada da DLQ
        assertThat(monitor.handleRequest(Map.of(), null)).containsEntry("alertou", false).containsEntry("quantidade", 6);
        assertThat(alertas()).isEmpty();
    }

    @Test
    void redriveListaSemMexerEDepoisDevolve() throws Exception {
        for (int i = 1; i <= 3; i++) {
            naDlq("{\"notaId\":" + (900 + i) + ",\"xmlPath\":\"s3://b/" + i + ".xml\",\"tentativa\":1}",
                    Map.of("origemFila", "sefaz-queue", "eventType", "send_sefaz"));
        }
        naDlq("{\"type\":\"decrease\",\"produtoId\":1,\"quantidade\":1}", Map.of("origemFila", "inventory-updates-queue"));
        var redrive = new DlqRedriveHandler(amb);

        Map<String, Object> lista = redrive.handleRequest(Map.of("origem", "sefaz-queue", "max", 10, "dryRun", true), null);
        assertThat(lista).containsEntry("encontradas", 3).containsEntry("reenviadas", 0);
        assertThat((List<?>) lista.get("mensagens")).hasSize(3);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(naDlqAgora()).isEqualTo(4));

        Map<String, Object> feito = redrive.handleRequest(Map.of("origem", "sefaz-queue", "dryRun", false), null);
        assertThat(feito).containsEntry("reenviadas", 3);
        List<Message> devolvidas = esvaziar("sefaz-queue");
        assertThat(devolvidas).hasSize(3).allSatisfy(m -> {
            assertThat(m.messageAttributes().get("redrive").stringValue()).isEqualTo("true");
            assertThat(m.messageAttributes().get("eventType").stringValue()).isEqualTo("send_sefaz");
        });
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(naDlqAgora()).isEqualTo(1));
        List<Message> sobrou = esvaziar("fiscal-dlq");
        assertThat(sobrou).hasSize(1);
        assertThat(sobrou.get(0).messageAttributes().get("origemFila").stringValue()).isEqualTo("inventory-updates-queue");
    }
}
