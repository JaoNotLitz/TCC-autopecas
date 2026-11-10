package br.com.wcautopecas.sales.venda;

import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Regras de pagamento da seção 5.4: {@code bandeira} só com crédito/débito (e obrigatória nesses);
 * {@code destinoDinheiro} só com dinheiro (e obrigatório nele).
 */
public final class Pagamento {

    static final Set<String> CARTAO = Set.of("credito", "debito");
    static final Set<String> BANDEIRAS = Set.of("mastercard", "visa", "elo", "outros");
    static final Set<String> DESTINOS = Set.of("banco", "cofre");

    private Pagamento() {
    }

    public static void validar(String metodo, String bandeira, String destino) {
        List<CampoErro> erros = new ArrayList<>();
        boolean temBandeira = bandeira != null && !bandeira.isBlank();
        boolean temDestino = destino != null && !destino.isBlank();
        if (CARTAO.contains(metodo)) {
            if (!temBandeira) {
                erros.add(new CampoErro("bandeira", "Informe a bandeira do cartão"));
            } else if (!BANDEIRAS.contains(bandeira)) {
                erros.add(new CampoErro("bandeira", "Bandeira inválida"));
            }
        } else if (temBandeira) {
            erros.add(new CampoErro("bandeira", "Bandeira só vale para crédito ou débito"));
        }
        if ("dinheiro".equals(metodo)) {
            if (!temDestino) {
                erros.add(new CampoErro("destinoDinheiro", "Informe o destino do dinheiro"));
            } else if (!DESTINOS.contains(destino)) {
                erros.add(new CampoErro("destinoDinheiro", "Destino do dinheiro inválido"));
            }
        } else if (temDestino) {
            erros.add(new CampoErro("destinoDinheiro", "Destino do dinheiro só vale para pagamento em dinheiro"));
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros.get(0).message(), erros);
        }
    }
}
