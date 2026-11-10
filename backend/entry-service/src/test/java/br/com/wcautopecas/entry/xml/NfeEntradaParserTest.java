package br.com.wcautopecas.entry.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.wcautopecas.commons.validacao.Documentos;
import br.com.wcautopecas.entry.api.EntradaInput;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** HU-B11.2: leitura do XML do fornecedor, CFOP de entrada e proteção contra XXE. */
class NfeEntradaParserTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    static byte[] fixture() throws IOException {
        try (var in = NfeEntradaParserTest.class.getResourceAsStream("/xml/nfe-fornecedor.xml")) {
            return in.readAllBytes();
        }
    }

    @Test
    void nfeProcPreencheOFormulario() throws Exception {
        EntradaInput e = NfeEntradaParser.ler(fixture(), HOJE);
        assertThat(e.fornecedor().cnpj()).hasSize(14).satisfies(c -> assertThat(Documentos.cnpjValido(c)).isTrue());
        assertThat(e.fornecedor().razaoSocial()).isEqualTo("Distribuidora Freios Brasil LTDA");
        assertThat(e.fornecedor().ie()).isEqualTo("244123456119");
        assertThat(e.fornecedor().uf()).isEqualTo("SP");
        assertThat(e.numero()).isEqualTo("12345");
        assertThat(e.serie()).isEqualTo("1");
        assertThat(e.dataEmissao()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(e.dataEntrada()).isEqualTo(HOJE);
        assertThat(e.chaveAcesso()).hasSize(44).satisfies(c -> assertThat(Documentos.chaveAcessoValida(c)).isTrue());
        assertThat(e.naturezaOperacao()).isEqualTo("Venda de mercadoria");
        assertThat(e.itens()).hasSize(2);
        EntradaInput.Item a = e.itens().get(0);
        assertThat(a.produtoId()).isNull();
        assertThat(a.codigo()).isEqualTo("PF-001");
        assertThat(a.ncm()).isEqualTo("87083010");
        assertThat(a.cfop()).isEqualTo("1102");
        assertThat(a.quantidade()).isEqualTo(100);
        assertThat(a.valorUnitario()).isEqualByComparingTo(new BigDecimal("45.50"));
        assertThat(e.itens().get(1).cfop()).isEqualTo("2102");
    }

    @Test
    void nfeSemProtocoloUsaOIdDoInfNFe() throws Exception {
        String xml = new String(fixture(), StandardCharsets.UTF_8);
        String semProt = xml.substring(0, xml.indexOf("<protNFe")) + "</nfeProc>";
        String nfe = semProt.substring(semProt.indexOf("<NFe>"), semProt.indexOf("</NFe>") + 6)
                .replace("<NFe>", "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
        EntradaInput e = NfeEntradaParser.ler(nfe.getBytes(StandardCharsets.UTF_8), HOJE);
        assertThat(e.chaveAcesso()).isEqualTo(NfeEntradaParser.ler(fixture(), HOJE).chaveAcesso());
    }

    @Test
    void xxeERecusado() {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\"><NFe><infNFe Id=\"NFe1\"><ide><natOp>&e;</natOp>"
                + "</ide><emit/></infNFe></NFe></nfeProc>";
        assertThatThrownBy(() -> NfeEntradaParser.ler(xml.getBytes(StandardCharsets.UTF_8), HOJE))
                .isInstanceOf(NfeEntradaParser.XmlInvalidoException.class)
                .hasMessage("XML inválido ou não é NF-e");
    }

    @Test
    void naoEXmlOuNaoENfe() {
        assertThatThrownBy(() -> NfeEntradaParser.ler("isto não é xml".getBytes(StandardCharsets.UTF_8), HOJE))
                .isInstanceOf(NfeEntradaParser.XmlInvalidoException.class);
        assertThatThrownBy(() -> NfeEntradaParser.ler("<pedido><id>1</id></pedido>".getBytes(StandardCharsets.UTF_8),
                HOJE)).isInstanceOf(NfeEntradaParser.XmlInvalidoException.class);
    }

    @Test
    void cfopDeSaidaViraEntrada() {
        assertThat(NfeEntradaParser.cfopEntrada("5102")).isEqualTo("1102");
        assertThat(NfeEntradaParser.cfopEntrada("6405")).isEqualTo("2405");
        assertThat(NfeEntradaParser.cfopEntrada("1102")).isEqualTo("1102");
    }
}
