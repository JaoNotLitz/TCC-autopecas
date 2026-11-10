package br.com.wcautopecas.fiscal.xml;

import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/** Consome a {@code fiscal-jobs-queue} (EPB-08), 3 mensagens em paralelo. */
@Component
public class FiscalProcessorListener {

    private static final Logger log = LoggerFactory.getLogger(FiscalProcessorListener.class);

    private final ProcessadorXml processador;
    private final ObjectMapper mapper;

    public FiscalProcessorListener(ProcessadorXml processador, ObjectMapper mapper) {
        this.processador = processador;
        this.mapper = mapper;
    }

    @SqsListener(value = "${wc.fiscal.fila-jobs}", maxConcurrentMessages = "3", maxMessagesPerPoll = "3",
            messageVisibilitySeconds = "${wc.fiscal.visibilidade-jobs:120}")
    public void receber(Message<String> mensagem) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            JsonNode job = mapper.readTree(mensagem.getPayload());
            if (!"generate_xml".equals(job.path("jobType").asText())) {
                log.warn("job desconhecido na fila fiscal: {}", job.path("jobType").asText());
                return;
            }
            processador.processar(job.path("notaId").asLong());
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }
}
