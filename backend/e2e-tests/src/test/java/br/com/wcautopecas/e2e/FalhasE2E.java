package br.com.wcautopecas.e2e;

import static br.com.wcautopecas.e2e.Stack.PRAZO;
import static br.com.wcautopecas.e2e.Stack.aguardarEstoque;
import static br.com.wcautopecas.e2e.Stack.aguardarNota;
import static br.com.wcautopecas.e2e.Stack.aguardarNotaDaVenda;
import static br.com.wcautopecas.e2e.Stack.api;
import static br.com.wcautopecas.e2e.Stack.emailsSes;
import static br.com.wcautopecas.e2e.Stack.estoque;
import static br.com.wcautopecas.e2e.Stack.garantirEstoque;
import static br.com.wcautopecas.e2e.Stack.lambda;
import static br.com.wcautopecas.e2e.Stack.notaDaVenda;
import static br.com.wcautopecas.e2e.Stack.valorSemGatilho;
import static br.com.wcautopecas.e2e.Stack.venda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-B17.4: a SEFAZ cai, a mensagem vai para a DLQ com alerta e volta pelo redrive. Os tempos entre as
 * tentativas vêm de {@code SEFAZ_ESPERAS_RETENTATIVA}, que o {@code make e2e} sobe em segundos (RF04).
 */
@DisplayName("HU-B17.4 Falhas: SEFAZ fora, DLQ e redrive")
class FalhasE2E {

    private static final long FLUIDO = 5;
    /** Três tentativas com as esperas curtas do make e2e, mais a ida para a DLQ. */
    private static final Duration PRAZO_DLQ = Duration.ofSeconds(90);

    @Test
    @DisplayName("TC-B17.4.1 SEFAZ fora do ar: 3 tentativas, DLQ e alerta; o redrive autoriza a nota")
    void quedaERedrive() {
        garantirEstoque(FLUIDO, 5);
        long antes = estoque(FLUIDO);
        long vendaId;
        long notaId;

        try (SefazMock ignorado = SefazMock.foraDoAr()) {
            vendaId = venda("nfce", null, FLUIDO, 1, valorSemGatilho(FLUIDO, 1));
            await("nota da venda " + vendaId).atMost(PRAZO).until(() -> notaDaVenda(vendaId) != null);
            notaId = notaDaVenda(vendaId).path("id").asLong();

            // A mensagem da nota chega à fiscal-dlq (o redrive em dryRun só lista).
            AtomicReference<JsonNode> naDlq = new AtomicReference<>();
            await("nota " + notaId + " na fiscal-dlq").atMost(PRAZO_DLQ).pollInterval(Duration.ofSeconds(3))
                    .until(() -> {
                        naDlq.set(mensagemDaNota(lambda("wc-dlq-redrive",
                                Map.of("origem", "sefaz-queue", "max", 50, "dryRun", true)), notaId));
                        return naDlq.get() != null;
                    });
            assertThat(naDlq.get().path("recebimentos").asInt()).isGreaterThanOrEqualTo(3);
            assertThat(Stack.nota(notaId).path("status").asText()).isEqualTo("processando");

            // Alerta high do fiscal-service, entregue pelo SES ao pessoal de operação.
            String alerta = "(id " + notaId + ") foi para a DLQ";
            await("alerta da nota " + notaId).atMost(PRAZO).until(() -> emailsSes().contains(alerta));
            assertThat(emailsSes()).contains(Stack.OPS);
        }

        // SEFAZ de volta: o redrive devolve a mensagem para a sefaz-queue e a nota sai autorizada.
        JsonNode redrive = lambda("wc-dlq-redrive", Map.of("origem", "sefaz-queue", "max", 50, "dryRun", false));
        assertThat(redrive.path("reenviadas").asInt()).isGreaterThanOrEqualTo(1);
        aguardarNota(notaId, "autorizada");
        aguardarEstoque(FLUIDO, antes - 1);
    }

    @Test
    @DisplayName("TC-B17.4.2 Rejeição da SEFAZ: com o simulador normal, o reenvio autoriza")
    void rejeicaoEReenvio() {
        garantirEstoque(FLUIDO, 5);
        long notaId;

        try (SefazMock ignorado = SefazMock.rejeitando()) {
            long vendaId = venda("nfce", null, FLUIDO, 1, valorSemGatilho(FLUIDO, 1));
            notaId = aguardarNotaDaVenda(vendaId, "rejeitada", PRAZO).path("id").asLong();
        }

        JsonNode reenviada = Stack.json(api().post("/notas/{id}/reenviar", notaId).then().statusCode(200).extract()
                .asString());
        assertThat(reenviada.path("status").asText()).isEqualTo("processando");
        aguardarNota(notaId, "autorizada");
    }

    private static JsonNode mensagemDaNota(JsonNode redrive, long notaId) {
        for (JsonNode m : redrive.path("mensagens")) {
            String corpo = m.path("corpo").asText();
            if (corpo.matches("(?s).*\"notaId\"\\s*:\\s*" + notaId + "\\b.*")) {
                return m;
            }
        }
        return null;
    }
}
