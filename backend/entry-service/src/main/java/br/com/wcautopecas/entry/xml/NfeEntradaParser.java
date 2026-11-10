package br.com.wcautopecas.entry.xml;

import br.com.wcautopecas.entry.api.EntradaInput;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Lê a NF-e do fornecedor ({@code nfeProc} ou {@code NFe}) e devolve um {@link EntradaInput} sem
 * {@code produtoId} (HU-B11.2 RF02). Seguro contra XXE: DOCTYPE recusado, sem entidades externas.
 * Compartilhado com o processamento assíncrono do EPB-12.
 */
public final class NfeEntradaParser {

    public static final String NS = "http://www.portalfiscal.inf.br/nfe";

    public static class XmlInvalidoException extends RuntimeException {
        public XmlInvalidoException(String message) {
            super(message);
        }
    }

    private NfeEntradaParser() {
    }

    public static EntradaInput ler(byte[] xml, LocalDate hoje) {
        Document doc = parse(xml);
        Element infNFe = primeiro(doc.getDocumentElement(), "infNFe");
        if (infNFe == null) {
            throw new XmlInvalidoException("XML inválido ou não é NF-e");
        }
        Element ide = primeiro(infNFe, "ide");
        Element emit = primeiro(infNFe, "emit");
        if (ide == null || emit == null) {
            throw new XmlInvalidoException("XML inválido ou não é NF-e");
        }
        String chave = texto(doc.getDocumentElement(), "chNFe");
        if (chave == null) {
            String id = infNFe.getAttribute("Id");
            chave = id.startsWith("NFe") ? id.substring(3) : id;
        }
        Element ender = primeiro(emit, "enderEmit");
        var fornecedor = new EntradaInput.Fornecedor(texto(emit, "CNPJ"), texto(emit, "xNome"), texto(emit, "IE"),
                ender == null ? null : texto(ender, "UF"));

        List<EntradaInput.Item> itens = new ArrayList<>();
        NodeList dets = infNFe.getElementsByTagNameNS(NS, "det");
        for (int i = 0; i < dets.getLength(); i++) {
            Element prod = primeiro((Element) dets.item(i), "prod");
            if (prod == null) {
                continue;
            }
            itens.add(new EntradaInput.Item(null, texto(prod, "cProd"), texto(prod, "xProd"), texto(prod, "NCM"),
                    cfopEntrada(texto(prod, "CFOP")), quantidade(texto(prod, "qCom")), valor(texto(prod, "vUnCom"))));
        }
        String emissao = texto(ide, "dhEmi");
        if (emissao == null) {
            emissao = texto(ide, "dEmi");
        }
        LocalDate dataEmissao = emissao == null || emissao.length() < 10 ? null : LocalDate.parse(emissao.substring(0, 10));
        return new EntradaInput(fornecedor, texto(ide, "nNF"), texto(ide, "serie"), dataEmissao, hoje, chave,
                texto(ide, "natOp"), itens);
    }

    /** CFOP de saída do fornecedor para o de entrada: 5xxx → 1xxx, 6xxx → 2xxx (HU-B11.2). */
    public static String cfopEntrada(String cfop) {
        if (cfop == null || cfop.length() != 4) {
            return cfop;
        }
        return switch (cfop.charAt(0)) {
            case '5' -> "1" + cfop.substring(1);
            case '6' -> "2" + cfop.substring(1);
            default -> cfop;
        };
    }

    static Document parse(byte[] xml) {
        try {
            var f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            var b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new XmlInvalidoException("XML inválido ou não é NF-e");
        }
    }

    private static Element primeiro(Element pai, String tag) {
        NodeList l = pai.getElementsByTagNameNS(NS, tag);
        if (l.getLength() == 0 && tag.equals(pai.getLocalName())) {
            return pai;
        }
        return l.getLength() == 0 ? null : (Element) l.item(0);
    }

    /** Texto do primeiro {@code tag} descendente direto ou não; {@code null} se faltar ou vazio. */
    private static String texto(Element pai, String tag) {
        NodeList l = pai.getElementsByTagNameNS(NS, tag);
        if (l.getLength() == 0) {
            return null;
        }
        Node n = l.item(0);
        String t = n.getTextContent();
        return t == null || t.isBlank() ? null : t.trim();
    }

    private static Long quantidade(String q) {
        if (q == null) {
            return null;
        }
        BigDecimal v = new BigDecimal(q);
        return v.stripTrailingZeros().scale() <= 0 ? v.longValueExact() : (long) Math.ceil(v.doubleValue());
    }

    private static BigDecimal valor(String v) {
        return v == null ? null : new BigDecimal(v).setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}
