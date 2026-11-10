package br.com.wcautopecas.fiscal.xml;

import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.FiscalProperties.Emitente;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Monta o XML da NF-e/NFC-e no layout 4.00 (HU-B08.1) com DOM, na ordem dos elementos do XSD
 * {@code leiauteNFe_v4.00.xsd}. A conferência com o schema oficial fica no {@link ValidadorXsd}.
 */
@Component
public class NfeXmlBuilder {

    public static final String NS = "http://www.portalfiscal.inf.br/nfe";
    static final String HOMOLOGACAO_DEST = "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
    static final String HOMOLOGACAO_ITEM = "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
    static final String NAO_INFORMADO = "NAO INFORMADO";
    static final DateTimeFormatter DH = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** Método de pagamento do front → {@code tPag}. */
    static final Map<String, String> T_PAG = Map.of("dinheiro", "01", "credito", "03", "debito", "04",
            "boleto", "15", "pix", "17");
    /** Bandeira do front → {@code tBand}. */
    static final Map<String, String> T_BAND = Map.of("visa", "01", "mastercard", "02", "elo", "06", "outros", "99");

    private final FiscalProperties props;

    public NfeXmlBuilder(FiscalProperties props) {
        this.props = props;
    }

    /** Valores de um item depois do rateio do desconto. */
    record ItemCalculado(NotaLinha linha, BigDecimal vProd, BigDecimal vDesc, BigDecimal vBC, BigDecimal pICMS,
            BigDecimal vICMS) {
    }

