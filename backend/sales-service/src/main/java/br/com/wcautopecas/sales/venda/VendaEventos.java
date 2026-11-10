package br.com.wcautopecas.sales.venda;

import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.sales.VendasProperties;
import br.com.wcautopecas.sales.integracao.Integracoes.Cliente;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Mensagens da venda (HU-B06.4, B06.6 e B06.7), todas pelo {@link Outbox}: se a fila ou o tópico
 * falharem, a venda continua gravada e o evento é reenviado depois.
 */
@Component
public class VendaEventos {

    public static final String SALE_CREATED = "sale.created";
    public static final String SALE_COMPLETED = "sale_completed";

    private final Outbox outbox;
    private final VendasProperties props;
    private final Clock clock;

    public VendaEventos(Outbox outbox, VendasProperties props, Clock clock) {
        this.outbox = outbox;
        this.props = props;
        this.clock = clock;
    }

    /** Um {@code reserve} por produto (linhas do mesmo produto somadas; a chave de idempotência é por produto). */
    public void reservar(VendaItem v, List<VendaItemLinha> linhas, long usuarioId) {
        estoque("reserve", v, linhas, usuarioId);
    }

    public void liberar(VendaItem v, List<VendaItemLinha> linhas, long usuarioId) {
        estoque("release", v, linhas, usuarioId);
    }

    private void estoque(String tipo, VendaItem v, List<VendaItemLinha> linhas, long usuarioId) {
        Map<Long, Long> somas = new LinkedHashMap<>();
        linhas.forEach(l -> somas.merge(l.getProdutoId(), l.getQuantidade(), Long::sum));
        somas.forEach((produtoId, quantidade) -> {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("type", tipo);
            op.put("produtoId", produtoId);
            op.put("quantidade", quantidade);
            op.put("referenciaId", v.getId());
            op.put("referenciaType", "sale");
            op.put("usuarioId", usuarioId);
            op.put("motivo", "Venda " + v.getNumero());
            outbox.fila(props.getFilaEstoque(), tipo, op);
        });
    }

    /** {@code sale.created} para o fiscal e o customer; {@code sale_completed} se o cliente tem e-mail. */
    public void vendaFinalizada(VendaItem v, Cliente cliente) {
        String agora = clock.instant().toString();
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("eventType", SALE_CREATED);
        evento.put("saleId", v.getId());
        evento.put("tipoDocumento", v.getTipoDocumento());
        evento.put("clienteId", v.getClienteId());
        evento.put("valorTotal", v.getTotal());
        evento.put("timestamp", agora);
        outbox.topico(props.getTopicoVendas(), SALE_CREATED, evento, Map.of());

        if (cliente != null && cliente.email() != null && !cliente.email().isBlank()
                && v.getTotal().compareTo(props.getNotificacaoMinimo()) >= 0) {
            Map<String, Object> aviso = new LinkedHashMap<>();
            aviso.put("MessageType", SALE_COMPLETED);
            aviso.put("vendaId", v.getId());
            aviso.put("numero", v.getNumero());
            aviso.put("clienteNome", cliente.nome());
            aviso.put("clienteEmail", cliente.email());
            aviso.put("valorTotal", v.getTotal());
            aviso.put("notaFiscalId", v.getNotaFiscalId());
            aviso.put("timestamp", agora);
            outbox.topico(props.getTopicoNotificacoes(), SALE_COMPLETED, aviso, Map.of());
        }
    }
}
