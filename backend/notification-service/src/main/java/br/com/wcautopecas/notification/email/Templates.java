package br.com.wcautopecas.notification.email;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** HU-B15.1 RF02: templates Thymeleaf em {@code resources/email-templates/}, com CSS inline. */
@Component
public class Templates {

    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Templates conhecidos e o assunto usado quando o pedido não traz um. */
    static final Map<String, String> ASSUNTOS = Map.of(
            "welcome", "Bem-vindo(a) à WC Autopeças",
            "sale-confirmation", "Sua compra na WC Autopeças",
            "low-stock-alert", "Estoque crítico",
            "fiscal-error", "Alerta do sistema WC Autopeças",
            "contador-lote", "WC Autopeças - XMLs do mês");

    private final TemplateEngine engine;

    public Templates(TemplateEngine emailTemplateEngine) {
        this.engine = emailTemplateEngine;
    }

    public boolean existe(String template) {
        return template != null && ASSUNTOS.containsKey(template);
    }

    public String renderizar(String template, Map<String, Object> dados) {
        Context ctx = new Context(PT_BR);
        if (dados != null) {
            dados.forEach(ctx::setVariable);
        }
        return engine.process(template, ctx);
    }

    public String assunto(PedidoEmail p) {
        return Optional.ofNullable(p.subject()).filter(s -> !s.isBlank()).orElseGet(() -> ASSUNTOS.get(p.template()));
    }

    @Configuration
    static class Config {

        @Bean
        TemplateEngine emailTemplateEngine() {
            var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("email-templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(true);
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }
    }
}
