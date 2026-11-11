package br.com.wcautopecas.dlq;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Lê a DLQ segurando as mensagens (visibility) enquanto a Lambda decide o que fazer com elas. */
final class LeitorDlq {

    private LeitorDlq() {
    }

    static String url(SqsClient sqs, String fila) {
        return sqs.getQueueUrl(r -> r.queueName(fila)).queueUrl();
    }

    /** Lê até {@code max} mensagens (lotes de 10), cada uma invisível por {@code segurar} segundos. */
    static List<MensagemDlq> ler(SqsClient sqs, String url, int max, int segurar) {
        List<MensagemDlq> lidas = new ArrayList<>();
        while (lidas.size() < max) {
            int lote = Math.min(10, max - lidas.size());
            var resp = sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(lote).visibilityTimeout(segurar)
                    .waitTimeSeconds(0).messageAttributeNames("All")
                    .attributeNamesWithStrings("All"));
            if (resp.messages().isEmpty()) {
                break;
            }
            resp.messages().forEach(m -> lidas.add(MensagemDlq.de(m)));
        }
        return lidas;
    }

    static void liberar(SqsClient sqs, String url, List<MensagemDlq> mensagens) {
        mensagens.forEach(m -> sqs.changeMessageVisibility(r -> r.queueUrl(url).receiptHandle(m.receiptHandle())
                .visibilityTimeout(0)));
    }
}
