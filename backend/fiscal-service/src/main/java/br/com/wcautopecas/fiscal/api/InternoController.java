package br.com.wcautopecas.fiscal.api;

import br.com.wcautopecas.fiscal.evento.EventoFiscal;
import br.com.wcautopecas.fiscal.evento.EventoRepository;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import br.com.wcautopecas.fiscal.nota.NotaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas internas (decisão A4): nota completa para o document-service e os testes ponta a ponta. */
@RestController
@RequestMapping("/internal/notas")
public class InternoController {

    private final NotaService notas;
    private final NotaRepository repo;
    private final EventoRepository eventos;

    public InternoController(NotaService notas, NotaRepository repo, EventoRepository eventos) {
        this.notas = notas;
        this.repo = repo;
        this.eventos = eventos;
    }

    /** {@code eventos}: cancelamentos pedidos para a nota, com resultado na SEFAZ (HU-B10.3 RF03). */
    public record NotaCompleta(NotaItem nota, List<NotaLinha> itens, List<Evento> eventos) {
    }

    public record Evento(long id, String tipo, String status, String protocolo, String codigoStatus, String motivo,
            String justificativa, String registradoEm) {

        static Evento de(EventoFiscal e) {
            return new Evento(e.getId(), e.getTipo(), e.getStatus(), e.getProtocolo(), e.getCodigoStatus(),
                    e.getMotivoRejeicao(), e.getJustificativa(), e.getRegistradoEm());
        }
    }

    @GetMapping("/{id}")
    public NotaCompleta buscar(@PathVariable long id) {
        NotaItem n = notas.existente(id);
        List<Evento> lista = eventos.daNota(id).stream()
                .sorted(java.util.Comparator.comparing(EventoFiscal::getId)).map(Evento::de).toList();
        return new NotaCompleta(n, repo.itens(id), lista);
    }
}
