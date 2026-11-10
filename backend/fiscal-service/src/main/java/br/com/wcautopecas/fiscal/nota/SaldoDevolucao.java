package br.com.wcautopecas.fiscal.nota;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Saldo por produto da nota original (fica na primeira linha de cada produto). A devolução criada
 * soma "em devolução"; autorizada, passa para "devolvida"; rejeitada, só libera; reenviada, reserva de novo.
 */
@Component
public class SaldoDevolucao {

    public enum Evento { REENVIADA, AUTORIZADA, REJEITADA }

    private final NotaRepository repo;

    public SaldoDevolucao(NotaRepository repo) {
        this.repo = repo;
    }

    public void aplicar(NotaItem devolucao, Evento evento) {
        if (!"DEVOLUCAO".equals(devolucao.getTipo()) || devolucao.getNotaReferenciadaId() == null) {
            return;
        }
        Map<Long, Long> somas = new LinkedHashMap<>();
        repo.itens(devolucao.getId()).forEach(l -> somas.merge(l.getProdutoId(), l.getQuantidade(), Long::sum));
        Map<Long, NotaLinha> primeira = new LinkedHashMap<>();
        repo.itens(devolucao.getNotaReferenciadaId()).forEach(l -> primeira.putIfAbsent(l.getProdutoId(), l));
        somas.forEach((produto, q) -> {
            NotaLinha l = primeira.get(produto);
            if (l == null) {
                return;
            }
            long emDevolucao = nz(l.getQuantidadeEmDevolucao());
            switch (evento) {
                case REENVIADA -> l.setQuantidadeEmDevolucao(emDevolucao + q);
                case REJEITADA -> l.setQuantidadeEmDevolucao(Math.max(0, emDevolucao - q));
                case AUTORIZADA -> {
                    l.setQuantidadeEmDevolucao(Math.max(0, emDevolucao - q));
                    l.setQuantidadeDevolvida(nz(l.getQuantidadeDevolvida()) + q);
                }
            }
            repo.salvarLinha(l);
        });
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
