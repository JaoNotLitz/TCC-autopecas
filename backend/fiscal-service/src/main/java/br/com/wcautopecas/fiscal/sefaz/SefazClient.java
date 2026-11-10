package br.com.wcautopecas.fiscal.sefaz;

import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.xml.CertificadoA1;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Cliente SOAP 1.2 dos webservices da NF-e 4.00, com o {@link HttpClient} do JDK. Em endpoint
 * HTTPS usa TLS mútuo com o certificado A1; no simulador (HTTP) não.
 */
@Component
public class SefazClient {

    static final String SOAP = "http://www.w3.org/2003/05/soap-envelope";
    static final String WSDL = "http://www.portalfiscal.inf.br/nfe/wsdl/";
    static final String NFE = "http://www.portalfiscal.inf.br/nfe";

    private final FiscalProperties props;
    private final CertificadoA1 certificado;

    public SefazClient(FiscalProperties props, CertificadoA1 certificado) {
        this.props = props;
        this.certificado = certificado;
    }

    /** Erro de comunicação: timeout, conexão recusada ou HTTP diferente de 200. */
    public static class ComunicacaoException extends RuntimeException {
        public ComunicacaoException(String message, Throwable causa) {
            super(message, causa);
        }
    }

    /** Resposta já sem o envelope: o elemento de retorno ({@code retEnviNFe}, {@code retEnvEvento}...). */
    public record Resposta(Document doc, String corpo) {

        public String texto(String tag) {
            NodeList l = doc.getElementsByTagNameNS(NFE, tag);
            return l.getLength() == 0 ? null : l.item(0).getTextContent();
        }

        /** Texto de {@code tag} dentro do primeiro {@code pai}. */
        public String texto(String pai, String tag) {
            NodeList p = doc.getElementsByTagNameNS(NFE, pai);
            if (p.getLength() == 0) {
                return null;
            }
            NodeList l = ((Element) p.item(0)).getElementsByTagNameNS(NFE, tag);
            return l.getLength() == 0 ? null : l.item(0).getTextContent();
        }

        /** Trecho cru de um elemento (ex.: {@code protNFe}), para anexar ao XML sem reformatar. */
        public String trecho(String tag) {
            int i = corpo.indexOf("<" + tag);
            int f = corpo.indexOf("</" + tag + ">", i);
            return i < 0 || f < 0 ? null : corpo.substring(i, f + tag.length() + 3);
        }
    }

    public Resposta autorizar(String nfeXml, long idLote) {
        String dados = "<enviNFe xmlns=\"" + NFE + "\" versao=\"4.00\"><idLote>" + idLote
                + "</idLote><indSinc>1</indSinc>" + semDeclaracao(nfeXml) + "</enviNFe>";
        return chamar("NFeAutorizacao4", "nfeAutorizacaoLote", dados);
    }

    public Resposta consultarRecibo(String nRec) {
        String dados = "<consReciNFe xmlns=\"" + NFE + "\" versao=\"4.00\"><tpAmb>" + tpAmb() + "</tpAmb><nRec>"
                + nRec + "</nRec></consReciNFe>";
        return chamar("NFeRetAutorizacao4", "nfeRetAutorizacaoLote", dados);
    }

    public Resposta enviarEvento(String eventoXml, long idLote) {
        String dados = "<envEvento xmlns=\"" + NFE + "\" versao=\"1.00\"><idLote>" + idLote + "</idLote>"
                + semDeclaracao(eventoXml) + "</envEvento>";
        return chamar("NFeRecepcaoEvento4", "nfeRecepcaoEvento", dados);
    }

    public Resposta inutilizar(String inutXml) {
        return chamar("NFeInutilizacao4", "nfeInutilizacaoNF", semDeclaracao(inutXml));
    }

    public Resposta status(String cUF) {
        String dados = "<consStatServ xmlns=\"" + NFE + "\" versao=\"4.00\"><tpAmb>" + tpAmb() + "</tpAmb><cUF>"
                + cUF + "</cUF><xServ>STATUS</xServ></consStatServ>";
        return chamar("NFeStatusServico4", "nfeStatusServicoNF", dados);
    }

    String tpAmb() {
        return "producao".equalsIgnoreCase(props.getAmbiente()) ? "1" : "2";
    }

    private Resposta chamar(String servico, String acao, String dados) {
        String url = props.getSefaz().endpoint(servico);
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap12:Envelope xmlns:soap12=\"" + SOAP
                + "\"><soap12:Body><nfeDadosMsg xmlns=\"" + WSDL + servico + "\">" + dados
                + "</nfeDadosMsg></soap12:Body></soap12:Envelope>";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(props.getSefaz().getTimeout())
                .header("Content-Type", "application/soap+xml; charset=utf-8; action=\"" + WSDL + servico + "/" + acao + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp;
        try {
            resp = cliente(url).send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ComunicacaoException(servico + ": " + e.getClass().getSimpleName() + " " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComunicacaoException(servico + ": interrompido", e);
        }
        if (resp.statusCode() != 200) {
            throw new ComunicacaoException(servico + ": HTTP " + resp.statusCode(), null);
        }
        try {
            var f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(resp.body().getBytes(StandardCharsets.UTF_8)));
            return new Resposta(doc, resp.body());
        } catch (Exception e) {
            throw new ComunicacaoException(servico + ": resposta inválida", e);
        }
    }

    private HttpClient cliente(String url) {
        var b = HttpClient.newBuilder().connectTimeout(props.getSefaz().getTimeout());
        if (url.startsWith("https")) {
            b.sslContext(tlsMutuo());
        }
        return b.build();
    }

    private SSLContext tlsMutuo() {
        try {
            CertificadoA1.Credencial c = certificado.get();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            char[] senha = "tls".toCharArray();
            ks.setKeyEntry("a1", c.chave(), senha, new Certificate[] {c.certificado()});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, senha);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("não foi possível montar o TLS com o certificado A1", e);
        }
    }

    static String semDeclaracao(String xml) {
        String t = xml.strip();
        return t.startsWith("<?xml") ? t.substring(t.indexOf("?>") + 2) : t;
    }
}
