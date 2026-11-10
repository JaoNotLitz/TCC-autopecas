package br.com.wcautopecas.commons.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentosTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735"})
    void cpfValido(String cpf) {
        assertThat(Documentos.cpfValido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11111111111", "52998224724", "529.982.247-25", "123", ""})
    void cpfInvalido(String cpf) {
        assertThat(Documentos.cpfValido(cpf)).isFalse();
    }

    @Test
    void cpfComMascaraValidoDepoisDeLimpar() {
        assertThat(Documentos.cpfValido(Documentos.digitos("529.982.247-25"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000181", "45997418000153"})
    void cnpjValido(String cnpj) {
        assertThat(Documentos.cnpjValido(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000000", "11222333000182", "1122233300018"})
    void cnpjInvalido(String cnpj) {
        assertThat(Documentos.cnpjValido(cnpj)).isFalse();
    }

    @Test
    void ufNcm() {
        assertThat(Documentos.ufValida("SP")).isTrue();
        assertThat(Documentos.ufValida("XX")).isFalse();
        assertThat(Documentos.ncmValido("87083010")).isTrue();
        assertThat(Documentos.ncmValido("8708301")).isFalse();
    }

    @Test
    void chaveAcessoComDv() {
        String base = "3526091234567800019065001000126000123456789";
        String chave = base + Documentos.dvChave(base);
        assertThat(Documentos.chaveAcessoValida(chave)).isTrue();
        int errado = (Documentos.dvChave(base) + 1) % 10;
        assertThat(Documentos.chaveAcessoValida(base + errado)).isFalse();
    }
}
