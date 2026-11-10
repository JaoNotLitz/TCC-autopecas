package br.com.wcautopecas.fiscal.xml;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.wcautopecas.fiscal.FiscalProperties;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** HU-B10.1 e B10.2: XML do cancelamento e da inutilização, assinados. */
class EventoXmlTest {

    static CertificadoA1.Credencial cred;
    final FiscalProperties props = new FiscalProperties();
    final EventoXmlBuilder builder = new EventoXmlBuilder(props);
    final AssinadorXml assinador = new AssinadorXml();
    final ZonedDateTime quando = ZonedDateTime.parse("2026-11-10T15:30:00-03:00");
    static final String CHAVE = ChaveAcesso.gerar("35", "2611", "12345678000195", "65", "1", 126, "1").valor();

    @BeforeAll
    static void certificado() {
        cred = CertificadoA1.carregar(CertificadosTeste.secret(false));
    }

    @Test
    void idsComZerosAEsquerda() {
        assertThat(EventoXmlBuilder.idCancelamento(CHAVE)).isEqualTo("ID110111" + CHAVE + "01").hasSize(54);
        assertThat(EventoXmlBuilder.idInutilizacao("35", 2026, "12345678000195", "55", "1", 100, 105))
                .isEqualTo("ID35261234567800019555001000000100000000105")
                .hasSize(43);
    }

    @Test
    void cancelamentoAssinado() {
        Document doc = builder.cancelamento(CHAVE, "135260000000001", "Cliente desistiu da compra após emissão",
                quando);
        assinador.assinar(doc, "infEvento", doc.getDocumentElement(), cred);
        String xml = AssinadorXml.texto(doc);
        assertThat(xml)
                .contains("<evento versao=\"1.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">")
                .contains("<infEvento Id=\"ID110111" + CHAVE + "01\"><cOrgao>35</cOrgao><tpAmb>2</tpAmb>"
                        + "<CNPJ>12345678000195</CNPJ><chNFe>" + CHAVE + "</chNFe>"
                        + "<dhEvento>2026-11-10T15:30:00-03:00</dhEvento><tpEvento>110111</tpEvento>"
                        + "<nSeqEvento>1</nSeqEvento><verEvento>1.00</verEvento>")
                .contains("<detEvento versao=\"1.00\"><descEvento>Cancelamento</descEvento>"
                        + "<nProt>135260000000001</nProt><xJust>CLIENTE DESISTIU DA COMPRA APOS EMISSAO</xJust>")
                .contains("<Reference URI=\"#ID110111" + CHAVE + "01\">");
        assertThat(assinador.valida(doc)).isTrue();
    }

    @Test
    void inutilizacaoAssinada() {
        Document doc = builder.inutilizacao("55", "1", 100, 105, "Numeração pulada\npor erro no sistema", quando);
        assinador.assinar(doc, "infInut", doc.getDocumentElement(), cred);
        String xml = AssinadorXml.texto(doc);
        assertThat(xml)
                .contains("<inutNFe versao=\"4.00\" xmlns=\"http://www.portalfiscal.inf.br/nfe\">")
                .contains("<infInut Id=\"ID35261234567800019555001000000100000000105\"><tpAmb>2</tpAmb>"
                        + "<xServ>INUTILIZAR</xServ><cUF>35</cUF><ano>26</ano><CNPJ>12345678000195</CNPJ>"
                        + "<mod>55</mod><serie>1</serie><nNFIni>100</nNFIni><nNFFin>105</nNFFin>"
                        + "<xJust>NUMERACAO PULADA POR ERRO NO SISTEMA</xJust></infInut>");
        assertThat(assinador.valida(doc)).isTrue();
    }
}
