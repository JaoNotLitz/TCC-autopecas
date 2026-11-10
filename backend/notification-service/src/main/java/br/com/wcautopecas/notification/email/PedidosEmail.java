package br.com.wcautopecas.notification.email;

import br.com.wcautopecas.notification.NotificacoesProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Converte o que chega na email-queue em {@link PedidoEmail}. Dois formatos: o pedido pronto
 * ({@code template}) que os serviços enviam pela Outbox, e os alertas dos tópicos SNS
 * ({@code MessageType}, HU-B15.3), com ou sem o envelope SNS ({@code Type: Notification}).
 */
@Component
public class PedidosEmail {

    private static final Logger log = LoggerFactory.getLogger(PedidosEmail.class);
    static final Set<String> SISTEMA = Set.of("warning", "error", "fiscal-error");
    static final Set<String> SEVERIDADES_EMAIL = Set.of("critical", "high");
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {
    };

    private final ObjectMapper mapper;
    private final NotificacoesProperties props;

    public PedidosEmail(ObjectMapper mapper, NotificacoesProperties props) {
        this.mapper = mapper;
        this.props = props;
    }

    public Optional<PedidoEmail> de(JsonNode corpo) throws Exception {
        if ("Notification".equals(corpo.path("Type").asText()) && corpo.hasNonNull("Message")) {
            corpo = mapper.readTree(corpo.get("Message").asText());
        }
        if (corpo.hasNonNull("template")) {
            return Optional.of(new PedidoEmail(lista(corpo.get("to")), lista(corpo.get("cc")),
                    texto(corpo, "subject"), corpo.get("template").asText(), dados(corpo.get("data")),
                    texto(corpo, "priority")));
        }
        String tipo = corpo.path("MessageType").asText("");
        Map<String, Object> dados = dados(corpo);
        switch (tipo) {
            case "low_stock_alert" -> {
                return Optional.of(new PedidoEmail(props.getAlertaEstoqueDestinatarios(), List.of(),
                        "Estoque crítico: " + corpo.path("nome").asText(corpo.path("codigo").asText()),
                        "low-stock-alert", dados, "high"));
            }
            case "sale_completed" -> {
                return Optional.of(new PedidoEmail(lista(corpo.get("clienteEmail")), List.of(),
                        "Sua compra na WC Autopeças" + (corpo.hasNonNull("numero")
                                ? " (venda nº " + corpo.get("numero").asText() + ")" : ""),
                        "sale-confirmation", dados, "normal"));
            }
            default -> {
                if (SISTEMA.contains(tipo)) {
                    String severidade = corpo.path("severity").asText("");
                    if (!SEVERIDADES_EMAIL.contains(severidade)) {
                        log.info("alerta {} de severidade {} não vira e-mail", tipo, severidade);
                        return Optional.empty();
                    }
                    return Optional.of(new PedidoEmail(props.getOpsDestinatarios(), List.of(),
                            "[" + severidade.toUpperCase() + "] Alerta do " + corpo.path("service").asText("sistema"),
                            "fiscal-error", dados, severidade));
                }
                log.warn("mensagem sem template nem MessageType conhecido ({}); descartada", tipo);
                return Optional.empty();
            }
        }
    }

    /** {@code to} e {@code cc} podem vir como texto (um ou vários separados por vírgula) ou array. */
    static List<String> lista(JsonNode n) {
        List<String> r = new ArrayList<>();
        if (n == null || n.isNull()) {
            return r;
        }
        if (n.isArray()) {
            n.forEach(x -> r.addAll(lista(x)));
            return r;
        }
        for (String s : n.asText().split(",")) {
            if (!s.isBlank()) {
                r.add(s.trim());
            }
        }
        return r;
    }

    private Map<String, Object> dados(JsonNode n) {
        return n == null || !n.isObject() ? new LinkedHashMap<>() : mapper.convertValue(n, MAPA);
    }

    private static String texto(JsonNode n, String campo) {
        return n.hasNonNull(campo) ? n.get(campo).asText() : null;
    }
}
