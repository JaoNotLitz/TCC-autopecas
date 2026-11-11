package br.com.wcautopecas.dlq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

/** Uma mensagem lida da DLQ, com a fila de origem já identificada. */
public record MensagemDlq(String id, String receiptHandle, String origem, String eventType, int recebimentos,
        String corpo, Map<String, MessageAttributeValue> atributos) {

    static final ObjectMapper JSON = new ObjectMapper();

    public static MensagemDlq de(Message m) {
        Map<String, String> attrs = new HashMap<>();
        m.messageAttributes().forEach((k, v) -> {
            if (v.stringValue() != null) {
                attrs.put(k, v.stringValue());
            }
        });
        Map<String, String> sistema = new HashMap<>();
        m.attributesAsStrings().forEach(sistema::put);
        String recebidas = m.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
        return new MensagemDlq(m.messageId(), m.receiptHandle(), origem(attrs, sistema, m.body()),
                eventType(attrs, m.body()), recebidas == null ? 0 : Integer.parseInt(recebidas), m.body(),
                m.messageAttributes());
    }

    /**
     * HU-B16.2 RF02: o atributo {@code origemFila} (posto pelo EventPublisher do wc-commons) vale
     * primeiro; depois a origem que a SQS registra na DLQ; por último o formato do corpo, para as
     * mensagens que chegam por assinatura SNS ou foram enviadas antes do atributo existir.
     */
    static String origem(Map<String, String> attrs, Map<String, String> sistema, String corpo) {
        String origem = attrs.get("origemFila");
        if (origem != null && !origem.isBlank()) {
            return origem;
        }
        String arn = sistema.get("DeadLetterQueueSourceArn");
        if (arn != null && arn.contains(":")) {
            return arn.substring(arn.lastIndexOf(':') + 1);
        }
        JsonNode c = ler(corpo);
        String tipo = attrs.getOrDefault("eventType", c.path("eventType").asText(c.path("jobType").asText("")));
        if (c.has("jobType") || "generate_xml".equals(tipo)) {
            return "fiscal-jobs-queue";
        }
        if ("send_sefaz".equals(tipo) || (c.has("notaId") && c.has("xmlPath"))) {
            return "sefaz-queue";
        }
        if ("cancelamento".equals(tipo) || "inutilizacao".equals(tipo) || c.has("eventoId")) {
            return "fiscal-events-queue";
        }
        if ("nota.autorizada".equals(tipo)) {
            return "document-events-queue";
        }
        if ("sale.created".equals(tipo)) {
            return "sales-events-queue"; // também vai para a customer-sales-events-queue; o fiscal é quem importa
        }
        if (c.has("produtoId") && c.has("quantidade") && c.has("type")) {
            return "inventory-updates-queue";
        }
        return "desconhecida";
    }

    static String eventType(Map<String, String> attrs, String corpo) {
        if (attrs.containsKey("eventType")) {
            return attrs.get("eventType");
        }
        JsonNode c = ler(corpo);
        return c.path("eventType").asText(c.path("jobType").asText(c.path("type").asText(null)));
    }

    private static JsonNode ler(String corpo) {
        try {
            return JSON.readTree(corpo == null ? "{}" : corpo);
        } catch (Exception e) {
            return JSON.createObjectNode();
        }
    }

    /** Até 300 caracteres do corpo, para a listagem. */
    public String resumo() {
        return corpo == null || corpo.length() <= 300 ? corpo : corpo.substring(0, 300) + "...";
    }
}
