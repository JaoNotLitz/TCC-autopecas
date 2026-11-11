package br.com.wcautopecas.entry.integracao;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.web.ClienteInterno;
import br.com.wcautopecas.entry.EntradasProperties;
import br.com.wcautopecas.entry.api.NotaFiscalDto;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import br.com.wcautopecas.commons.seguranca.TokenServico;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Timeout de 2 s e uma retentativa em falha de rede ou 5xx; depois responde 503. */
@Component
public class IntegracoesHttp implements Integracoes {

    private static final Logger log = LoggerFactory.getLogger(IntegracoesHttp.class);
    private static final int TENTATIVAS = 2;

    private final RestClient produtos;
    private final RestClient fiscal;

    /** No XmlProcessorListener não há usuário na requisição: vai o token do usuário técnico (EPB-12). */
    public IntegracoesHttp(EntradasProperties props, TokenServico sistema) {
        this.produtos = ClienteInterno.criar(props.getProductUrl(), props.getTimeoutChamadas(), sistema);
        this.fiscal = ClienteInterno.criar(props.getFiscalUrl(), props.getTimeoutChamadas(), sistema);
    }

    @Override
    public List<CodigoProduto> produtosPorCodigo(List<String> codigos) {
        if (codigos.isEmpty()) {
            return List.of();
        }
        return chamar("Serviço de produtos indisponível", () -> Arrays.asList(produtos.post()
                .uri("/internal/produtos/por-codigo").body(codigos).retrieve().body(CodigoProduto[].class)));
    }

    @Override
    public NotaFiscalDto registrarNota(NotaEntradaFiscal nota) {
        return chamar("Serviço fiscal indisponível", () -> fiscal.post().uri("/internal/notas/entrada")
                .body(nota).retrieve().body(NotaFiscalDto.class));
    }

    private <T> T chamar(String indisponivel, Supplier<T> chamada) {
        RuntimeException ultima = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                return chamada.get();
            } catch (ResourceAccessException e) {
                ultima = e;
            } catch (RestClientResponseException e) {
                if (!e.getStatusCode().is5xxServerError()) {
                    log.error("{}: resposta inesperada {} {}", indisponivel, e.getStatusCode(),
                            e.getResponseBodyAsString());
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, indisponivel);
                }
                ultima = e;
            }
        }
        log.error("{}: {}", indisponivel, ultima.getMessage());
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, indisponivel);
    }
}
