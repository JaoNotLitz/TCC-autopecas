package br.com.wcautopecas.reports.fiscal;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.web.ClienteInterno;
import br.com.wcautopecas.reports.RelatoriosProperties;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Pede ao fiscal-service a mesma listagem que a tela usa ({@code GET /api/notas}, com o token do
 * usuário repassado), para o CSV ter exatamente as notas do Painel Fiscal. Uma retentativa em
 * falha de rede ou 5xx; depois responde 503.
 */
@Component
public class NotasFiscaisHttp implements NotasFiscais {

    private static final Logger log = LoggerFactory.getLogger(NotasFiscaisHttp.class);
    private static final String INDISPONIVEL = "Serviço fiscal indisponível";

    private final RestClient fiscal;

    public NotasFiscaisHttp(RelatoriosProperties props) {
        this.fiscal = ClienteInterno.criar(props.getFiscalUrl(), props.getTimeoutChamadas());
    }

    @Override
    public List<NotaResumo> listar(String status, LocalDate de, LocalDate ate) {
        RuntimeException ultima = null;
        for (int tentativa = 1; tentativa <= 2; tentativa++) {
            try {
                NotaResumo[] notas = fiscal.get().uri(u -> {
                    u.path("/api/notas").queryParam("de", de).queryParam("ate", ate);
                    if (status != null) {
                        u.queryParam("status", status);
                    }
                    return u.build();
                }).retrieve().body(NotaResumo[].class);
                return notas == null ? List.of() : Arrays.asList(notas);
            } catch (ResourceAccessException e) {
                ultima = e;
            } catch (RestClientResponseException e) {
                if (!e.getStatusCode().is5xxServerError()) {
                    log.error("{}: resposta inesperada {} {}", INDISPONIVEL, e.getStatusCode(),
                            e.getResponseBodyAsString());
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, INDISPONIVEL);
                }
                ultima = e;
            }
        }
        log.error("{}: {}", INDISPONIVEL, ultima.getMessage());
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, INDISPONIVEL);
    }
}