    public Document montar(NotaItem n, List<NotaLinha> linhas, ChaveAcesso chave, ZonedDateTime emissao) {
        Emitente emit = props.getEmitente();
        boolean nfce = "65".equals(chave.modelo());
        boolean devolucao = "DEVOLUCAO".equals(n.getTipo());
        boolean homologacao = !"producao".equalsIgnoreCase(props.getAmbiente());
        String tpAmb = homologacao ? "2" : "1";
        String documento = n.getClienteDocumento() == null ? "" : n.getClienteDocumento().replaceAll("\\D", "");
        boolean temDest = !documento.isEmpty();
        boolean interestadual = temDest && n.getClienteUf() != null && !nfce
                && !n.getClienteUf().equalsIgnoreCase(emit.getUf());

        List<ItemCalculado> itens = calcular(linhas, n.getValorDesconto());

        Document doc = novoDocumento();
        Element nfe = doc.createElementNS(NS, "NFe");
        doc.appendChild(nfe);
        Element inf = el(nfe, "infNFe");
        inf.setAttribute("versao", "4.00");
        inf.setAttribute("Id", "NFe" + chave.valor());
        inf.setIdAttribute("Id", true);

        Element ide = el(inf, "ide");
        el(ide, "cUF", chave.cUF());
        el(ide, "cNF", chave.cNF());
        el(ide, "natOp", devolucao ? "DEVOLUCAO DE MERCADORIA" : "VENDA DE MERCADORIA");
        el(ide, "mod", chave.modelo());
        el(ide, "serie", String.valueOf(Integer.parseInt(n.getSerie())));
        el(ide, "nNF", String.valueOf(Long.parseLong(n.getNumero())));
        el(ide, "dhEmi", DH.format(emissao));
        el(ide, "tpNF", devolucao ? "0" : "1");
        el(ide, "idDest", interestadual ? "2" : "1");
        el(ide, "cMunFG", emit.getCodigoMunicipio());
        el(ide, "tpImp", nfce ? "4" : "1");
        el(ide, "tpEmis", "1");
        el(ide, "cDV", chave.cDV());
        el(ide, "tpAmb", tpAmb);
        el(ide, "finNFe", devolucao ? "4" : "1");
        el(ide, "indFinal", nfce || documento.length() == 11 ? "1" : "0");
        String indPres = devolucao ? "0" : "virtual".equals(n.getModalidade()) ? "2" : "1";
        el(ide, "indPres", indPres);
        if ("2".equals(indPres)) {
            el(ide, "indIntermed", "0");
        }
        el(ide, "procEmi", "0");
        el(ide, "verProc", "WC-AUTOPECAS-1.0");
        if (devolucao && n.getChaveReferenciada() != null) {
            el(el(ide, "NFref"), "refNFe", n.getChaveReferenciada());
        }

        Element e = el(inf, "emit");
        el(e, "CNPJ", emit.getCnpj());
        el(e, "xNome", emit.getRazaoSocial());
        el(e, "xFant", emit.getNomeFantasia());
        Element ee = el(e, "enderEmit");
        el(ee, "xLgr", emit.getLogradouro());
        el(ee, "nro", emit.getNumero());
        el(ee, "xBairro", emit.getBairro());
        el(ee, "cMun", emit.getCodigoMunicipio());
        el(ee, "xMun", emit.getMunicipio());
        el(ee, "UF", emit.getUf());
        el(ee, "CEP", emit.getCep());
        el(ee, "cPais", "1058");
        el(ee, "xPais", "BRASIL");
        el(ee, "fone", emit.getFone());
        el(e, "IE", emit.getIe());
        el(e, "CRT", emit.getCrt());

        if (temDest) {
            Element d = el(inf, "dest");
            el(d, documento.length() == 14 ? "CNPJ" : "CPF", documento);
            el(d, "xNome", homologacao ? HOMOLOGACAO_DEST : limitar(n.getClienteNome(), 60));
            if (!nfce) {
                Ibge.Municipio mun = Ibge.municipio(n.getClienteCidade(), n.getClienteUf());
                Element ed = el(d, "enderDest");
                el(ed, "xLgr", NAO_INFORMADO);
                el(ed, "nro", NAO_INFORMADO);
                el(ed, "xBairro", NAO_INFORMADO);
                el(ed, "cMun", mun.codigo());
                el(ed, "xMun", mun.nome());
                el(ed, "UF", mun.uf());
                el(ed, "cPais", "1058");
                el(ed, "xPais", "BRASIL");
            }
            el(d, "indIEDest", "9");
            if (n.getClienteEmail() != null && !n.getClienteEmail().isBlank()) {
                el(d, "email", n.getClienteEmail());
            }
        }

        BigDecimal tProd = BigDecimal.ZERO;
        BigDecimal tDesc = BigDecimal.ZERO;
        BigDecimal tBC = BigDecimal.ZERO;
        BigDecimal tICMS = BigDecimal.ZERO;
        int nItem = 1;
        for (ItemCalculado i : itens) {
            NotaLinha l = i.linha();
            Element det = el(inf, "det");
            det.setAttribute("nItem", String.valueOf(nItem));
            Element p = el(det, "prod");
            el(p, "cProd", l.getCodigo());
            el(p, "cEAN", "SEM GTIN");
            el(p, "xProd", nfce && homologacao && nItem == 1 ? HOMOLOGACAO_ITEM : limitar(Ibge.semAcento(l.getDescricao()), 120));
            el(p, "NCM", l.getNcm());
            el(p, "CFOP", l.getCfop());
            el(p, "uCom", "UN");
            el(p, "qCom", qtd(l.getQuantidade()));
            el(p, "vUnCom", unit(l.getValorUnitario()));
            el(p, "vProd", dois(i.vProd()));
            el(p, "cEANTrib", "SEM GTIN");
            el(p, "uTrib", "UN");
            el(p, "qTrib", qtd(l.getQuantidade()));
            el(p, "vUnTrib", unit(l.getValorUnitario()));
            if (i.vDesc().signum() > 0) {
                el(p, "vDesc", dois(i.vDesc()));
            }
            el(p, "indTot", "1");

            Element imp = el(det, "imposto");
            Element icms = el(imp, "ICMS");
            String cst = l.getCstIcms() == null ? "00" : l.getCstIcms();
            String orig = l.getOrigem() == null ? "0" : l.getOrigem();
            switch (cst) {
                case "40", "41", "50" -> {
                    Element g = el(icms, "ICMS40");
                    el(g, "orig", orig);
                    el(g, "CST", cst);
                }
                case "60" -> {
                    Element g = el(icms, "ICMS60");
                    el(g, "orig", orig);
                    el(g, "CST", cst);
                }
                default -> {
                    Element g = el(icms, "ICMS00");
                    el(g, "orig", orig);
                    el(g, "CST", "00");
                    el(g, "modBC", "3");
                    el(g, "vBC", dois(i.vBC()));
                    el(g, "pICMS", dois(i.pICMS()));
                    el(g, "vICMS", dois(i.vICMS()));
                    tBC = tBC.add(i.vBC());
                    tICMS = tICMS.add(i.vICMS());
                }
            }
            el(el(el(imp, "PIS"), "PISNT"), "CST", "07");
            el(el(el(imp, "COFINS"), "COFINSNT"), "CST", "07");
            tProd = tProd.add(i.vProd());
            tDesc = tDesc.add(i.vDesc());
            nItem++;
        }

        BigDecimal vNF = tProd.subtract(tDesc);
        Element tot = el(el(inf, "total"), "ICMSTot");
        el(tot, "vBC", dois(tBC));
        el(tot, "vICMS", dois(tICMS));
        for (String zero : List.of("vICMSDeson", "vFCP", "vBCST", "vST", "vFCPST", "vFCPSTRet")) {
            el(tot, zero, "0.00");
        }
        el(tot, "vProd", dois(tProd));
        el(tot, "vFrete", "0.00");
        el(tot, "vSeg", "0.00");
        el(tot, "vDesc", dois(tDesc));
        for (String zero : List.of("vII", "vIPI", "vIPIDevol", "vPIS", "vCOFINS", "vOutro")) {
            el(tot, zero, "0.00");
        }
        el(tot, "vNF", dois(vNF));

        el(el(inf, "transp"), "modFrete", "9");

        Element detPag = el(el(inf, "pag"), "detPag");
        if (devolucao) {
            el(detPag, "tPag", "90");
            el(detPag, "vPag", "0.00");
        } else {
            String tPag = T_PAG.getOrDefault(n.getMetodoPagamento(), "99");
            el(detPag, "tPag", tPag);
            if ("99".equals(tPag)) {
                el(detPag, "xPag", "OUTROS");
            }
            el(detPag, "vPag", dois(vNF));
            if ("03".equals(tPag) || "04".equals(tPag)) {
                Element card = el(detPag, "card");
                el(card, "tpIntegra", "2");
                el(card, "tBand", T_BAND.getOrDefault(n.getBandeira(), "99"));
            }
        }

        el(el(inf, "infAdic"), "infCpl", devolucao
                ? limitar(Ibge.semAcento(n.getMotivo()) + ". REF. NF-E " + n.getChaveReferenciada(), 5000)
                : "VENDA " + n.getVendaId());

        if (nfce) {
            Element supl = el(nfe, "infNFeSupl");
            el(supl, "qrCode", qrCode(chave, tpAmb));
            el(supl, "urlChave", props.getUrlChave());
        }
        return doc;
    }

