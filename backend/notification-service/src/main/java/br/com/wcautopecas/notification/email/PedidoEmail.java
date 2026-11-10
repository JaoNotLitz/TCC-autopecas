package br.com.wcautopecas.notification.email;

import java.util.List;
import java.util.Map;

/** O que chega na email-queue (seção 6.8), já com os destinatários normalizados em lista. */
public record PedidoEmail(List<String> to, List<String> cc, String subject, String template,
        Map<String, Object> data, String priority) {
}
