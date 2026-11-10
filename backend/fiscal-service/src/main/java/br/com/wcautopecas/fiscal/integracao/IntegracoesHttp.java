package br.com.wcautopecas.fiscal.integracao;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.seguranca.TokenServico;
import br.com.wcautopecas.commons.web.ClienteInterno;
import br.com.wcautopecas.fiscal.FiscalProperties;
import java.util.Map;
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
 * Implementação HTTP: timeout curto e uma retentativa em falha de rede ou 5xx. No listener não há
 * usuário na requisição, então vai o token do usuário técnico ({@link TokenServico}).
 */
@Component
public class IntegracoesHttp implements Integracoes {

    private static final Logger log = LoggerFactory.getLogger(IntegracoesHttp.class);
    private static final int TENTATIVAS = 2;

    private final RestClient vendas;
    private final RestClient clientes;
    private final RestClient produtos;

    public IntegracoesHttp(FiscalProperties props, TokenServico sistema) {
        this.vendas = ClienteInterno.criar(props.getSalesUrl(), props.getTimeoutChamadas(), sistema);
        this.clientes = ClienteInterno.criar(props.getCustomerUrl(), props.getTimeoutChamadas(), sistema);
        this.produtos = ClienteInterno.criar(props.getProductUrl(), props.getTimeoutChamadas(), sistema);
    }

    @Override
    public Venda venda(long id) {
        return chamar("Serviço de vendas indisponível", "Venda " + id + " não encontrada",
                () -> vendas.get().uri("/api/vendas/{id}", id).retrieve().body(Venda.class));
    }

    @Override
    public Cliente cliente(long id) {
        return chamar("Serviço de clientes indisponível", "Cliente não encontrado",
                () -> clientes.get().uri("/api/clientes/{id}", id).retrieve().body(Cliente.class));
    }

    @Override
    public ProdutoFiscal produto(long id) {
        return chamar("Serviço de produtos indisponível", "Produto " + id + " não encontrado",
                () -> produtos.get().uri("/internal/produtos/{id}", id).retrieve().body(ProdutoFiscal.class));
    }

    @Override
    public void vincularNota(long vendaId, long notaFiscalId) {
        chamar("Serviço de vendas indisponível", "Venda " + vendaId + " não encontrada",
                () -> vendas.put().uri("/internal/vendas/{id}/nota-fiscal", vendaId)
                        .body(Map.of("notaFiscalId", notaFiscalId)).retrieve().toBodilessEntity());
    }

    private <T> T chamar(String indisponivel, String naoEncontrado, Supplier<T> chamada) {
        RuntimeException ultima = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                return chamada.get();
            } catch (HttpClientErrorException.NotFound e) {
                throw new NaoEncontradoException(naoEncontrado);
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
