package br.com.wcautopecas.fiscal.xml;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** HU-B08.1 a B08.4 sem Spring: monta, assina e valida contra o XSD oficial. */
class NfeXmlTest {

    static CertificadoA1.Credencial cred;
    final FiscalProperties props = new FiscalProperties();
    final NfeXmlBuilder builder = new NfeXmlBuilder(props);
    final AssinadorXml assinador = new AssinadorXml();
    static final ValidadorXsd XSD = new ValidadorXsd();
    final ZonedDateTime emissao = ZonedDateTime.parse("2026-11-10T15:30:00-03:00");

    @BeforeAll
    static void certificado() {
        cred = CertificadoA1.carregar(CertificadosTeste.secret(false));
    }

    static NotaItem nota(String tipo, String doc, String cidade, String uf) {
        NotaItem n = new NotaItem();
        n.setId(1L);
        n.setTipo(tipo);
        n.setModelo("NFCE".equals(tipo) ? "65" : "55");
        n.setSerie("1");
        n.setNumero("000126");
        n.setVendaId(126L);
        n.setClienteDocumento(doc);
        n.setClienteNome(doc == null ? null : "Auto Peças Silva LTDA");
        n.setClienteCidade(cidade);
        n.setClienteUf(uf);
        n.setClienteEmail(doc == null ? null : "contato@silva.com.br");
        n.setMetodoPagamento("credito");
        n.setBandeira("visa");
        n.setModalidade("fisica");
        n.setValorDesconto(BigDecimal.ZERO);
        n.setEmitidaEm(Instant.now().toString());
        return n;
    }

    static NotaLinha linha(long item, String valor, long qtd, String ncm, String cfop) {
        NotaLinha l = new NotaLinha();
        l.setNotaId(1L);
        l.setItemId(item);
        l.setProdutoId(item);
        l.setCodigo("PF-00" + item);
        l.setDescricao("Pastilha de Freio Dianteira");
        l.setNcm(ncm);
        l.setCfop(cfop);
        l.setOrigem("0");
        l.setCstIcms("00");
        l.setAliquotaIcms(new BigDecimal("18"));
        l.setQuantidade(qtd);
        l.setValorUnitario(new BigDecimal(valor));
        return l;
    }

    String gerar(NotaItem n, List<NotaLinha> linhas) {
        ChaveAcesso chave = ChaveAcesso.gerar("35", "2611", "12345678000195", n.getModelo(), n.getSerie(), 126, "1");
        Document doc = builder.montar(n, linhas, chave, emissao);
        assinador.assinar(doc, "infNFe", doc.getDocumentElement(), cred);
        return AssinadorXml.texto(doc);
    }

