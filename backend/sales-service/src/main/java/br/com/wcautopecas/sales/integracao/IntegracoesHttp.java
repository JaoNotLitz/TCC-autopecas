package br.com.wcautopecas.sales.integracao;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.web.ClienteInterno;
import br.com.wcautopecas.sales.VendasProperties;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementação HTTP (decisão A4): timeout de 2 s e uma retentativa em falha de rede ou 5xx
 * (HU-B06.2). Depois disso responde 503 com a mensagem do serviço que caiu.
 */
@Component
public class IntegracoesHttp implements Integracoes {

    private static final Logger log = LoggerFactory.getLogger(IntegracoesHttp.class);
    private static final int TENTATIVAS = 2;

    private final RestClient clientes;
    private final RestClient produtos;

    public IntegracoesHttp(VendasProperties props) {
        this.clientes = ClienteInterno.criar(props.getCustomerUrl(), props.getTimeoutChamadas());
        this.produtos = ClienteInterno.criar(props.getProductUrl(), props.getTimeoutChamadas());
    }

    @Override
    public Cliente cliente(long id) {
        return chamar("Serviço de clientes indisponível", () -> {
            try {
                return clientes.get().uri("/api/clientes/{id}", id).retrieve().body(Cliente.class);
            } catch (HttpClientErrorException.NotFound e) {
                throw new NaoEncontradoException("Cliente não encontrado");
            }
        });
    }

    @Override
    public Produto produto(long id) {
        return chamar("Serviço de produtos indisponível", () -> {
            try {
                return produtos.get().uri("/internal/produtos/{id}", id).retrieve().body(Produto.class);
            } catch (HttpClientErrorException.NotFound e) {
                throw new NaoEncontradoException("Produto " + id + " não encontrado");
            }
        });
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
