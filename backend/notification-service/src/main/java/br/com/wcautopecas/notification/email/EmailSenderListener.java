package br.com.wcautopecas.notification.email;

import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import br.com.wcautopecas.notification.NotificacoesProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * HU-B15.1 RF01: consome a email-queue com 5 concorrentes. Erro do SES devolve a mensagem para a
 * fila (visibility 30 s) até {@code maximoTentativas}; depois descarta com log ERROR, porque a
 * email-queue não tem DLQ (seção 6.8).
 */
@Component
public class EmailSenderListener {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderListener.class);

    private final PedidosEmail pedidos;
    private final EnvioEmail envio;
    private final ObjectMapper mapper;
    private final NotificacoesProperties props;

    public EmailSenderListener(PedidosEmail pedidos, EnvioEmail envio, ObjectMapper mapper,
            NotificacoesProperties props) {
        this.pedidos = pedidos;
        this.envio = envio;
        this.mapper = mapper;
        this.props = props;
    }

    @SqsListener(value = "${wc.notificacoes.fila-email}", maxConcurrentMessages = "5", maxMessagesPerPoll = "5")
    public void receber(Message<String> mensagem) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            JsonNode corpo;
            try {
                corpo = mapper.readTree(mensagem.getPayload());
            } catch (JsonProcessingException e) {
                log.error("mensagem da email-queue não é JSON; descartada: {}", e.getOriginalMessage());
                return;
            }
            var pedido = pedidos.de(corpo);
            if (pedido.isEmpty()) {
                return;
            }
            try {
                envio.enviar(pedido.get());
            } catch (SdkException e) {
                int tentativa = tentativa(mensagem);
                if (tentativa >= props.getMaximoTentativas()) {
                    log.error("SES falhou {} vezes para {} ({}); e-mail descartado: {}", tentativa,
                            pedido.get().to(), pedido.get().template(), e.getMessage());
                    return;
                }
                log.warn("SES falhou (tentativa {} de {}): {}", tentativa, props.getMaximoTentativas(),
                        e.getMessage());
                throw e;
            }
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    static int tentativa(Message<?> m) {
        Object n = m.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT);
        try {
            return n == null ? 1 : Integer.parseInt(n.toString());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
