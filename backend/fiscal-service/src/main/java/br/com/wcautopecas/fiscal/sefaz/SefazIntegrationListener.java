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
 * Consome a {@code sefaz-queue} (EPB-09), 2 mensagens em paralelo. Erro de comunicação: muda a
 * visibility para espaçar as tentativas (imediata, 1 min, 5 min) e relança; na 3ª falha a SQS manda
 * para a {@code fiscal-dlq}.
 */
@Component
public class SefazIntegrationListener {

    private final AutorizacaoService autorizacao;
    private final ObjectMapper mapper;

    public SefazIntegrationListener(AutorizacaoService autorizacao, ObjectMapper mapper) {
        this.autorizacao = autorizacao;
        this.mapper = mapper;
    }

    @SqsListener(value = "${wc.fiscal.fila-sefaz}", maxConcurrentMessages = "2", maxMessagesPerPoll = "2",
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
                autorizacao.processar(m.path("notaId").asLong(), m.path("tentativa").asInt(1), recebimento);
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
