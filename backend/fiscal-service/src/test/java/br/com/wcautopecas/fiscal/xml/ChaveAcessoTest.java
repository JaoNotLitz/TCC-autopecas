package br.com.wcautopecas.fiscal.xml;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.wcautopecas.commons.validacao.Documentos;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ChaveAcessoTest {

    @Test
    void dvDoExemploDoManual() {
        // exemplo do Manual de Orientação do Contribuinte (cálculo do dígito verificador)
        assertThat(ChaveAcesso.dv("5206043300991100250655012000000780026730161")).isEqualTo(5);
    }

    @RepeatedTest(20)
    void chaveGeradaTemFormatoEDvValidos() {
        ChaveAcesso c = ChaveAcesso.gerar("35", "2611", "12345678000195", "65", "1", 126, "1");
        assertThat(c.valor()).hasSize(44).matches("\\d{44}");
        assertThat(c.valor()).startsWith("35261112345678000195650010000001261");
        assertThat(c.cNF()).isNotEqualTo("00000126");
        assertThat(Documentos.chaveAcessoValida(c.valor())).isTrue();
    }
}
