package br.com.wcautopecas.commons.web;

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
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout((int) timeout.toMillis());
        fabrica.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .requestInterceptor((req, corpo, exec) -> {
                    UsuarioAtual.opcional().ifPresent(u -> req.getHeaders().setBearerAuth(u.token()));
                    String correlation = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (correlation != null) {
                        req.getHeaders().set(CorrelationIdFilter.HEADER, correlation);
                    }
                    return exec.execute(req, corpo);
                })
                .build();
    }
}
