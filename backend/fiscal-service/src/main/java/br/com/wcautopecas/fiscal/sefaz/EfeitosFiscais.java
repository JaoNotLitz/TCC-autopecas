package br.com.wcautopecas.fiscal.sefaz;

import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.commons.seguranca.TokenServico;
import br.com.wcautopecas.fiscal.FiscalProperties;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Saídas comuns da autorização e dos eventos: operação de estoque e aviso de sistema. */
@Component
public class EfeitosFiscais {

    private static final Logger log = LoggerFactory.getLogger(EfeitosFiscais.class);

    private final Outbox outbox;
    private final EventPublisher publisher;
    private final FiscalProperties props;
    private final Clock clock;

    public EfeitosFiscais(Outbox outbox, EventPublisher publisher, FiscalProperties props, Clock clock) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.props = props;
        this.clock = clock;
    }

    /** Operação na {@code inventory-updates-queue} (increase | decrease), pelo outbox. */
    public void estoque(String tipo, long produto, long q, long referencia, String referenciaType, String motivo) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("type", tipo);
        op.put("produtoId", produto);
        op.put("quantidade", q);
        op.put("referenciaId", referencia);
        op.put("referenciaType", referenciaType);
        op.put("usuarioId", TokenServico.USUARIO_SISTEMA);
        op.put("motivo", motivo);
        outbox.fila(props.getFilaEstoque(), tipo, op);
    }

    /** {@code fiscal-error} no tópico de sistema; high e critical viram e-mail. */
    public void avisar(String severidade, String mensagem) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("MessageType", "fiscal-error");
        msg.put("service", "fiscal-service");
        msg.put("severity", severidade);
        msg.put("message", mensagem);
        msg.put("timestamp", clock.instant().toString());
        try {
            publisher.publicarTopico(props.getTopicoSistema(), "fiscal-error", msg, Map.of("severity", severidade));
        } catch (Exception e) {
            log.error("falha ao publicar aviso de sistema", e);
        }
    }
}
