package br.com.wcautopecas.customer.eventos;

import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.customer.ClientesProperties;
import br.com.wcautopecas.customer.cliente.ClienteItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Eventos da seção 6.1 na {@code customer-events-queue} (HU-B03.6). Se o envio falhar, a gravação
 * não é desfeita: o evento vai para uma lista no Redis (outbox simplificado) e um job reenvia a
 * cada minuto.
 */
@Component
public class ClienteEventos {

    public static final String CRIADO = "customer.created";
    public static final String ATUALIZADO = "customer.updated";
    public static final String DESATIVADO = "customer.deactivated";
    static final String OUTBOX = "outbox:customer-events";

    private static final Logger log = LoggerFactory.getLogger(ClienteEventos.class);

    private final EventPublisher publisher;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ClientesProperties props;
    private final Clock clock;

    public ClienteEventos(EventPublisher publisher, StringRedisTemplate redis, ObjectMapper mapper,
            ClientesProperties props, Clock clock) {
        this.publisher = publisher;
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
    }

    public void publicar(String eventType, ClienteItem c, String emailAnterior) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", c.getId());
        data.put("nome", c.getNome());
        data.put("email", c.getEmail());
        data.put("documento", c.getDocumento());
        if (emailAnterior != null) {
            data.put("emailAnterior", emailAnterior);
        }
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("eventType", eventType);
        evento.put("customerId", c.getId());
        evento.put("timestamp", clock.instant().toString());
        evento.put("data", data);
        try {
            publisher.enviarFila(props.getFilaEventos(), eventType, evento);
        } catch (Exception e) {
            log.error("falha ao publicar {} do cliente {}; guardado para reenvio", eventType, c.getId(), e);
            guardar(eventType, evento);
        }
    }

    private void guardar(String eventType, Map<String, Object> evento) {
        try {
            redis.opsForList().rightPush(OUTBOX, mapper.writeValueAsString(Map.of("eventType", eventType,
                    "evento", evento)));
        } catch (Exception e) {
            log.error("evento {} perdido: nem a fila nem o Redis responderam", eventType, e);
        }
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void reenviarPendentes() {
        String json;
        while ((json = redis.opsForList().leftPop(OUTBOX)) != null) {
            try {
                var pendente = mapper.readTree(json);
                publisher.enviarFila(props.getFilaEventos(), pendente.get("eventType").asText(),
                        mapper.treeToValue(pendente.get("evento"), Map.class));
            } catch (Exception e) {
                redis.opsForList().leftPush(OUTBOX, json); // tenta de novo no próximo ciclo
                log.warn("reenvio de evento de cliente falhou: {}", e.getMessage());
                return;
            }
        }
    }
}
