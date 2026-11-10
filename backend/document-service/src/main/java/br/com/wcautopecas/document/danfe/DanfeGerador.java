package br.com.wcautopecas.document.danfe;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * DANFE (HU-B13.1): NF-e em A4 retrato com Code-128C da chave; NFC-e em bobina de 80 mm com QR
 * Code. Em homologação, marca d'água "SEM VALOR FISCAL". {@link #carimbarCancelada} sobrepõe o
 * carimbo da HU-B13.3 em memória.
 */
@Component
public class DanfeGerador {

    static final float BOBINA = 226f; // 80 mm em pontos
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    static final DeviceRgb VERMELHO = new DeviceRgb(200, 0, 0);

    public byte[] gerar(DanfeDados d) {
        return d.nfce() ? nfce(d) : nfe(d);
    }

    // ---- NF-e (modelo 55)

    byte[] nfe(DanfeDados d) {
        var out = new ByteArrayOutputStream();
        try (var pdf = new PdfDocument(new PdfWriter(out)); var doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(20, 20, 20, 20);
            PdfFont normal = fonte(StandardFonts.HELVETICA);
            PdfFont negrito = fonte(StandardFonts.HELVETICA_BOLD);
            doc.setFont(normal).setFontSize(7);

            DanfeDados.Pessoa e = d.emitente();
            Table topo = new Table(UnitValue.createPercentArray(new float[] {38, 20, 42})).useAllAvailableWidth();
            topo.addCell(caixa().add(new Paragraph(texto(e.nome())).setFont(negrito).setFontSize(10))
                    .add(new Paragraph(texto(e.logradouro()) + " - " + texto(e.bairro())))
                    .add(new Paragraph(texto(e.municipio()) + " - " + texto(e.uf()) + "  CEP " + texto(e.cep())))
                    .add(new Paragraph("Fone " + texto(e.fone()))));
            topo.addCell(caixa().setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph("DANFE").setFont(negrito).setFontSize(12))
                    .add(new Paragraph("Documento Auxiliar da Nota Fiscal Eletrônica"))
                    .add(new Paragraph("0 - Entrada   1 - Saída"))
                    .add(new Paragraph("Nº " + texto(d.numero()) + "   Série " + texto(d.serie())).setFont(negrito)));
            Image barras = new Image(codigo128(d.chave(), pdf)).setWidth(UnitValue.createPercentValue(100)).setHeight(34);
            topo.addCell(caixa().add(barras).add(rotulo("CHAVE DE ACESSO", negrito))
                    .add(new Paragraph(chaveFormatada(d.chave())).setFont(negrito).setFontSize(8))
                    .add(new Paragraph("Consulta de autenticidade no portal nacional da NF-e www.nfe.fazenda.gov.br/portal")
                            .setFontSize(6)));
            doc.add(topo);

            Table natureza = new Table(UnitValue.createPercentArray(new float[] {58, 42})).useAllAvailableWidth();
            natureza.addCell(campo("NATUREZA DA OPERAÇÃO", d.natOp(), negrito));
            natureza.addCell(campo("PROTOCOLO DE AUTORIZAÇÃO DE USO",
                    texto(d.protocolo()) + "  " + dataHora(d.dhRecbto()), negrito));
            doc.add(natureza);
            Table ies = new Table(UnitValue.createPercentArray(new float[] {50, 50})).useAllAvailableWidth();
            ies.addCell(campo("INSCRIÇÃO ESTADUAL", e.ie(), negrito));
            ies.addCell(campo("CNPJ", documento(e.documento()), negrito));
            doc.add(ies);

            doc.add(rotulo("DESTINATÁRIO / REMETENTE", negrito).setMarginTop(4));
            DanfeDados.Pessoa dest = d.destinatario();
            Table destino = new Table(UnitValue.createPercentArray(new float[] {50, 25, 25})).useAllAvailableWidth();
            destino.addCell(campo("NOME / RAZÃO SOCIAL", dest == null ? null : dest.nome(), negrito));
            destino.addCell(campo("CNPJ / CPF", dest == null ? null : documento(dest.documento()), negrito));
            destino.addCell(campo("DATA DA EMISSÃO", dataHora(d.dhEmi()), negrito));
            destino.addCell(campo("ENDEREÇO", dest == null ? null : dest.logradouro(), negrito));
            destino.addCell(campo("MUNICÍPIO", dest == null ? null : dest.municipio(), negrito));
            destino.addCell(campo("UF", dest == null ? null : dest.uf(), negrito));
            doc.add(destino);

            doc.add(rotulo("CÁLCULO DO IMPOSTO", negrito).setMarginTop(4));
            Table impostos = new Table(UnitValue.createPercentArray(5)).useAllAvailableWidth();
            impostos.addCell(campo("BASE DE CÁLCULO DO ICMS", moeda(d.vBC()), negrito));
            impostos.addCell(campo("VALOR DO ICMS", moeda(d.vICMS()), negrito));
            impostos.addCell(campo("VALOR TOTAL DOS PRODUTOS", moeda(d.vProd()), negrito));
            impostos.addCell(campo("DESCONTO", moeda(d.vDesc()), negrito));
            impostos.addCell(campo("VALOR TOTAL DA NOTA", moeda(d.vNF()), negrito));
            doc.add(impostos);

            doc.add(rotulo("DADOS DOS PRODUTOS / SERVIÇOS", negrito).setMarginTop(4));
            Table itens = new Table(UnitValue.createPercentArray(new float[] {10, 38, 9, 7, 5, 9, 11, 11}))
                    .useAllAvailableWidth();
            for (String h : new String[] {"CÓDIGO", "DESCRIÇÃO", "NCM", "CFOP", "UN", "QTD", "V. UNIT.", "V. TOTAL"}) {
                itens.addHeaderCell(new Cell().add(new Paragraph(h).setFont(negrito).setFontSize(6))
                        .setBackgroundColor(new DeviceRgb(235, 235, 235)));
            }
            for (DanfeDados.Item i : d.itens()) {
                itens.addCell(celula(i.codigo()));
                itens.addCell(celula(i.descricao()));
                itens.addCell(celula(i.ncm()));
                itens.addCell(celula(i.cfop()));
                itens.addCell(celula(i.unidade()));
                itens.addCell(celula(numero(i.quantidade())).setTextAlignment(TextAlignment.RIGHT));
                itens.addCell(celula(moeda(i.valorUnitario())).setTextAlignment(TextAlignment.RIGHT));
                itens.addCell(celula(moeda(i.valorTotal())).setTextAlignment(TextAlignment.RIGHT));
            }
            doc.add(itens);

            doc.add(rotulo("DADOS ADICIONAIS", negrito).setMarginTop(4));
            doc.add(new Paragraph(texto(d.infCpl())).setBorder(new SolidBorder(0.5f)).setPadding(4).setMinHeight(40));
            if (d.homologacao()) {
                marcaDagua(pdf, negrito, "SEM VALOR FISCAL", 60, new DeviceRgb(160, 160, 160));
            }
        }
        return out.toByteArray();
    }

    // ---- NFC-e (modelo 65), bobina de 80 mm

    byte[] nfce(DanfeDados d) {
        var out = new ByteArrayOutputStream();
        float altura = 520 + d.itens().size() * 26f + d.pagamentos().size() * 12f;
        try (var pdf = new PdfDocument(new PdfWriter(out));
                var doc = new Document(pdf, new PageSize(BOBINA, altura))) {
            doc.setMargins(8, 8, 8, 8);
            PdfFont normal = fonte(StandardFonts.HELVETICA);
            PdfFont negrito = fonte(StandardFonts.HELVETICA_BOLD);
            doc.setFont(normal).setFontSize(6.5f).setTextAlignment(TextAlignment.CENTER);

            DanfeDados.Pessoa e = d.emitente();
            doc.add(new Paragraph(texto(e.nome())).setFont(negrito).setFontSize(8));
            doc.add(new Paragraph("CNPJ " + documento(e.documento()) + "  IE " + texto(e.ie())));
            doc.add(new Paragraph(texto(e.logradouro()) + " - " + texto(e.bairro()) + " - " + texto(e.municipio())
                    + "/" + texto(e.uf())));
            doc.add(separador());
            doc.add(new Paragraph("DANFE NFC-e - Documento Auxiliar da Nota Fiscal de Consumidor Eletrônica")
                    .setFont(negrito));
            if (d.homologacao()) {
                doc.add(new Paragraph("EMITIDA EM AMBIENTE DE HOMOLOGAÇÃO - SEM VALOR FISCAL").setFont(negrito)
                        .setFontColor(VERMELHO));
            }
            doc.add(separador());

            Table itens = new Table(UnitValue.createPercentArray(new float[] {46, 14, 18, 22})).useAllAvailableWidth()
                    .setFontSize(6);
            for (String h : new String[] {"DESCRIÇÃO", "QTD", "V. UNIT", "V. TOTAL"}) {
                itens.addHeaderCell(semBorda(h).setFont(negrito));
            }
            for (DanfeDados.Item i : d.itens()) {
                itens.addCell(semBorda(texto(i.codigo()) + " " + texto(i.descricao())).setTextAlignment(TextAlignment.LEFT));
                itens.addCell(semBorda(numero(i.quantidade()) + " " + texto(i.unidade())));
                itens.addCell(semBorda(moeda(i.valorUnitario())).setTextAlignment(TextAlignment.RIGHT));
                itens.addCell(semBorda(moeda(i.valorTotal())).setTextAlignment(TextAlignment.RIGHT));
            }
            doc.add(itens);
            doc.add(separador());

            Table totais = new Table(UnitValue.createPercentArray(new float[] {60, 40})).useAllAvailableWidth();
            linha(totais, "Qtd. total de itens", Integer.toString(d.itens().size()), normal);
            linha(totais, "Valor total R$", moeda(d.vProd()), normal);
            if (d.vDesc() != null && d.vDesc().signum() > 0) {
                linha(totais, "Desconto R$", moeda(d.vDesc()), normal);
            }
            linha(totais, "Valor a pagar R$", moeda(d.vNF()), negrito);
            linha(totais, "FORMA DE PAGAMENTO", "VALOR PAGO R$", negrito);
            for (DanfeDados.Pagamento p : d.pagamentos()) {
                linha(totais, p.forma(), moeda(p.valor()), normal);
            }
            if (d.troco() != null && d.troco().signum() > 0) {
                linha(totais, "Troco R$", moeda(d.troco()), normal);
            }
            doc.add(totais);
            doc.add(separador());

            doc.add(new Paragraph("Consulte pela Chave de Acesso em").setFont(negrito));
            doc.add(new Paragraph(texto(d.urlChave())));
            doc.add(new Paragraph(chaveFormatada(d.chave())).setFont(negrito));
            doc.add(separador());
            DanfeDados.Pessoa c = d.destinatario();
            doc.add(new Paragraph(c == null || c.documento() == null ? "CONSUMIDOR NÃO IDENTIFICADO"
                    : "CONSUMIDOR - CPF/CNPJ " + documento(c.documento()) + (c.nome() == null ? "" : " - " + c.nome())));
            doc.add(new Paragraph("NFC-e nº " + texto(d.numero()) + "  Série " + texto(d.serie()) + "  "
                    + dataHora(d.dhEmi())).setFont(negrito));
            doc.add(new Paragraph("Protocolo de autorização: " + texto(d.protocolo())));
            doc.add(new Paragraph("Data de autorização: " + dataHora(d.dhRecbto())));
            if (d.qrCode() != null) {
                var qr = new BarcodeQRCode(d.qrCode());
                doc.add(new Image(qr.createFormXObject(ColorConstants.BLACK, pdf)).setWidth(120).setHeight(120)
                        .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER));
            }
        }
        return out.toByteArray();
    }

    // ---- carimbo da HU-B13.3

    public byte[] carimbarCancelada(byte[] pdfOriginal) {
        var out = new ByteArrayOutputStream();
        try (var pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfOriginal)), new PdfWriter(out))) {
            PdfFont negrito = fonte(StandardFonts.HELVETICA_BOLD);
            float tamanho = pdf.getFirstPage().getPageSize().getWidth() < 300 ? 34 : 90;
            marcaDagua(pdf, negrito, "CANCELADA", tamanho, VERMELHO);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void marcaDagua(PdfDocument pdf, PdfFont fonte, String texto, float tamanho, DeviceRgb cor) {
        for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
            PdfPage pagina = pdf.getPage(i);
            Rectangle area = pagina.getPageSize();
            var canvas = new PdfCanvas(pagina.newContentStreamAfter(), pagina.getResources(), pdf);
            canvas.saveState().setExtGState(new PdfExtGState().setFillOpacity(0.35f));
            try (var c = new Canvas(canvas, area)) {
                c.setFont(fonte).setFontSize(tamanho).setFontColor(cor);
                c.showTextAligned(new Paragraph(texto), area.getWidth() / 2, area.getHeight() / 2, i,
                        TextAlignment.CENTER, VerticalAlignment.MIDDLE, (float) Math.toRadians(45));
            }
            canvas.restoreState();
        }
    }

    // ---- utilitários

    private static com.itextpdf.kernel.pdf.xobject.PdfFormXObject codigo128(String chave, PdfDocument pdf) {
        var b = new Barcode128(pdf);
        b.setCodeType(Barcode128.CODE_C);
        b.setCode(chave);
        b.setFont(null);
        return b.createFormXObject(ColorConstants.BLACK, null, pdf);
    }

    private static PdfFont fonte(String nome) {
        try {
            return PdfFontFactory.createFont(nome);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Cell caixa() {
        return new Cell().setBorder(new SolidBorder(0.5f)).setPadding(4);
    }

    private static Paragraph rotulo(String t, PdfFont negrito) {
        return new Paragraph(t).setFont(negrito).setFontSize(6).setMargin(0);
    }

    private static Cell campo(String rotulo, String valor, PdfFont negrito) {
        return new Cell().setBorder(new SolidBorder(0.5f)).setPadding(2)
                .add(new Paragraph(rotulo).setFont(negrito).setFontSize(5.5f).setMargin(0))
                .add(new Paragraph(texto(valor)).setFontSize(8).setMargin(0));
    }

    private static Cell celula(String t) {
        return new Cell().add(new Paragraph(texto(t)).setFontSize(6.5f)).setPadding(2);
    }

    private static Cell semBorda(String t) {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(1).add(new Paragraph(texto(t)));
    }

    private static void linha(Table t, String a, String b, PdfFont fonte) {
        t.addCell(semBorda(a).setFont(fonte).setTextAlignment(TextAlignment.LEFT));
        t.addCell(semBorda(b).setFont(fonte).setTextAlignment(TextAlignment.RIGHT));
    }

    private static Paragraph separador() {
        return new Paragraph("").setBorderBottom(new DashedBorder(0.5f)).setMargin(2);
    }

    static String texto(String s) {
        return s == null ? "" : s;
    }

    static String moeda(BigDecimal v) {
        if (v == null) {
            return "0,00";
        }
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(v);
    }

    static String numero(BigDecimal v) {
        if (v == null) {
            return "";
        }
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        f.setMaximumFractionDigits(4);
        return f.format(v);
    }

    static String dataHora(String iso) {
        if (iso == null) {
            return "";
        }
        try {
            return DATA_HORA.format(OffsetDateTime.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }

    static String chaveFormatada(String chave) {
        if (chave == null) {
            return "";
        }
        return chave.replaceAll("(\\d{4})(?=\\d)", "$1 ");
    }

    static String documento(String d) {
        if (d == null) {
            return "";
        }
        if (d.length() == 14) {
            return d.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        if (d.length() == 11) {
            return d.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return d;
    }
}
