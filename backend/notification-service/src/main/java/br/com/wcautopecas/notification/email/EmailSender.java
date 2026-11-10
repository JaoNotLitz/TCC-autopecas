package br.com.wcautopecas.notification.email;

import br.com.wcautopecas.notification.NotificacoesProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;

/** HU-B15.1 RF03: {@code SendEmail} do SES com o remetente configurado. Devolve o messageId. */
@Component
public class EmailSender {

    private final SesClient ses;
    private final NotificacoesProperties props;

    public EmailSender(SesClient ses, NotificacoesProperties props) {
        this.ses = ses;
        this.props = props;
    }

    public String enviar(PedidoEmail p, String assunto, String html) {
        return ses.sendEmail(r -> r.source(props.getRemetente())
                .destination(d -> d.toAddresses(p.to()).ccAddresses(p.cc()))
                .message(m -> m.subject(c -> c.data(assunto).charset("UTF-8"))
                        .body(b -> b.html(c -> c.data(html).charset("UTF-8")))))
                .messageId();
    }
}
