package br.com.wcautopecas.notification.cliente;

import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import br.com.wcautopecas.notification.NotificacoesProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * HU-B15.2: eventos da customer-events-queue. Cadastro com e-mail (ou troca de e-mail) vira um
 * pedido {@code welcome} na email-queue; o resto é só log de auditoria.
 */
@Component
public class CustomerEventsListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventsListener.class);

    private final Outbox outbox;
    private final ObjectMapper mapper;
    private final NotificacoesProperties props;

    public CustomerEventsListener(Outbox outbox, ObjectMapper mapper, NotificacoesProperties props) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.props = props;
    }

    @SqsListener(value = "${wc.notificacoes.fila-clientes}", maxConcurrentMessages = "3", maxMessagesPerPoll = "3")
    public void receber(Message<String> mensagem) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            processar(mapper.readTree(mensagem.getPayload()));
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    void processar(JsonNode evento) {
        String tipo = evento.path("eventType").asText();
        JsonNode d = evento.path("data");
        long id = evento.path("customerId").asLong();
        String email = d.path("email").asText("").trim();
        switch (tipo) {
            case "customer.created" -> {
                if (email.isEmpty()) {
                    log.info("cliente {} cadastrado sem e-mail; sem boas-vindas", id);
                    return;
                }
                boasVindas(email, d.path("nome").asText(), false);
            }
            case "customer.updated" -> {
                if (d.has("emailAnterior") && !email.isEmpty()
                        && !email.equalsIgnoreCase(d.path("emailAnterior").asText())) {
                    boasVindas(email, d.path("nome").asText(), true);
                } else {
                    log.info("auditoria: cliente {} atualizado", id);
                }
            }
            case "customer.deactivated" -> log.info("auditoria: cliente {} desativado", id);
            default -> log.info("evento de cliente {} ignorado", tipo);
        }
    }

    private void boasVindas(String email, String nome, boolean atualizacao) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", nome);
        dados.put("atualizacao", atualizacao);
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("to", email);
        pedido.put("cc", List.of());
        pedido.put("subject", atualizacao ? "Seu e-mail foi atualizado na WC Autopeças" : "Bem-vindo(a) à WC Autopeças");
        pedido.put("template", "welcome");
        pedido.put("data", dados);
        pedido.put("priority", "normal");
        outbox.fila(props.getFilaEmail(), "welcome", pedido);
    }
}
