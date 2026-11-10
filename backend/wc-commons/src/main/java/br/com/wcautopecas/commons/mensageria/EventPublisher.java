package br.com.wcautopecas.commons.mensageria;

import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Publica eventos em JSON. Toda mensagem leva os atributos {@code eventType} e
 * {@code correlationId} (mais os extras informados, usados em filter policies do SNS).
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final SqsClient sqs;
    private final SnsClient sns;
    private final ObjectMapper mapper;
    private final Map<String, String> filas = new ConcurrentHashMap<>();
    private final Map<String, String> topicos = new ConcurrentHashMap<>();

    public EventPublisher(SqsClient sqs, SnsClient sns, ObjectMapper mapper) {
        this.sqs = sqs;
        this.sns = sns;
        this.mapper = mapper;
    }

    public void enviarFila(String fila, String eventType, Object evento) {
        enviarFila(fila, eventType, evento, Map.of());
    }

    public void enviarFila(String fila, String eventType, Object evento, Map<String, String> atributos) {
        String url = filas.computeIfAbsent(fila, n -> sqs.getQueueUrl(r -> r.queueName(n)).queueUrl());
        Map<String, software.amazon.awssdk.services.sqs.model.MessageAttributeValue> attrs = new HashMap<>();
        atributos(eventType, atributos).forEach((k, v) -> attrs.put(k,
                software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                        .dataType("String").stringValue(v).build()));
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(json(evento)).messageAttributes(attrs));
        log.info("evento {} enviado para a fila {}", eventType, fila);
    }

    public void publicarTopico(String topico, String eventType, Object evento) {
        publicarTopico(topico, eventType, evento, Map.of());
    }

    public void publicarTopico(String topico, String eventType, Object evento, Map<String, String> atributos) {
        String arn = topicos.computeIfAbsent(topico, n -> sns.createTopic(r -> r.name(n)).topicArn());
        Map<String, software.amazon.awssdk.services.sns.model.MessageAttributeValue> attrs = new HashMap<>();
        atributos(eventType, atributos).forEach((k, v) -> attrs.put(k,
                software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder()
                        .dataType("String").stringValue(v).build()));
        sns.publish(r -> r.topicArn(arn).message(json(evento)).messageAttributes(attrs));
        log.info("evento {} publicado no tópico {}", eventType, topico);
    }

    private Map<String, String> atributos(String eventType, Map<String, String> extras) {
        Map<String, String> attrs = new HashMap<>(extras);
        attrs.put("eventType", eventType);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        attrs.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        return attrs;
    }

    private String json(Object evento) {
        try {
            return evento instanceof String s ? s : mapper.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evento não serializável", e);
        }
    }
}
