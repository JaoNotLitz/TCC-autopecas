package br.com.wcautopecas.fiscal.emissao;

import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consome {@code sale.created} da {@code sales-events-queue} (HU-B07.1). Erro relança: a SQS
 * reentrega e, depois de 3 tentativas, manda para a {@code fiscal-dlq}. Um lock curto no Redis
 * evita duas notas quando a mesma mensagem é entregue em paralelo.
 */
@Component
public class SalesEventListener {

    private static final Logger log = LoggerFactory.getLogger(SalesEventListener.class);
    private static final Duration LOCK = Duration.ofMinutes(2);

    private final CriarNotaDeVenda criar;
    private final IdempotencyGuard locks;
    private final ObjectMapper mapper;

    public SalesEventListener(CriarNotaDeVenda criar, IdempotencyGuard locks, ObjectMapper mapper) {
        this.criar = criar;
        this.locks = locks;
        this.mapper = mapper;
    }

    @SqsListener(value = "${wc.fiscal.fila-vendas}", maxConcurrentMessages = "5", maxMessagesPerPoll = "5")
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
        if (!"sale.created".equals(evento.path("eventType").asText())) {
            log.warn("evento ignorado na fila de vendas: {}", evento.path("eventType").asText());
            return;
        }
        long vendaId = evento.path("saleId").asLong();
        String lock = "fiscal-venda:" + vendaId;
        if (!locks.primeiraVez(lock, LOCK)) {
            throw new IllegalStateException("venda " + vendaId + " já está sendo processada; tenta de novo depois");
        }
        try {
            criar.criar(vendaId);
        } finally {
            locks.liberar(lock);
        }
    }
}
