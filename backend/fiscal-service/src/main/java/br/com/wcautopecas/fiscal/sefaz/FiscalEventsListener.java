package br.com.wcautopecas.fiscal.sefaz;

import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.time.Duration;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consome a {@code fiscal-events-queue} (EPB-10), 2 mensagens em paralelo. Mesma política de erro
 * da {@link SefazIntegrationListener}: espera crescente entre tentativas e DLQ na 3ª falha.
 */
@Component
public class FiscalEventsListener {

    private final EventosSefazService eventos;
    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;

    public FiscalEventsListener(EventosSefazService eventos, AutorizacaoService autorizacao, ObjectMapper mapper) {
        this.eventos = eventos;
        this.autorizacao = autorizacao;
        this.mapper = mapper;
    }

    @SqsListener(value = "${wc.fiscal.fila-eventos}", maxConcurrentMessages = "2", maxMessagesPerPoll = "2",
            messageVisibilitySeconds = "${wc.fiscal.visibilidade-sefaz:300}")
    public void receber(Message<String> mensagem, Visibility visibility) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            JsonNode m = mapper.readTree(mensagem.getPayload());
            Object recebidas = mensagem.getHeaders()
                    .get(SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT);
            int recebimento = recebidas == null ? 1 : Integer.parseInt(recebidas.toString());
            try {
                eventos.processar(m.path("eventoId").asLong(), recebimento);
            } catch (SefazClient.ComunicacaoException e) {
                Duration espera = autorizacao.esperaDepoisDaFalha(recebimento);
                visibility.changeTo((int) espera.toSeconds());
                throw e;
            }
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }
}
