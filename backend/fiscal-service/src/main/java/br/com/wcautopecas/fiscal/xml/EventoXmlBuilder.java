package br.com.wcautopecas.fiscal.xml;

import static br.com.wcautopecas.fiscal.xml.NfeXmlBuilder.el;

import br.com.wcautopecas.fiscal.FiscalProperties;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * XML dos eventos da EPB-10: cancelamento (evento 110111, leiaute 1.00) e inutilização
 * ({@code inutNFe} 4.00). Os dois saem sem assinatura; quem chama assina {@code infEvento} /
 * {@code infInut} com o {@link AssinadorXml}.
 */
@Component
public class EventoXmlBuilder {

    public static final String CANCELAMENTO = "110111";

    private final FiscalProperties props;

    public EventoXmlBuilder(FiscalProperties props) {
        this.props = props;
    }

    /** {@code Id = "ID110111" + chave + nSeqEvento(2)}. */
    public static String idCancelamento(String chave) {
        return "ID" + CANCELAMENTO + chave + "01";
    }

    /** {@code Id = "ID" + cUF + ano(2) + CNPJ + mod + serie(3) + nNFIni(9) + nNFFin(9)}. */
    public static String idInutilizacao(String cUF, int ano, String cnpj, String modelo, String serie, long ini,
            long fim) {
        return "ID" + cUF + String.format("%02d", ano % 100) + cnpj + modelo
                + String.format("%03d", Integer.parseInt(serie)) + String.format("%09d", ini)
                + String.format("%09d", fim);
    }

    public Document cancelamento(String chave, String protocolo, String justificativa, ZonedDateTime quando) {
        Document doc = NfeXmlBuilder.novoDocumento();
        Element evento = doc.createElementNS(NfeXmlBuilder.NS, "evento");
        evento.setAttribute("versao", "1.00");
        doc.appendChild(evento);
        Element inf = el(evento, "infEvento");
        inf.setAttribute("Id", idCancelamento(chave));
        el(inf, "cOrgao", chave.substring(0, 2));
        el(inf, "tpAmb", tpAmb());
        el(inf, "CNPJ", cnpj());
        el(inf, "chNFe", chave);
        el(inf, "dhEvento", NfeXmlBuilder.DH.format(quando.withNano(0)));
        el(inf, "tpEvento", CANCELAMENTO);
        el(inf, "nSeqEvento", "1");
        el(inf, "verEvento", "1.00");
        Element det = el(inf, "detEvento");
        det.setAttribute("versao", "1.00");
        el(det, "descEvento", "Cancelamento");
        el(det, "nProt", protocolo);
        el(det, "xJust", limpar(justificativa));
        return doc;
    }

    public Document inutilizacao(String modelo, String serie, long ini, long fim, String justificativa,
            ZonedDateTime quando) {
        String cUF = Ibge.codigoUf(props.getEmitente().getUf());
        int ano = quando.getYear() % 100;
        Document doc = NfeXmlBuilder.novoDocumento();
        Element inut = doc.createElementNS(NfeXmlBuilder.NS, "inutNFe");
        inut.setAttribute("versao", "4.00");
        doc.appendChild(inut);
        Element inf = el(inut, "infInut");
        inf.setAttribute("Id", idInutilizacao(cUF, ano, cnpj(), modelo, serie, ini, fim));
        el(inf, "tpAmb", tpAmb());
        el(inf, "xServ", "INUTILIZAR");
        el(inf, "cUF", cUF);
        el(inf, "ano", String.format("%02d", ano));
        el(inf, "CNPJ", cnpj());
        el(inf, "mod", modelo);
        el(inf, "serie", Integer.toString(Integer.parseInt(serie)));
        el(inf, "nNFIni", Long.toString(ini));
        el(inf, "nNFFin", Long.toString(fim));
        el(inf, "xJust", limpar(justificativa));
        return doc;
    }

    private String tpAmb() {
        return "producao".equalsIgnoreCase(props.getAmbiente()) ? "1" : "2";
    }

    private String cnpj() {
        return props.getEmitente().getCnpj().replaceAll("\\D", "");
    }

    /** xJust: 15 a 255 caracteres, sem acento nem quebra de linha. */
    static String limpar(String s) {
        String t = Ibge.semAcento(s).replaceAll("\\s+", " ").trim();
        return t.length() > 255 ? t.substring(0, 255) : t;
    }
}
