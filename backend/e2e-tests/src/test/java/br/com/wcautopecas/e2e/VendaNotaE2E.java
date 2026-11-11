package br.com.wcautopecas.e2e;

import static br.com.wcautopecas.e2e.Stack.PRAZO;
import static br.com.wcautopecas.e2e.Stack.aguardarEstoque;
import static br.com.wcautopecas.e2e.Stack.aguardarNotaDaVenda;
import static br.com.wcautopecas.e2e.Stack.api;
import static br.com.wcautopecas.e2e.Stack.cliente;
import static br.com.wcautopecas.e2e.Stack.criarVenda;
import static br.com.wcautopecas.e2e.Stack.estoque;
import static br.com.wcautopecas.e2e.Stack.garantirEstoque;
import static br.com.wcautopecas.e2e.Stack.reservado;
import static br.com.wcautopecas.e2e.Stack.valorSemGatilho;
import static br.com.wcautopecas.e2e.Stack.venda;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-B17.1: a venda vira nota autorizada, com XML, DANFE e estoque baixado. */
@DisplayName("HU-B17.1 Venda até o DANFE")
class VendaNotaE2E {

    private static final long PF_001 = 1;
    private static final long DF_001 = 3;

    @Test
    @DisplayName("TC-B17.1.1 NFC-e: autorizada em até 60 s, com XML, PDF e estoque baixado")
    void vendaNfce() {
        garantirEstoque(PF_001, 10);
        long antes = estoque(PF_001);
        long reservadoAntes = reservado(PF_001);

        long vendaId = venda("nfce", null, PF_001, 2, valorSemGatilho(PF_001, 2));
        JsonNode nota = aguardarNotaDaVenda(vendaId, "autorizada", PRAZO);

        assertThat(nota.path("tipo").asText()).isEqualTo("NFCE");
        String chave = nota.path("chaveAcesso").asText();
        assertThat(chave).matches("\\d{44}");
        aguardarEstoque(PF_001, antes - 2);
        assertThat(reservado(PF_001)).isEqualTo(reservadoAntes);
        conferirDocumentos(nota.path("id").asLong(), chave);
    }

    @Test
    @DisplayName("TC-B17.1.2 NF-e para cliente PJ: autorizada com XML e PDF")
    void vendaNfePj() {
        garantirEstoque(DF_001, 5);
        long antes = estoque(DF_001);
        long pj = cliente("PJ");

        long vendaId = venda("nfe", pj, DF_001, 1, valorSemGatilho(DF_001, 1));
        JsonNode nota = aguardarNotaDaVenda(vendaId, "autorizada", PRAZO);

        assertThat(nota.path("tipo").asText()).isEqualTo("NFE");
        assertThat(nota.path("clienteId").asLong()).isEqualTo(pj);
        assertThat(nota.path("clienteDocumento").asText()).hasSize(14);
        aguardarEstoque(DF_001, antes - 1);
        conferirDocumentos(nota.path("id").asLong(), nota.path("chaveAcesso").asText());
    }

    @Test
    @DisplayName("TC-B17.1.3 NF-e sem cliente responde 400 e não reserva estoque")
    void nfeSemCliente() {
        long reservadoAntes = reservado(PF_001);

        criarVenda("nfe", null, PF_001, 1, new BigDecimal("89.90")).then().statusCode(400);

        assertThat(reservado(PF_001)).isEqualTo(reservadoAntes);
    }

    private static void conferirDocumentos(long notaId, String chave) {
        String xml = api().get("/notas/{id}/xml", notaId).then().statusCode(200).extract().asString();
        assertThat(xml).contains("<nfeProc").contains(chave).contains("<protNFe");

        byte[] pdf = api().accept("application/pdf").get("/notas/{id}/pdf", notaId).then().statusCode(200)
                .extract().asByteArray();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(Pdf.texto(pdf).replaceAll("\\s", "")).contains(chave);
    }
}
