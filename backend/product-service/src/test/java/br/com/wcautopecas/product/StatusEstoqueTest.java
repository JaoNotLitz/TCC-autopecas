package br.com.wcautopecas.product;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.wcautopecas.product.estoque.StatusEstoque;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatusEstoqueTest {

    @ParameterizedTest
    @CsvSource({
        "15,15,ok", "20,15,ok", "14,15,atencao", "11,15,atencao", "10,15,baixo", "7,15,baixo",
        "6,15,critico", "0,15,critico", "4,10,critico", "5,10,baixo", "7,10,atencao", "0,0,ok", "3,0,ok"})
    void regraDaSecao43(long atual, long minimo, String esperado) {
        assertThat(StatusEstoque.calcular(atual, minimo)).isEqualTo(esperado);
    }
}
