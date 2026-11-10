package br.com.wcautopecas.commons.web;

import br.com.wcautopecas.commons.seguranca.TokenServico;
import br.com.wcautopecas.commons.seguranca.UsuarioAtual;
import java.time.Duration;
import org.slf4j.MDC;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Chamadas HTTP entre serviços na rede do Compose (decisão A4): repassam o token do usuário e o
 * {@code X-Correlation-Id}, com timeout curto.
 */
public final class ClienteInterno {

    private ClienteInterno() {
    }

    public static RestClient criar(String baseUrl, Duration timeout) {
        return criar(baseUrl, timeout, null);
    }

    /**
     * Fora de uma requisição (listener de fila), usa o token do usuário técnico; dentro dela,
     * repassa o token de quem chamou.
     */
    public static RestClient criar(String baseUrl, Duration timeout, TokenServico sistema) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) timeout.toMillis());
        fabrica.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor((req, corpo, exec) -> {
                    var usuario = UsuarioAtual.opcional();
                    if (usuario.isPresent()) {
                        req.getHeaders().setBearerAuth(usuario.get().token());
                    } else if (sistema != null) {
                        req.getHeaders().setBearerAuth(sistema.token());
                    }
                    String correlation = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (correlation != null) {
                        req.getHeaders().set(CorrelationIdFilter.HEADER, correlation);
                    }
                    return exec.execute(req, corpo);
                })
                .build();
    }
}