    /** QR Code da NFC-e versão 2, emissão online: {@code p=chave|2|tpAmb|idCSC|SHA1(chave|2|tpAmb|idCSC + CSC)}. */
    String qrCode(ChaveAcesso chave, String tpAmb) {
        String idCsc = String.valueOf(Integer.parseInt(props.getCscId()));
        String dados = chave.valor() + "|2|" + tpAmb + "|" + idCsc;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest((dados + props.getCsc()).getBytes(StandardCharsets.UTF_8));
            return props.getUrlQrCode() + "?p=" + dados + "|" + HexFormat.of().withUpperCase().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Rateio do desconto proporcional ao valor de cada item; a sobra de centavos vai para o
     * último item, para a soma dos {@code vDesc} bater com o desconto da nota.
     */
    List<ItemCalculado> calcular(List<NotaLinha> linhas, BigDecimal desconto) {
        BigDecimal desc = desconto == null ? BigDecimal.ZERO : desconto.setScale(2, RoundingMode.HALF_EVEN);
        List<BigDecimal> vProds = linhas.stream()
                .map(l -> l.getValorUnitario().multiply(BigDecimal.valueOf(l.getQuantidade())).setScale(2, RoundingMode.HALF_EVEN))
                .toList();
        BigDecimal total = vProds.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ItemCalculado> out = new ArrayList<>();
        BigDecimal rateado = BigDecimal.ZERO;
        for (int i = 0; i < linhas.size(); i++) {
            NotaLinha l = linhas.get(i);
            BigDecimal vProd = vProds.get(i);
            BigDecimal vDesc;
            if (i == linhas.size() - 1) {
                vDesc = desc.subtract(rateado);
            } else if (total.signum() == 0) {
                vDesc = BigDecimal.ZERO.setScale(2);
            } else {
                vDesc = desc.multiply(vProd).divide(total, 2, RoundingMode.HALF_EVEN);
            }
            rateado = rateado.add(vDesc);
            BigDecimal vBC = vProd.subtract(vDesc);
            BigDecimal pICMS = l.getAliquotaIcms() == null ? BigDecimal.ZERO : l.getAliquotaIcms();
            BigDecimal vICMS = vBC.multiply(pICMS).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            out.add(new ItemCalculado(l, vProd, vDesc, vBC, pICMS, vICMS));
        }
        return out;
    }

    static Document novoDocumento() {
        try {
            var f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            return f.newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Element el(Element pai, String nome) {
        Element e = pai.getOwnerDocument().createElementNS(NS, nome);
        pai.appendChild(e);
        return e;
    }

    private static Element el(Element pai, String nome, String texto) {
        Element e = el(pai, nome);
        e.setTextContent(texto);
        return e;
    }

    static String dois(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    static String unit(BigDecimal v) {
        return v.setScale(Math.max(2, Math.min(10, v.stripTrailingZeros().scale())), RoundingMode.HALF_EVEN)
                .toPlainString();
    }

    static String qtd(long q) {
        return q + ".0000";
    }

    private static String limitar(String s, int max) {
        String t = s == null || s.isBlank() ? NAO_INFORMADO : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
