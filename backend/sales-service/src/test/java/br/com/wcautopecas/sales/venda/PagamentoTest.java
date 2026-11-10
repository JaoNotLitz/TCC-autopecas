package br.com.wcautopecas.sales.venda;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.wcautopecas.commons.erro.ValidacaoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PagamentoTest {

    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
        "credito, visa, -",
        "debito, elo, -",
        "dinheiro, -, cofre",
        "dinheiro, -, banco",
        "pix, -, -",
        "boleto, -, -",
        "pix, '', ''"
    })
    void combinacoesValidas(String metodo, String bandeira, String destino) {
        assertThatCode(() -> Pagamento.validar(metodo, bandeira, destino)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
        "credito, -, -, bandeira",
        "debito, amex, -, bandeira",
        "pix, visa, -, bandeira",
        "dinheiro, -, -, destinoDinheiro",
        "dinheiro, -, gaveta, destinoDinheiro",
        "credito, visa, cofre, destinoDinheiro",
        "boleto, -, banco, destinoDinheiro"
    })
    void combinacoesInvalidas(String metodo, String bandeira, String destino, String campo) {
        assertThatThrownBy(() -> Pagamento.validar(metodo, bandeira, destino))
                .isInstanceOfSatisfying(ValidacaoException.class, e -> org.assertj.core.api.Assertions
                        .assertThat(e.getCampos()).extracting(c -> c.campo()).contains(campo));
    }
}
