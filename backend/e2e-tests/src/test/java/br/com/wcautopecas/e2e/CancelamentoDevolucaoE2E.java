package br.com.wcautopecas.e2e;

import static br.com.wcautopecas.e2e.Stack.PRAZO;
import static br.com.wcautopecas.e2e.Stack.aguardarEstoque;
import static br.com.wcautopecas.e2e.Stack.aguardarNota;
import static br.com.wcautopecas.e2e.Stack.aguardarNotaDaVenda;
import static br.com.wcautopecas.e2e.Stack.api;
import static br.com.wcautopecas.e2e.Stack.cliente;
import static br.com.wcautopecas.e2e.Stack.estoque;
import static br.com.wcautopecas.e2e.Stack.garantirEstoque;
import static br.com.wcautopecas.e2e.Stack.json;
import static br.com.wcautopecas.e2e.Stack.valorSemGatilho;
import static br.com.wcautopecas.e2e.Stack.venda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-B17.2: cancelamento, devolução e inutilização ajustando nota e estoque. */
@DisplayName("HU-B17.2 Cancelamento, devolução e inutilização")
class CancelamentoDevolucaoE2E {

    private static final long PF_002 = 2;
    private static final long FILTRO_OLEO = 4;

    @Test
    @DisplayName("TC-B17.2.1 Cancelamento da NFC-e devolve o estoque e cancela a venda")
    void cancelamento() {
        garantirEstoque(PF_002, 10);
        long antes = estoque(PF_002);
        long vendaId = venda("nfce", null, PF_002, 2, valorSemGatilho(PF_002, 2));
        long notaId = aguardarNotaDaVenda(vendaId, "autorizada", PRAZO).path("id").asLong();
        aguardarEstoque(PF_002, antes - 2);

        api().body(Map.of("justificativa", "Cliente desistiu da compra no balcao"))
                .post("/notas/{id}/cancelamento", notaId).then().statusCode(200);

        aguardarNota(notaId, "cancelada");
        aguardarEstoque(PF_002, antes);
        await("venda " + vendaId + " cancelada").atMost(PRAZO).until(() -> "cancelada".equals(
                api().get("/vendas/{id}", vendaId).then().statusCode(200).extract().path("status")));
        // O DANFE da cancelada continua disponível, agora com o carimbo.
        assertThat(Pdf.texto(api().get("/notas/{id}/pdf", notaId).then().statusCode(200).extract().asByteArray()))
                .contains("CANCELADA");
    }

    @Test
    @DisplayName("TC-B17.2.2 Devolução parcial soma o estoque; devolver acima do saldo responde 422")
    void devolucao() {
        garantirEstoque(FILTRO_OLEO, 10);
        long antes = estoque(FILTRO_OLEO);
        BigDecimal valor = valorSemGatilho(FILTRO_OLEO, 3);
        long vendaId = venda("nfe", cliente("PJ"), FILTRO_OLEO, 3, valor);
        long notaId = aguardarNotaDaVenda(vendaId, "autorizada", PRAZO).path("id").asLong();
        aguardarEstoque(FILTRO_OLEO, antes - 3);

        JsonNode dev = json(api().body(devolucao(notaId, 1, valor)).post("/notas/devolucao").then().statusCode(201)
                .extract().asString());
        assertThat(dev.path("tipo").asText()).isEqualTo("DEVOLUCAO");
        assertThat(dev.path("notaReferenciadaId").asLong()).isEqualTo(notaId);

        aguardarNota(dev.path("id").asLong(), "autorizada");
        aguardarEstoque(FILTRO_OLEO, antes - 2);

        // Saldo devolvível agora é 2: pedir 3 é recusado com a mensagem do saldo.
        String erro = api().body(devolucao(notaId, 3, valor)).post("/notas/devolucao").then().statusCode(422)
                .extract().asString();
        assertThat(erro).contains("pode devolver até 2");
    }

    @Test
    @DisplayName("TC-B17.2.3 Inutilização de uma faixa acima do último número é homologada")
    void inutilizacao() {
        long inicio = 500_000_000L + (System.currentTimeMillis() / 1000) % 100_000_000L;
        JsonNode criada = json(api().body(Map.of("serie", "1", "numeroInicial", inicio, "numeroFinal", inicio + 1,
                "justificativa", "Falha no sistema pulou a numeracao")).post("/inutilizacoes").then().statusCode(201)
                .extract().asString());
        long id = criada.path("id").asLong();

        await("inutilização " + id + " homologada").atMost(PRAZO).until(() -> {
            for (JsonNode i : json(api().get("/inutilizacoes").then().statusCode(200).extract().asString())) {
                if (i.path("id").asLong() == id) {
                    return !i.path("protocolo").asText("").isBlank();
                }
            }
            return false;
        });
    }

    /** {@code DevolucaoInput} do front. */
    private static Map<String, Object> devolucao(long notaId, long quantidade, BigDecimal valor) {
        return Map.of("notaReferenciadaId", notaId, "motivo", "Peça com defeito",
                "itens", List.of(Map.of("produtoId", FILTRO_OLEO, "quantidade", quantidade, "valorUnitario", valor)));
    }
}
