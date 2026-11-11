package br.com.wcautopecas.entry.entrada;

import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Seção 8.6: consome {@code entry.uploaded} da {@code entry-events-queue} (3 em paralelo, visibility 60 s).
 * Erro passageiro relança (a SQS tenta 3 vezes e depois manda para a {@code fiscal-dlq}); XML ruim já
 * sai do {@link ImportacaoXmlService} como entrada {@code erro} e mensagem na DLQ.
 */
@Component
public class XmlProcessorListener {

    private static final Logger log = LoggerFactory.getLogger(XmlProcessorListener.class);

    private final ImportacaoXmlService importacao;
    private final ObjectMapper mapper;

    public XmlProcessorListener(ImportacaoXmlService importacao, ObjectMapper mapper) {
        this.importacao = importacao;
        this.mapper = mapper;
    }

    @SqsListener(value = "${wc.entradas.fila-entradas}", maxConcurrentMessages = "3", maxMessagesPerPoll = "3",
            messageVisibilitySeconds = "60")
    public void receber(Message<String> mensagem) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            JsonNode m = mapper.readTree(mensagem.getPayload());
            if (!ImportacaoXmlService.EVENTO.equals(m.path("eventType").asText())) {
                log.warn("evento ignorado na fila de entradas: {}", m.path("eventType").asText());
                return;
            }
            boolean redrive = "true".equals(String.valueOf(mensagem.getHeaders().get("redrive")));
            importacao.processar(m.path("entryId").asLong(), m.path("s3Key").asText(null), redrive);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }
}
