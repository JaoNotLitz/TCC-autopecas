package br.com.wcautopecas.product.estoque;

import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consome a {@code inventory-updates-queue} (EPB-05). Mensagem inválida ou impossível lança
 * exceção: a SQS reentrega até 3 vezes e depois manda para a {@code fiscal-dlq}.
 */
@Component
public class InventoryUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryUpdateListener.class);
    private static final Duration JANELA = Duration.ofDays(7);

    private final EstoqueService estoque;
    private final IdempotencyGuard idempotencia;
    private final ObjectMapper mapper;

    public InventoryUpdateListener(EstoqueService estoque, IdempotencyGuard idempotencia, ObjectMapper mapper) {
        this.estoque = estoque;
        this.idempotencia = idempotencia;
        this.mapper = mapper;
    }

    @SqsListener("${wc.produtos.fila-estoque}")
    public void receber(Message<String> mensagem) throws Exception {
        Object correlation = mensagem.getHeaders().get("correlationId");
        if (correlation != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlation.toString());
        }
        try {
            Object id = mensagem.getHeaders().get("id");
            processar(mensagem.getPayload(), id == null ? UUID.randomUUID().toString() : id.toString());
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    void processar(String corpo, String idMensagem) throws Exception {
        Operacao op;
        try {
            op = mapper.readValue(corpo, Operacao.class);
        } catch (Exception e) {
            log.error("mensagem de estoque inválida (MENSAGEM_INVALIDA): {}", corpo);
            throw e;
        }
        String chave = op.chave(idMensagem);
        if (!idempotencia.primeiraVez(chave, JANELA)) {
            log.info("operação {} já aplicada", chave);
            return;
        }
        try {
            var r = estoque.aplicar(op);
            log.info("estoque do produto {}: {} {} -> {} ({})", op.produtoId(), op.type(),
                    r.antes().getEstoqueAtual(), r.depois().getEstoqueAtual(), r.depois().getStatus());
        } catch (RuntimeException e) {
            idempotencia.liberar(chave);
            if (e instanceof EstoqueException ee) {
                log.error("operação de estoque recusada ({}): {}", ee.getMotivo(), corpo);
                if ("ESTOQUE_INSUFICIENTE".equals(ee.getMotivo())
                        && idempotencia.primeiraVez("alerta:" + chave, JANELA)) {
                    estoque.avisarSistema("Reserva recusada por estoque insuficiente: " + ee.getMessage());
                }
            }
            throw e;
        }
    }
}
