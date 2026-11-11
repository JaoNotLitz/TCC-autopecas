package br.com.wcautopecas.customer.api;

import br.com.wcautopecas.customer.cliente.ClienteItem;
import br.com.wcautopecas.customer.cliente.ClienteRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas internas (decisão A4): o gateway não roteia {@code /internal}. */
@RestController
@RequestMapping("/internal/clientes")
public class InternoController {

    private final ClienteRepository repo;

    public InternoController(ClienteRepository repo) {
        this.repo = repo;
    }

    /** EPB-14: números do dashboard. {@code novos} conta os cadastrados a partir de {@code desde} (ISO-8601). */
    @GetMapping("/resumo")
    public Resumo resumo(@RequestParam(required = false) String desde) {
        Instant inicio = desde == null || desde.isBlank() ? Instant.EPOCH : Instant.parse(desde);
        List<ClienteItem> todos = repo.todos();
        long ativos = todos.stream().filter(c -> Boolean.TRUE.equals(c.getAtivo())).count();
        long novos = todos.stream().filter(c -> criadoDesde(c, inicio)).count();
        return new Resumo(todos.size(), ativos, novos);
    }

    private static boolean criadoDesde(ClienteItem c, Instant inicio) {
        try {
            return c.getDataCriacao() != null && !Instant.parse(c.getDataCriacao()).isBefore(inicio);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public record Resumo(long total, long ativos, long novos) {
    }
}
