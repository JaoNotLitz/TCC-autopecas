package br.com.wcautopecas.notification;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.notificacoes")
public class NotificacoesProperties {

    private String filaEmail = "email-queue";
    private String filaClientes = "customer-events-queue";
    private String remetente = "noreply@wcautopecas.com.br";
    /** HU-B15.3: quem recebe o alerta de estoque crítico. */
    private List<String> alertaEstoqueDestinatarios = new ArrayList<>();
    /** HU-B15.3: quem recebe os alertas de sistema (high e critical). */
    private List<String> opsDestinatarios = new ArrayList<>();
    /** Erro do SES: tentativas antes de descartar (a email-queue não tem DLQ, seção 6.8). */
    private int maximoTentativas = 3;
}
