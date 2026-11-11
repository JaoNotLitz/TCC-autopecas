package br.com.wcautopecas.dlq;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * HU-B16.2: {@code make redrive ARGS='{"origem":"sefaz-queue","max":10,"dryRun":true}'}.
 * Com dryRun (o padrão) só lista; sem, devolve cada mensagem à fila de origem com o atributo
 * {@code redrive=true} e apaga da DLQ. Mensagem de origem desconhecida nunca é reenviada.
 */
public class DlqRedriveHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    static final int MAX_PADRAO = 10;
    static final int SEGURAR_SEGUNDOS = 60;

    private final Ambiente amb;
    private final SqsClient sqs;

    public DlqRedriveHandler() {
        this(Ambiente.doSistema());
    }

    DlqRedriveHandler(Ambiente amb) {
        this.amb = amb;
        this.sqs = amb.sqs();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> entrada, Context context) {
        Map<String, Object> in = entrada == null ? Map.of() : entrada;
        String filtro = in.get("origem") == null ? null : in.get("origem").toString();
        int max = in.get("max") == null ? MAX_PADRAO : Integer.parseInt(in.get("max").toString());
        boolean dryRun = in.get("dryRun") == null || Boolean.parseBoolean(in.get("dryRun").toString());

        String dlq = LeitorDlq.url(sqs, amb.dlq());
        // Lê a DLQ inteira (até um teto) para o filtro por origem não parar nas primeiras mensagens.
        List<MensagemDlq> lidas = LeitorDlq.ler(sqs, dlq, Math.max(max * 10, 100), SEGURAR_SEGUNDOS);
        List<MensagemDlq> escolhidas = new ArrayList<>();
        List<MensagemDlq> devolver = new ArrayList<>();
        for (MensagemDlq m : lidas) {
            boolean serve = (filtro == null || filtro.equals(m.origem())) && escolhidas.size() < max;
            (serve ? escolhidas : devolver).add(m);
        }

        List<Map<String, Object>> lista = new ArrayList<>();
        int reenviadas = 0;
        for (MensagemDlq m : escolhidas) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("id", m.id());
            linha.put("origem", m.origem());
            linha.put("eventType", m.eventType());
            linha.put("recebimentos", m.recebimentos());
            linha.put("corpo", m.resumo());
            if (!dryRun && !"desconhecida".equals(m.origem())) {
                reenviar(m);
                sqs.deleteMessage(r -> r.queueUrl(dlq).receiptHandle(m.receiptHandle()));
                linha.put("reenviada", true);
                reenviadas++;
            } else {
                devolver.add(m);
                linha.put("reenviada", false);
            }
            lista.add(linha);
        }
        LeitorDlq.liberar(sqs, dlq, devolver);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dryRun", dryRun);
        r.put("origem", filtro);
        r.put("naDlq", lidas.size());
        r.put("encontradas", escolhidas.size());
        r.put("reenviadas", reenviadas);
        r.put("mensagens", lista);
        System.out.println("wc-dlq-redrive dryRun=" + dryRun + " encontradas=" + escolhidas.size()
                + " reenviadas=" + reenviadas);
        return r;
    }

    private void reenviar(MensagemDlq m) {
        Map<String, MessageAttributeValue> attrs = new HashMap<>(m.atributos());
        attrs.put("redrive", MessageAttributeValue.builder().dataType("String").stringValue("true").build());
        attrs.putIfAbsent("origemFila", MessageAttributeValue.builder().dataType("String").stringValue(m.origem())
                .build());
        String url = LeitorDlq.url(sqs, m.origem());
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(m.corpo()).messageAttributes(attrs));
    }
}
