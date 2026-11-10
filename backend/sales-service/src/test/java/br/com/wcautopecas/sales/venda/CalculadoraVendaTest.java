package br.com.wcautopecas.sales.venda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.sales.api.VendaInput;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraVendaTest {

    static VendaInput.Item item(long qtd, String valor) {
        return new VendaInput.Item(1L, qtd, new BigDecimal(valor));
    }

    @Test
    void nfceAVista() {
        var v = CalculadoraVenda.calcular(List.of(item(2, "89.90")), BigDecimal.ZERO);
        assertThat(v.subtotal()).isEqualByComparingTo("179.80");
        assertThat(v.total()).isEqualByComparingTo("179.80");
        assertThat(v.total().scale()).isEqualTo(2);
    }

    @Test
    void arredondaMeioParaPar() {
        // 0,125 → 0,12 e 0,135 → 0,14 (HALF_EVEN)
        var v = CalculadoraVenda.calcular(List.of(item(1, "0.125"), item(1, "0.135")), new BigDecimal("0.005"));
        assertThat(v.subtotal()).isEqualByComparingTo("0.26");
        assertThat(v.desconto()).isEqualByComparingTo("0.00");
        assertThat(v.total()).isEqualByComparingTo("0.26");
    }

    @Test
    void descontoNuloViraZero() {
        var v = CalculadoraVenda.calcular(List.of(item(3, "10")), null);
        assertThat(v.desconto()).isEqualByComparingTo("0");
        assertThat(v.total()).isEqualByComparingTo("30.00");
    }

    @Test
    void descontoIgualAoSubtotalZeraOTotal() {
        var v = CalculadoraVenda.calcular(List.of(item(1, "50")), new BigDecimal("50"));
        assertThat(v.total()).isEqualByComparingTo("0");
    }

    @Test
    void descontoMaiorQueOSubtotal() {
        assertThatThrownBy(() -> CalculadoraVenda.calcular(List.of(item(2, "89.90")), new BigDecimal("200")))
                .isInstanceOf(ValidacaoException.class)
                .hasMessage("Desconto maior que o subtotal")
                .satisfies(e -> assertThat(((ValidacaoException) e).getCampos())
                        .extracting(c -> c.campo()).containsExactly("desconto"));
    }
}
