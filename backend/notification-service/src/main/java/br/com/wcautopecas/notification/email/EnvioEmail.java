package br.com.wcautopecas.notification.email;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HU-B15.1: valida, renderiza e envia. Destino inválido e template desconhecido são descartados
 * com log (sem retentativa); erro do SES sobe para o listener decidir.
 */
@Service
public class EnvioEmail {

    private static final Logger log = LoggerFactory.getLogger(EnvioEmail.class);
    static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;.]{2,}$");

    private final Templates templates;
    private final EmailSender sender;

    public EnvioEmail(Templates templates, EmailSender sender) {
        this.templates = templates;
        this.sender = sender;
    }

    /** Devolve o messageId do SES, ou vazio quando o pedido foi descartado. */
    public Optional<String> enviar(PedidoEmail p) {
        if (!templates.existe(p.template())) {
            log.error("template de e-mail '{}' não existe; pedido descartado", p.template());
            return Optional.empty();
        }
        List<String> to = validos(p.to(), "to");
        List<String> cc = validos(p.cc(), "cc");
        if (to.isEmpty()) {
            log.warn("e-mail {} sem destinatário válido (to={}); nada enviado", p.template(), p.to());
            return Optional.empty();
        }
        PedidoEmail limpo = new PedidoEmail(to, cc, p.subject(), p.template(), p.data(), p.priority());
        String assunto = templates.assunto(limpo);
        String html = templates.renderizar(p.template(), p.data());
        String messageId = sender.enviar(limpo, assunto, html);
        log.info("e-mail enviado: to={} template={} messageId={}", to, p.template(), messageId);
        return Optional.of(messageId);
    }

    static boolean valido(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    private static List<String> validos(List<String> emails, String campo) {
        if (emails == null) {
            return List.of();
        }
        emails.stream().filter(e -> !valido(e)).forEach(e -> log.warn("{} inválido ignorado: '{}'", campo, e));
        return emails.stream().filter(EnvioEmail::valido).distinct().toList();
    }
}
