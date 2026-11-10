package br.com.wcautopecas.document.danfe;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** O que o DANFE mostra, lido do {@code nfeProc} gravado pelo fiscal-service. */
public record DanfeDados(String chave, String modelo, String serie, String numero, String dhEmi, String natOp,
        String tpAmb, Pessoa emitente, Pessoa destinatario, List<Item> itens, BigDecimal vProd, BigDecimal vDesc,
        BigDecimal vNF, BigDecimal vBC, BigDecimal vICMS, List<Pagamento> pagamentos, BigDecimal troco,
        String protocolo, String dhRecbto, String qrCode, String urlChave, String infCpl) {

    static final String NS = "http://www.portalfiscal.inf.br/nfe";
    static final Map<String, String> FORMAS = Map.of("01", "Dinheiro", "03", "Cartão de crédito", "04",
            "Cartão de débito", "15", "Boleto", "17", "PIX", "90", "Sem pagamento", "99", "Outros");

    public record Pessoa(String documento, String nome, String ie, String logradouro, String bairro,
            String municipio, String uf, String cep, String fone, String email) {
    }

    public record Item(String codigo, String descricao, String ncm, String cfop, String unidade,
            BigDecimal quantidade, BigDecimal valorUnitario, BigDecimal valorTotal, BigDecimal desconto) {
    }

    public record Pagamento(String forma, BigDecimal valor) {
    }

    public boolean nfce() {
        return "65".equals(modelo);
    }

    public boolean homologacao() {
        return !"1".equals(tpAmb);
    }

    public static DanfeDados ler(byte[] xml) {
        Document doc = parse(xml);
        Element raiz = doc.getDocumentElement();
        Element inf = el(raiz, "infNFe");
        if (inf == null) {
            throw new IllegalArgumentException("XML sem infNFe");
        }
        Element ide = el(inf, "ide");
        Element emit = el(inf, "emit");
        Element dest = el(inf, "dest");
        Element tot = el(inf, "ICMSTot");
        String id = inf.getAttribute("Id");
        String chave = t(raiz, "chNFe") != null ? t(raiz, "chNFe") : id.replaceFirst("^NFe", "");

        List<Item> itens = new ArrayList<>();
        NodeList dets = inf.getElementsByTagNameNS(NS, "det");
        for (int i = 0; i < dets.getLength(); i++) {
            Element p = el((Element) dets.item(i), "prod");
            itens.add(new Item(t(p, "cProd"), t(p, "xProd"), t(p, "NCM"), t(p, "CFOP"), t(p, "uCom"),
                    n(t(p, "qCom")), n(t(p, "vUnCom")), n(t(p, "vProd")), n(t(p, "vDesc"))));
        }
        List<Pagamento> pags = new ArrayList<>();
        NodeList dps = inf.getElementsByTagNameNS(NS, "detPag");
        for (int i = 0; i < dps.getLength(); i++) {
            Element d = (Element) dps.item(i);
            String tp = t(d, "tPag");
            pags.add(new Pagamento(FORMAS.getOrDefault(tp, "Outros"), n(t(d, "vPag"))));
        }
        Element pag = el(inf, "pag");
        Element supl = el(raiz, "infNFeSupl");
        Element infAdic = el(inf, "infAdic");
        Element prot = el(raiz, "infProt");
        return new DanfeDados(chave, t(ide, "mod"), t(ide, "serie"), t(ide, "nNF"), t(ide, "dhEmi"), t(ide, "natOp"),
                t(ide, "tpAmb"), pessoa(emit, "enderEmit"), dest == null ? null : pessoa(dest, "enderDest"), itens,
                n(t(tot, "vProd")), n(t(tot, "vDesc")), n(t(tot, "vNF")), n(t(tot, "vBC")), n(t(tot, "vICMS")), pags,
                pag == null ? null : n(t(pag, "vTroco")), prot == null ? null : t(prot, "nProt"),
                prot == null ? null : t(prot, "dhRecbto"), supl == null ? null : t(supl, "qrCode"),
                supl == null ? null : t(supl, "urlChave"), infAdic == null ? null : t(infAdic, "infCpl"));
    }

    private static Pessoa pessoa(Element p, String tagEndereco) {
        Element e = el(p, tagEndereco);
        String doc = t(p, "CNPJ") != null ? t(p, "CNPJ") : t(p, "CPF");
        String logradouro = e == null ? null : junta(t(e, "xLgr"), t(e, "nro"));
        return new Pessoa(doc, t(p, "xNome"), t(p, "IE"), logradouro, e == null ? null : t(e, "xBairro"),
                e == null ? null : t(e, "xMun"), e == null ? null : t(e, "UF"), e == null ? null : t(e, "CEP"),
                e == null ? null : t(e, "fone"), t(p, "email"));
    }

    private static String junta(String a, String b) {
        if (a == null) {
            return b;
        }
        return b == null ? a : a + ", " + b;
    }

    static Document parse(byte[] xml) {
        try {
            var f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new IllegalArgumentException("XML da nota ilegível", e);
        }
    }

    private static Element el(Element pai, String tag) {
        if (pai == null) {
            return null;
        }
        NodeList l = pai.getElementsByTagNameNS(NS, tag);
        return l.getLength() == 0 ? null : (Element) l.item(0);
    }

    private static String t(Element pai, String tag) {
        Element e = el(pai, tag);
        if (e == null) {
            return null;
        }
        String s = e.getTextContent().trim();
        return s.isEmpty() ? null : s;
    }

    private static BigDecimal n(String s) {
        return s == null ? null : new BigDecimal(s);
    }
}
