package br.com.wcautopecas.reports.fontes;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.web.ClienteInterno;
import br.com.wcautopecas.reports.RelatoriosProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Chamadas aos serviços de vendas, produtos e clientes com o token de quem pediu o relatório. Uma
 * retentativa em falha de rede ou 5xx; depois responde 503 com o nome do serviço.
 */
@Component
public class FontesHttp implements Fontes {

    private static final Logger log = LoggerFactory.getLogger(FontesHttp.class);

    private final RestClient vendas;
    private final RestClient produtos;
    private final RestClient clientes;

    public FontesHttp(RelatoriosProperties props) {
        this.vendas = ClienteInterno.criar(props.getSalesUrl(), props.getTimeoutChamadas());
        this.produtos = ClienteInterno.criar(props.getProductUrl(), props.getTimeoutChamadas());
        this.clientes = ClienteInterno.criar(props.getCustomerUrl(), props.getTimeoutChamadas());
    }

    @Override
    public List<Venda> vendas(LocalDate de, LocalDate ate) {
        return lista("Serviço de vendas indisponível", () -> vendas.get()
                .uri(u -> u.path("/internal/vendas").queryParam("de", de).queryParam("ate", ate).build())
                .retrieve().body(Venda[].class));
    }

    @Override
    public List<Posicao> estoque() {
        return lista("Serviço de produtos indisponível", () -> produtos.get().uri("/internal/produtos/estoque")
                .retrieve().body(Posicao[].class));
    }

    @Override
    public ResumoEstoque resumoEstoque() {
        return chamar("Serviço de produtos indisponível", () -> produtos.get().uri("/api/produtos/resumo")
                .retrieve().body(ResumoEstoque.class));
    }

    @Override
    public ResumoClientes resumoClientes(Instant desde) {
        return chamar("Serviço de clientes indisponível", () -> clientes.get()
                .uri(u -> u.path("/internal/clientes/resumo").queryParam("desde", desde.toString()).build())
                .retrieve().body(ResumoClientes.class));
    }

    @Override
    public Map<Long, String> nomesClientes() {
        List<ClienteNome> lista = lista("Serviço de clientes indisponível", () -> clientes.get().uri("/api/clientes")
                .retrieve().body(ClienteNome[].class));
        Map<Long, String> nomes = new LinkedHashMap<>();
        lista.forEach(c -> nomes.put(c.id(), c.nome()));
        return nomes;
    }

    record ClienteNome(long id, String nome) {
    }

    private <T> List<T> lista(String indisponivel, Supplier<T[]> chamada) {
        T[] r = chamar(indisponivel, chamada);
        return r == null ? List.of() : Arrays.asList(r);
    }

    private <T> T chamar(String indisponivel, Supplier<T> chamada) {
        RuntimeException ultima = null;
        for (int tentativa = 1; tentativa <= 2; tentativa++) {
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
