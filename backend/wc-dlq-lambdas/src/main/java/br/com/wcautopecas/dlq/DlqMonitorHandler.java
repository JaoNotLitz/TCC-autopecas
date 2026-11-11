package br.com.wcautopecas.dlq;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * HU-B16.1: disparada pelo EventBridge a cada minuto. Acima do limite, publica um alerta
 * {@code high} no system-notifications-topic (que vira e-mail para a operação), uma vez por faixa:
 * com limite 5, as faixas são 6-10, 11-15, 16-20... A última faixa alertada fica na tabela
 * {@code counters} (item {@code dlq-monitor}).
 */
public class DlqMonitorHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    static final String ITEM = "dlq-monitor";
    static final ObjectMapper JSON = new ObjectMapper();

    private final Ambiente amb;
    private final SqsClient sqs;
    private final SnsClient sns;
    private final DynamoDbClient dynamo;

    public DlqMonitorHandler() {
        this(Ambiente.doSistema());
    }

    DlqMonitorHandler(Ambiente amb) {
        this.amb = amb;
        this.sqs = amb.sqs();
        this.sns = amb.sns();
        this.dynamo = amb.dynamo();
    }

    /** Faixa da quantidade: 0 até o limite; depois 1, 2, 3... a cada {@code limite} mensagens. */
    static int faixa(int quantidade, int limite) {
        return quantidade <= limite ? 0 : (quantidade - 1) / limite;
    }

    /** Alerta só quando sobe para uma faixa acima de 0 que ainda não foi alertada. */
    static boolean alertar(int faixaAtual, int ultimaFaixa) {
        return faixaAtual > 0 && faixaAtual > ultimaFaixa;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> evento, Context context) {
        String url = LeitorDlq.url(sqs, amb.dlq());
        int quantidade = Integer.parseInt(sqs.getQueueAttributes(r -> r.queueUrl(url)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
                .attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
        int atual = faixa(quantidade, amb.limite());
        int ultima = ultimaFaixa();
        boolean alertou = alertar(atual, ultima);
        if (alertou) {
            publicar(url, quantidade);
        }
        if (atual != ultima) {
            guardar(atual);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("quantidade", quantidade);
        r.put("limite", amb.limite());
        r.put("faixa", atual);
        r.put("alertou", alertou);
        System.out.println("wc-dlq-monitor " + r);
        return r;
    }

    private void publicar(String url, int quantidade) {
        List<MensagemDlq> amostra = LeitorDlq.ler(sqs, url, 5, 5);
        LeitorDlq.liberar(sqs, url, amostra);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("quantidade", quantidade);
        details.put("amostra", amostra.stream().map(m -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("origem", m.origem());
            a.put("motivo", (m.eventType() == null ? "mensagem" : m.eventType()) + " falhou "
                    + Math.max(m.recebimentos() - 1, 0) + "x na origem antes da DLQ");
            return a;
        }).toList());
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("MessageType", "error");
        msg.put("service", amb.dlq());
        msg.put("severity", "high");
        msg.put("message", quantidade + " mensagens na " + amb.dlq() + " (limite " + amb.limite()
                + "). Veja com make redrive ARGS='{\"dryRun\":true}'.");
        msg.put("details", details);
        msg.put("timestamp", Instant.now().toString());
        String arn = sns.createTopic(r -> r.name(amb.topico())).topicArn();
        try {
            String corpo = JSON.writeValueAsString(msg);
            sns.publish(r -> r.topicArn(arn).message(corpo).messageAttributes(Map.of(
                    "severity", MessageAttributeValue.builder().dataType("String").stringValue("high").build(),
                    "eventType", MessageAttributeValue.builder().dataType("String").stringValue("error").build())));
        } catch (Exception e) {
            throw new IllegalStateException("falha ao publicar o alerta da DLQ", e);
        }
    }

    private int ultimaFaixa() {
        var item = dynamo.getItem(r -> r.tableName(amb.tabelaCounters()).consistentRead(true)
                .key(Map.of("recurso", AttributeValue.fromS(ITEM)))).item();
        return item == null || !item.containsKey("faixa") ? 0 : Integer.parseInt(item.get("faixa").n());
    }

    private void guardar(int faixa) {
        dynamo.putItem(r -> r.tableName(amb.tabelaCounters()).item(Map.of(
                "recurso", AttributeValue.fromS(ITEM), "faixa", AttributeValue.fromN(String.valueOf(faixa)),
                "atualizadoEm", AttributeValue.fromS(Instant.now().toString()))));
    }
}
