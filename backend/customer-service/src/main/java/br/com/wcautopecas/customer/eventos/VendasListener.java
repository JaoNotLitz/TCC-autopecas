package br.com.wcautopecas.customer.eventos;

import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import br.com.wcautopecas.customer.cliente.ClienteCache;
import br.com.wcautopecas.customer.cliente.ClienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Estatísticas de compras (HU-B03.7): consome {@code sale.created} da
 * {@code customer-sales-events-queue}, que assina o {@code sales-events-topic} (decisão A2).
 */
@Component
public class VendasListener {

    private static final Logger log = LoggerFactory.getLogger(VendasListener.class);
    private static final Duration JANELA_IDEMPOTENCIA = Duration.ofDays(7);

    private final ClienteRepository repo;
    private final ClienteCache cache;
    private final IdempotencyGuard idempotencia;
    private final ObjectMapper mapper;

    public VendasListener(ClienteRepository repo, ClienteCache cache, IdempotencyGuard idempotencia,
            ObjectMapper mapper) {
        this.repo = repo;
        this.cache = cache;
        this.idempotencia = idempotencia;
        this.mapper = mapper;
    }

    @SqsListener("${wc.clientes.fila-vendas}")
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
        if (!"sale.created".equals(tipo)) {
            return; // sale.completed e outros não mexem nas estatísticas
        }
        JsonNode clienteId = evento.get("clienteId");
        if (clienteId == null || clienteId.isNull()) {
            return; // NFC-e sem cliente identificado
        }
        String venda = evento.path("saleId").asText();
        String chave = "customer-sale:" + venda;
        if (!idempotencia.primeiraVez(chave, JANELA_IDEMPOTENCIA)) {
            log.info("venda {} já contabilizada", venda);
            return;
        }
        try {
            String quando = evento.hasNonNull("timestamp") ? evento.get("timestamp").asText() : Instant.now().toString();
            boolean ok = repo.registrarCompra(clienteId.asLong(), evento.path("valorTotal").asDouble(), quando);
            if (!ok) {
                log.warn("venda {} cita o cliente {}, que não existe; ignorada", venda, clienteId.asLong());
                return;
            }
            cache.invalidar(clienteId.asLong());
            log.info("compra da venda {} registrada no cliente {}", venda, clienteId.asLong());
        } catch (RuntimeException e) {
            idempotencia.liberar(chave); // deixa a mensagem voltar e ser reprocessada
            throw e;
        }
    }
}
