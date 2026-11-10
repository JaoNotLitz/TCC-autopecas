package br.com.wcautopecas.fiscal.emissao;

import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.evento.EventoFiscal;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Mensagens do fiscal-service, pelo {@link Outbox} (não se perdem se a fila cair). */
@Component
public class FiscalMensagens {

    public static final String GENERATE_XML = "generate_xml";

    private final Outbox outbox;
    private final FiscalProperties props;
    private final Clock clock;

    public FiscalMensagens(Outbox outbox, FiscalProperties props, Clock clock) {
        this.outbox = outbox;
        this.props = props;
        this.clock = clock;
    }

    /** Job do EPB-08 na {@code fiscal-jobs-queue}. */
    public void gerarXml(NotaItem n) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobType", GENERATE_XML);
        job.put("notaId", n.getId());
        job.put("tipoNota", n.getTipo());
        job.put("timestamp", clock.instant().toString());
        outbox.fila(props.getFilaJobs(), GENERATE_XML, job);
    }

    /** Pedido de evento à SEFAZ na {@code fiscal-events-queue} (EPB-10). */
    public void eventoFiscal(EventoFiscal e, NotaItem nota) {
        String tipo = EventoFiscal.CANCELAMENTO.equals(e.getTipo()) ? "cancelamento" : "inutilizacao";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventType", tipo);
        m.put("eventoId", e.getId());
        if (nota != null) {
            m.put("notaId", nota.getId());
            m.put("chaveAcesso", nota.getChaveAcesso());
        } else {
            m.put("modelo", e.getModelo());
            m.put("serie", e.getSerie());
            m.put("numeroInicial", e.getNumeroInicial());
            m.put("numeroFinal", e.getNumeroFinal());
        }
        m.put("justificativa", e.getJustificativa());
        m.put("timestamp", clock.instant().toString());
        outbox.fila(props.getFilaEventos(), tipo, m);
    }
}
