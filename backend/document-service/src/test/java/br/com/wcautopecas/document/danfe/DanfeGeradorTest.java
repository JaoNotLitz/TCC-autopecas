package br.com.wcautopecas.document.danfe;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class DanfeGeradorTest {

    private final DanfeGerador gerador = new DanfeGerador();

    public static byte[] fixture(String nome) throws IOException {
        try (InputStream in = DanfeGeradorTest.class.getResourceAsStream("/xml/" + nome)) {
            return in.readAllBytes();
        }
    }

    public static String texto(byte[] pdf) throws IOException {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
            return sb.toString();
        }
    }

    static Rectangle tamanho(byte[] pdf) throws IOException {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            return doc.getPage(1).getPageSize();
        }
    }

    @Test
    void leNfeDoNfeProc() throws IOException {
        DanfeDados d = DanfeDados.ler(fixture("nfe-proc.xml"));
        assertThat(d.nfce()).isFalse();
        assertThat(d.homologacao()).isTrue();
        assertThat(d.chave()).hasSize(44).startsWith("35");
        assertThat(d.itens()).hasSize(2);
        assertThat(d.vNF()).isEqualByComparingTo(new BigDecimal("224.80"));
        assertThat(d.destinatario()).isNotNull();
        assertThat(d.destinatario().nome()).isNotBlank();
        assertThat(d.protocolo()).isNotBlank();
    }

    @Test
    void danfeA4DaNfe() throws IOException {
        DanfeDados d = DanfeDados.ler(fixture("nfe-proc.xml"));
        byte[] pdf = gerador.gerar(d);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(tamanho(pdf).getWidth()).isGreaterThan(500f); // A4 = 595pt
        String t = texto(pdf);
        assertThat(t).contains("DANFE").contains("SEM VALOR FISCAL").contains(d.protocolo());
        assertThat(t.replaceAll("\\s", "")).contains(DanfeGerador.chaveFormatada(d.chave()).replaceAll("\\s", ""));
    }

    @Test
    void danfeNfceEmBobina() throws IOException {
        DanfeDados d = DanfeDados.ler(fixture("nfce-proc.xml"));
        assertThat(d.nfce()).isTrue();
        assertThat(d.qrCode()).isNotBlank();
        byte[] pdf = gerador.gerar(d);
        assertThat(tamanho(pdf).getWidth()).isEqualTo(DanfeGerador.BOBINA);
        assertThat(texto(pdf)).contains("NFC-e").contains("SEM VALOR FISCAL");
    }

    @Test
    void carimboDeCancelada() throws IOException {
        byte[] pdf = gerador.gerar(DanfeDados.ler(fixture("nfe-proc.xml")));
        byte[] carimbado = gerador.carimbarCancelada(pdf);
        assertThat(texto(pdf)).doesNotContain("CANCELADA");
        assertThat(texto(carimbado)).contains("CANCELADA");
    }
}
