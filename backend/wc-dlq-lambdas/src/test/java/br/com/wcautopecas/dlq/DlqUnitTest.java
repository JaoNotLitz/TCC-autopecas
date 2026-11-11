package br.com.wcautopecas.dlq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DlqUnitTest {

    @Test
    void faixasEDeduplicacao() {
        assertThat(DlqMonitorHandler.faixa(0, 5)).isZero();
        assertThat(DlqMonitorHandler.faixa(5, 5)).isZero();
        assertThat(DlqMonitorHandler.faixa(6, 5)).isEqualTo(1);
        assertThat(DlqMonitorHandler.faixa(10, 5)).isEqualTo(1);
        assertThat(DlqMonitorHandler.faixa(11, 5)).isEqualTo(2);

        assertThat(DlqMonitorHandler.alertar(0, 0)).isFalse();   // abaixo do limite
        assertThat(DlqMonitorHandler.alertar(1, 0)).isTrue();    // passou do limite
        assertThat(DlqMonitorHandler.alertar(1, 1)).isFalse();   // mesma faixa: não repete
        assertThat(DlqMonitorHandler.alertar(2, 1)).isTrue();    // subiu de faixa
        assertThat(DlqMonitorHandler.alertar(1, 2)).isFalse();   // desceu: só guarda
    }

    @Test
    void origemPeloAtributoPelaDlqOuPeloCorpo() {
        assertThat(MensagemDlq.origem(Map.of("origemFila", "sefaz-queue"), Map.of(), "{}")).isEqualTo("sefaz-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of("DeadLetterQueueSourceArn",
                "arn:aws:sqs:sa-east-1:000000000000:inventory-updates-queue"), "{}"))
                .isEqualTo("inventory-updates-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of(), "{\"jobType\":\"generate_xml\",\"notaId\":1}"))
                .isEqualTo("fiscal-jobs-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of(), "{\"notaId\":1,\"xmlPath\":\"s3://b/k.xml\",\"tentativa\":1}"))
                .isEqualTo("sefaz-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of(), "{\"eventType\":\"cancelamento\",\"eventoId\":3}"))
                .isEqualTo("fiscal-events-queue");
        assertThat(MensagemDlq.origem(Map.of("eventType", "nota.autorizada"), Map.of(), "{\"notaId\":1}"))
                .isEqualTo("document-events-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of(), "{\"eventType\":\"sale.created\",\"saleId\":9}"))
                .isEqualTo("sales-events-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of(), "{\"type\":\"decrease\",\"produtoId\":1,\"quantidade\":2}"))
                .isEqualTo("inventory-updates-queue");
        assertThat(MensagemDlq.origem(Map.of(), Map.of(), "não é json")).isEqualTo("desconhecida");
    }
}
