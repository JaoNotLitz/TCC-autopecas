package br.com.wcautopecas.document.danfe;

import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/** HU-B13.1: {@code nota.autorizada} na {@code document-events-queue} (decisão A3) vira DANFE no S3. */
@Component
public class DanfeGeneratorListener {

    private static final Logger log = LoggerFactory.getLogger(DanfeGeneratorListener.class);

    private final DocumentoService documentos;
    private final ObjectMapper mapper;

    public DanfeGeneratorListener(DocumentoService documentos, ObjectMapper mapper) {
        this.documentos = documentos;
        this.mapper = mapper;
    }

    @SqsListener(value = "${wc.documentos.fila-documentos}", maxConcurrentMessages = "3", maxMessagesPerPoll = "3")
    public void receber(Message<String> mensagem) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            JsonNode m = mapper.readTree(mensagem.getPayload());
            String tipo = m.path("eventType").asText();
            if (!"nota.autorizada".equals(tipo)) {
                log.info("evento {} ignorado pelo document-service", tipo);
                return;
            }
            documentos.gerarAoAutorizar(m.path("notaId").asLong());
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }
}
