package br.com.wcautopecas.sales.venda;

import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.sales.api.VendaInput;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** HU-B06.1 RF02: subtotal = Σ quantidade × valorUnitario; total = subtotal − desconto (2 casas, HALF_EVEN). */
public final class CalculadoraVenda {

    public static final String MSG_DESCONTO = "Desconto maior que o subtotal";

    public record Valores(BigDecimal subtotal, BigDecimal desconto, BigDecimal total) {
    }

    private CalculadoraVenda() {
    }

    public static Valores calcular(List<VendaInput.Item> itens, BigDecimal desconto) {
        BigDecimal subtotal = itens.stream()
                .map(i -> dinheiro(i.valorUnitario()).multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = dinheiro(subtotal);
        BigDecimal d = dinheiro(desconto == null ? BigDecimal.ZERO : desconto);
        if (d.signum() < 0) {
            throw new ValidacaoException("Desconto inválido", List.of(new CampoErro("desconto", "Desconto inválido")));
        }
        if (d.compareTo(subtotal) > 0) {
            throw new ValidacaoException(MSG_DESCONTO, List.of(new CampoErro("desconto", MSG_DESCONTO)));
        }
        return new Valores(subtotal, d, subtotal.subtract(d));
    }

    public static BigDecimal dinheiro(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