    static Document ler(String xml) throws Exception {
        var f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    static String x(Document d, String expr) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expr, d);
    }

    @Test
    void nfceConsumidorFinal() throws Exception {
        String xml = gerar(nota("NFCE", null, null, null),
                List.of(linha(1, "89.90", 2, "87083010", "5102"), linha(2, "10.00", 1, "87083010", "5102")));
        assertThat(XSD.validar(xml)).isEmpty();
        Document d = ler(xml);
        assertThat(assinador.valida(d)).isTrue();
        assertThat(x(d, "//*[local-name()='ide']/*[local-name()='mod']")).isEqualTo("65");
        assertThat(x(d, "//*[local-name()='ide']/*[local-name()='tpImp']")).isEqualTo("4");
        assertThat(x(d, "count(//*[local-name()='dest'])")).isEqualTo("0");
        assertThat(x(d, "//*[local-name()='detPag']/*[local-name()='tPag']")).isEqualTo("03");
        assertThat(x(d, "//*[local-name()='card']/*[local-name()='tBand']")).isEqualTo("01");
        assertThat(x(d, "//*[local-name()='qrCode']")).contains("?p=").contains("|2|2|1|");
        assertThat(x(d, "//*[local-name()='vNF']")).isEqualTo("189.80");
    }

    @Test
    void nfeParaPj() throws Exception {
        String xml = gerar(nota("NFE", "11222333000181", "São Paulo", "SP"),
                List.of(linha(1, "89.90", 2, "87083010", "5102")));
        assertThat(XSD.validar(xml)).isEmpty();
        Document d = ler(xml);
        assertThat(assinador.valida(d)).isTrue();
        assertThat(x(d, "//*[local-name()='dest']/*[local-name()='CNPJ']")).isEqualTo("11222333000181");
        assertThat(x(d, "//*[local-name()='dest']/*[local-name()='indIEDest']")).isEqualTo("9");
        assertThat(x(d, "//*[local-name()='enderDest']/*[local-name()='cMun']")).isEqualTo("3550308");
        assertThat(x(d, "//*[local-name()='dest']/*[local-name()='xNome']")).isEqualTo(NfeXmlBuilder.HOMOLOGACAO_DEST);
        assertThat(x(d, "//*[local-name()='ICMS00']/*[local-name()='vICMS']")).isEqualTo("32.36");
        assertThat(x(d, "//*[local-name()='enderDest']/*[local-name()='xLgr']")).isEqualTo(NfeXmlBuilder.NAO_INFORMADO);
        assertThat(x(d, "count(//*[local-name()='enderDest']/*[local-name()='CEP'])")).isEqualTo("0");
    }

    @Test
    void nfeComEnderecoDoCadastro() throws Exception {
        NotaItem n = nota("NFE", "11222333000181", "São Paulo", "SP");
        n.setClienteCep("01310100");
        n.setClienteLogradouro("Avenida Paulista");
        n.setClienteNumero("1000");
        n.setClienteComplemento("Sala 12");
        n.setClienteBairro("Bela Vista");
        String xml = gerar(n, List.of(linha(1, "89.90", 2, "87083010", "5102")));
        assertThat(XSD.validar(xml)).isEmpty();
        Document d = ler(xml);
        String e = "//*[local-name()='enderDest']/*[local-name()='%s']";
        assertThat(x(d, e.formatted("xLgr"))).isEqualTo("Avenida Paulista");
        assertThat(x(d, e.formatted("nro"))).isEqualTo("1000");
        assertThat(x(d, e.formatted("xCpl"))).isEqualTo("Sala 12");
        assertThat(x(d, e.formatted("xBairro"))).isEqualTo("Bela Vista");
        assertThat(x(d, e.formatted("CEP"))).isEqualTo("01310100");
    }

    @Test
    void devolucao() throws Exception {
        NotaItem n = nota("DEVOLUCAO", "52998224725", "Campinas", "SP");
        n.setChaveReferenciada("35261112345678000195550010000001231000000010");
        n.setMotivo("Produto com defeito");
        String xml = gerar(n, List.of(linha(1, "89.90", 1, "87083010", "1202")));
        assertThat(XSD.validar(xml)).isEmpty();
        Document d = ler(xml);
        assertThat(x(d, "//*[local-name()='finNFe']")).isEqualTo("4");
        assertThat(x(d, "//*[local-name()='tpNF']")).isEqualTo("0");
        assertThat(x(d, "//*[local-name()='refNFe']")).isEqualTo(n.getChaveReferenciada());
        assertThat(x(d, "//*[local-name()='enderDest']/*[local-name()='cMun']")).isEqualTo("3509502");
        assertThat(x(d, "//*[local-name()='detPag']/*[local-name()='tPag']")).isEqualTo("90");
    }

    @Test
    void rateioDoDesconto() throws Exception {
        NotaItem n = nota("NFCE", null, null, null);
        n.setValorDesconto(new BigDecimal("1.00"));
        String xml = gerar(n, List.of(linha(1, "10", 1, "87083010", "5102"), linha(2, "10", 1, "87083010", "5102"),
                linha(3, "10", 1, "87083010", "5102")));
        assertThat(XSD.validar(xml)).isEmpty();
        Document d = ler(xml);
        assertThat(x(d, "sum(//*[local-name()='prod']/*[local-name()='vDesc'])")).isEqualTo("1");
        assertThat(x(d, "//*[local-name()='ICMSTot']/*[local-name()='vNF']")).isEqualTo("29.00");
        assertThat(x(d, "(//*[local-name()='prod']/*[local-name()='vDesc'])[3]")).isEqualTo("0.34");
    }

    @Test
    void xmlAlteradoDepoisDeAssinadoInvalidaAssinatura() throws Exception {
        String xml = gerar(nota("NFCE", null, null, null), List.of(linha(1, "89.90", 1, "87083010", "5102")));
        Document d = ler(xml.replace("<vProd>89.90</vProd>", "<vProd>8.99</vProd>"));
        assertThat(assinador.valida(d)).isFalse();
    }

    @Test
    void ncmComSeteDigitosFalhaNoXsd() {
        String xml = gerar(nota("NFCE", null, null, null), List.of(linha(1, "89.90", 1, "8708301", "5102")));
        assertThat(XSD.validar(xml)).hasValueSatisfying(m -> assertThat(m).contains("8708301"));
    }

    @Test
    void certificadoVencidoAbreMasEstaForaDaValidade() {
        var vencido = CertificadoA1.carregar(CertificadosTeste.secret(true));
        assertThat(vencido.validoAte()).isBefore(Instant.now());
    }
}
