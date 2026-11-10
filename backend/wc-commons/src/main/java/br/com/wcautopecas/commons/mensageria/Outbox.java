package br.com.wcautopecas.commons.mensageria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Publicação que não perde evento (outbox simplificado, HU-B03.6/B06.4): tenta enviar na hora;
 * se a fila ou o tópico falharem, guarda o evento numa lista do Redis e um job reenvia a cada
 * minuto. A gravação de negócio nunca é desfeita por falha de mensageria.
 */
public class Outbox {

    private static final Logger log = LoggerFactory.getLogger(Outbox.class);

    private final EventPublisher publisher;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final String chave;

    public Outbox(EventPublisher publisher, StringRedisTemplate redis, ObjectMapper mapper, String servico) {
        this.publisher = publisher;
        this.redis = redis;
        this.mapper = mapper;
        this.chave = "outbox:" + servico;
    }

    public void fila(String fila, String eventType, Object evento) {
        enviar("fila", fila, eventType, evento, Map.of());
    }

    public void topico(String topico, String eventType, Object evento, Map<String, String> atributos) {
        enviar("topico", topico, eventType, evento, atributos);
    }

    private void enviar(String tipo, String destino, String eventType, Object evento, Map<String, String> attrs) {
        try {
            despachar(tipo, destino, eventType, evento, attrs);
        } catch (Exception e) {
            log.error("falha ao enviar {} para {}; guardado para reenvio", eventType, destino, e);
            Map<String, Object> pendente = new LinkedHashMap<>();
            pendente.put("tipo", tipo);
            pendente.put("destino", destino);
            pendente.put("eventType", eventType);
            pendente.put("evento", evento);
            pendente.put("atributos", attrs);
            try {
                redis.opsForList().rightPush(chave, mapper.writeValueAsString(pendente));
            } catch (Exception ex) {
                log.error("evento {} perdido: nem a mensageria nem o Redis responderam", eventType, ex);
            }
        }
    }

    private void despachar(String tipo, String destino, String eventType, Object evento, Map<String, String> attrs) {
        if ("fila".equals(tipo)) {
            publisher.enviarFila(destino, eventType, evento, attrs);
        } else {
            publisher.publicarTopico(destino, eventType, evento, attrs);
        }
    }

    /** Reenvia o que ficou pendente; para no primeiro erro e tenta de novo no próximo ciclo. */
    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void reenviar() {
        String json;
        try {
            while ((json = redis.opsForList().leftPop(chave)) != null) {
                JsonNode p = mapper.readTree(json);
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> attrs = mapper.treeToValue(p.get("atributos"), Map.class);
                    despachar(p.get("tipo").asText(), p.get("destino").asText(), p.get("eventType").asText(),
                            mapper.treeToValue(p.get("evento"), Object.class), attrs);
                } catch (Exception e) {
                    redis.opsForList().leftPush(chave, json);
                    log.warn("reenvio do outbox falhou: {}", e.getMessage());
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("outbox indisponível: {}", e.getMessage());
        }
    }

    public long pendentes() {
        Long n = redis.opsForList().size(chave);
        return n == null ? 0 : n;
    }
}
