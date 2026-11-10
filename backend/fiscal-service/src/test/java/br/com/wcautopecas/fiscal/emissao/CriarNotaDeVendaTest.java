package br.com.wcautopecas.fiscal.emissao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CriarNotaDeVendaTest {

    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
        "5102, false, 5102",
        "5102, true, 6102",
        "5405, true, 6405",
        "-, false, 5102",
        "-, true, 6102",
        "1202, true, 1202"
    })
    void cfopDaVenda(String cfop, boolean interestadual, String esperado) {
        assertThat(CriarNotaDeVenda.cfopVenda(cfop, interestadual)).isEqualTo(esperado);
    }
}
